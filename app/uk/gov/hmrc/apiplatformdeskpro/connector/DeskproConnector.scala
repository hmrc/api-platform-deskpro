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
import uk.gov.hmrc.apiplatformdeskpro.domain.models._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{
  DeskproLinkedOrganisationWrapper,
  DeskproLinkedPersonWrapper,
  DeskproOrganisationWrapperResponse,
  DeskproTicket,
  DeskproTicketCreated
}
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}

class DeskproConnector @Inject() (http: HttpClientV2, config: AppConfig, metrics: ConnectorMetrics)(implicit val ec: ExecutionContext)
    extends ApplicationLogger {

  val DESKPRO_QUERY_HEADER = "X-Deskpro-Api-Get-Query-Body"

  lazy val serviceBaseUrl: String = config.deskproUrl
  val api: API                    = API("deskpro")

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
            Left(DeskproTicketCreationFailed("Missing authorization"))
          case _            =>
            logger.error(s"Deskpro ticket creation failed for: ${deskproTicket.subject}")
            Left(DeskproTicketCreationFailed("Unknown reason"))
        }
      )
  }

  def createPerson(userId: UserId, name: String, email: String)(implicit hc: HeaderCarrier): Future[DeskproPersonCreationResult] = metrics.record(api) {
    http
      .post(url"${requestUrl("/api/v2/people")}")
      .withProxy
      .withBody(Json.toJson(DeskproPerson(name, email)))
      .setHeader(AUTHORIZATION -> config.deskproApiKey)
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case CREATED                                             =>
            logger.info(s"Deskpro person creation '$userId' success")
            DeskproPersonCreationSuccess
          case BAD_REQUEST if response.body.contains("dupe_email") =>
            logger.info(s"Deskpro person creation '$userId' duplicate email warning")
            DeskproPersonExistsInDeskpro
          case BAD_REQUEST                                         =>
            logger.error(s"Deskpro person creation '$userId' failed Bad request other errorCode")
            DeskproPersonCreationFailure
          case _                                                   =>
            logger.error(s"Deskpro person creation '$userId' failed status: ${response.status}")
            DeskproPersonCreationFailure
        }
      )
  }

  def getOrganisationWithPeopleById(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[DeskproLinkedPersonWrapper] = metrics.record(api) {
    http
      .get(url"${requestUrl(s"/api/v2/organizations/$organisationId/members")}?include=person")
      .withProxy
      .setHeader(AUTHORIZATION -> config.deskproApiKey)
      .execute[DeskproLinkedPersonWrapper]
  }

  def getOrganisationById(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[DeskproOrganisationWrapperResponse] = metrics.record(api) {
    http
      .get(url"${requestUrl(s"/api/v2/organizations/$organisationId")}")
      .withProxy
      .setHeader(AUTHORIZATION -> config.deskproApiKey)
      .execute[DeskproOrganisationWrapperResponse]
  }

  def getOrganisationsForPersonEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[DeskproLinkedOrganisationWrapper] = {
    metrics.record(api) {
      http
        .post(url"${requestUrl(s"/api/v2/people")}")
        .withProxy
        .setHeader(AUTHORIZATION -> config.deskproApiKey)
        .setHeader(DESKPRO_QUERY_HEADER -> "1") // todo explain
        .withBody(Json.parse(
          s"""
             |{
             |  "primary_email":"${email.text}",
             |  "include": "organization_member,organization"
             |}
             |""".stripMargin
        ))
        .execute[DeskproLinkedOrganisationWrapper]
    }
  }

  private def requestUrl[B, A](uri: String): String = s"$serviceBaseUrl$uri"
}
