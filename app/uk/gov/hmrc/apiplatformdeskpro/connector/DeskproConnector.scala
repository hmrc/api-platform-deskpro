/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.apiplatformdeskpro.connector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status.{BAD_REQUEST, CREATED, UNAUTHORIZED}
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.domain.models.DeskproTicketCreationFailed
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{DeskproTicket, DeskproTicketCreated}
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.http.metrics.common.API
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.DeskproPerson
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import play.api.libs.json.JsValue
import play.api.libs.json.JsString

class DeskproConnector @Inject() (http: HttpClientV2, config: AppConfig, metrics: ConnectorMetrics)(implicit val ec: ExecutionContext)
    extends ApplicationLogger {

  lazy val serviceBaseUrl: String = config.deskproUrl
  val api                         = API("deskpro")

  def createTicket(deskproTicket: DeskproTicket)(implicit hc: HeaderCarrier): Future[Either[DeskproTicketCreationFailed, DeskproTicketCreated]] = metrics.record(api) {
    http.post(url"${requestUrl("/api/v2/tickets")}")
      .withProxy
      .withBody(Json.toJson(deskproTicket))
      .setHeader(AUTHORIZATION -> config.deskproApiKey)
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case CREATED      =>
            logger.info(s"Deskpro ticket '${deskproTicket.subject}' created successfully")
            Right(response.json.as[DeskproTicketCreated])
          case UNAUTHORIZED =>
            logger.error(s"Deskpro ticket creation failed for: ${deskproTicket.subject}")
            logger.error(response.body)
            Left(new DeskproTicketCreationFailed("Missing authorization"))
          case _            =>
            logger.error(s"Deskpro ticket creation failed for: ${deskproTicket.subject}")
            logger.error(response.body)
            Left(new DeskproTicketCreationFailed("Unknown reason"))
        }
      )
  }

    def createPerson(userId: UserId, name: String, email: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
        http
          .post(url"${requestUrl("/api/v2/people")}")
          .withProxy
          .withBody(Json.toJson(DeskproPerson(name, email)))
          .setHeader(AUTHORIZATION -> config.deskproApiKey)
          .execute[HttpResponse]
           .map(response =>
            response.status match {
              case CREATED      =>
                logger.info(s"Deskpro person creation '$userId' success")
                response
              case BAD_REQUEST           =>
                val errorJson: JsValue = (Json.parse(response.body) \ "errors" \ "fields" \ "emails" \ "fields" \ "emails_0" \ "errors" )(0)
                (errorJson \ "code").get match {
                  case JsString("dupe_email")      =>     logger.info(s"Deskpro person creation '$userId' duplicate email warning")
                  case JsString(errorCode)  =>     logger.error(s"Deskpro person creation '$userId' failed Bad request errorCode: $errorCode")
                }
                response
              case _           =>
                logger.error(s"Deskpro person creation '$userId' failed status: ${response.status}")
                response
            }
          )
      }

  private def requestUrl[B, A](uri: String): String = s"$serviceBaseUrl$uri"
}
/*201
{
  "data": {
    "id": 82,
    "primary_email": "dave@test.com",
    "first_name": "dave",
    "last_name": "testerson",
    "title_prefix": "",
    "name": "dave testerson",
    "display_name": "dave testerson",
    "is_agent": false,
    "avatar": {
      "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
      "url_pattern": null,
      "base_gravatar_url": null
    },
    "online": false,
    "online_for_chat": false,
    "last_seen": null,
    "agent_data": null,
    "is_user": false,
    "was_agent": false,
    "can_agent": false,
    "can_admin": false,
    "can_billing": false,
    "can_reports": false,
    "picture_blob": null,
    "disable_picture": false,
    "gravatar_url": "https:\/\/secure.gravatar.com\/avatar\/80d0801f72b3f4d774f15f7115807878?&d=mm",
    "is_contact": true,
    "disable_autoresponses": false,
    "disable_autoresponses_log": "",
    "is_confirmed": false,
    "is_deleted": false,
    "is_disabled": false,
    "creation_system": "web.person",
    "override_display_name": "",
    "display_contact": "dave testerson <dave@test.com>",
    "summary": "",
    "language": 1,
    "organization": null,
    "organization_members": [],
    "organization_position": "",
    "organization_manager": false,
    "timezone": "UTC",
    "date_created": "2024-08-16T14:04:18+0000",
    "date_last_login": null,
    "browser": null,
    "all_user_groups": [
      1
    ],
    "user_groups": [],
    "agent_groups": [],
    "labels": [],
    "emails": [
      "dave@test.com"
    ],
    "phone_numbers": [],
    "tickets_count": 0,
    "chats_count": 0,
    "fields": {},
    "contextual_options": [],
    "contact_data": [],
    "teams": [],
    "primary_team": null,
    "brands": [
      3
    ],
    "preferences": []
  },
  "meta": {},
  "linked": {}
}

{
  "status": 400,
  "code": "invalid_input",
  "message": "Request input is invalid.",
  "errors": {
    "fields": {
      "emails": {
        "fields": {
          "emails_0": {
            "errors": [
              {
                "code": "dupe_email",
                "message": "Email \"dave@test.com\" is already in use by other user."
              }
            ]
          }
        }
      }
    }
  }
}
*/
