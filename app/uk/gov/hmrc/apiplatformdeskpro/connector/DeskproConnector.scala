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

import java.time.Clock
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import play.api.http.HeaderNames.AUTHORIZATION
import play.api.http.Status.{BAD_REQUEST, CREATED, NO_CONTENT, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.domain.models._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector._
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

class DeskproConnector @Inject() (http: HttpClientV2, config: AppConfig, metrics: ConnectorMetrics, val clock: Clock)(implicit val ec: ExecutionContext)
    extends ApplicationLogger with ClockNow {

  val DESKPRO_QUERY_HEADER = "X-Deskpro-Api-Get-Query-Body"

  // query header value required for the body of the POST request to be treated as a query
  val DESKPRO_QUERY_MODE_ON = "1"

  lazy val serviceBaseUrl: String = config.deskproUrl
  val api: API                    = API("deskpro")

  def createTicket(deskproTicket: CreateDeskproTicket)(implicit hc: HeaderCarrier): Future[Either[DeskproTicketCreationFailed, DeskproTicketCreated]] = metrics.record(api) {
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
            logger.error(s"Deskpro ticket creation failed as unauthorized for: ${deskproTicket.subject}")
            Left(DeskproTicketCreationFailed("Missing authorization"))
          case _            =>
            val errorMessage = (Json.parse(response.body) \\ "message").mkString(",")
            logger.error(s"Deskpro ticket creation failed with message $errorMessage for: ${deskproTicket.subject}")
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

  def updatePerson(personId: Int, name: String)(implicit hc: HeaderCarrier): Future[DeskproPersonUpdateResult] = metrics.record(api) {
    http
      .put(url"${requestUrl(s"/api/v2/people/${personId}")}")
      .withProxy
      .withBody(Json.toJson(DeskproPersonUpdate(name)))
      .setHeader(AUTHORIZATION -> config.deskproApiKey)
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case NO_CONTENT  =>
            logger.info(s"Deskpro person update '$personId' success")
            DeskproPersonUpdateSuccess
          case BAD_REQUEST =>
            logger.error(s"Deskpro person update '$personId' failed Bad request")
            DeskproPersonUpdateFailure
          case _           =>
            logger.error(s"Deskpro person update '$personId' failed status: ${response.status}")
            DeskproPersonUpdateFailure
        }
      )
  }

  def getPeopleByOrganisationId(organisationId: OrganisationId, pageWanted: Int = 1)(implicit hc: HeaderCarrier): Future[DeskproPeopleResponse] = metrics.record(api) {
    val queryParams = Seq("organization" -> organisationId, "count" -> 200, "page" -> pageWanted)
    http
      .get(url"${requestUrl(s"/api/v2/people")}?$queryParams")
      .withProxy
      .setHeader(AUTHORIZATION -> config.deskproApiKey)
      .execute[DeskproPeopleResponse]
  }

  def markPersonInactive(personId: Int)(implicit hc: HeaderCarrier): Future[DeskproPersonUpdateResult] = metrics.record(api) {
    http
      .put(url"${requestUrl(s"/api/v2/people/$personId")}")
      .withProxy
      .withBody(Json.toJson(createDeskproInactivePerson))
      .setHeader(AUTHORIZATION -> config.deskproApiKey)
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case NO_CONTENT  =>
            logger.info(s"Deskpro mark person inactive '$personId' success")
            DeskproPersonUpdateSuccess
          case BAD_REQUEST =>
            logger.error(s"Deskpro mark person inactive '$personId' failed Bad request")
            DeskproPersonUpdateFailure
          case _           =>
            logger.error(s"Deskpro mark person inactive '$personId' failed status: ${response.status}")
            DeskproPersonUpdateFailure
        }
      )
  }

  private def createDeskproInactivePerson: DeskproInactivePerson =
    DeskproInactivePerson(
      Map(
        config.deskproInactive        -> "1",
        config.deskproInactivatedDate -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(now())
      )
    )

  def getOrganisationById(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[DeskproOrganisationWrapperResponse] = metrics.record(api) {
    http
      .get(url"${requestUrl(s"/api/v2/organizations/$organisationId")}")
      .withProxy
      .setHeader(AUTHORIZATION -> config.deskproApiKey)
      .execute[DeskproOrganisationWrapperResponse]
  }

  def getOrganisationsForPersonEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[DeskproLinkedOrganisationWrapper] = {
    queryPersonForEmail(Json.toJson(GetOrganisationByPersonEmailRequest(email.text)))
  }

  def getPersonForEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[DeskproLinkedOrganisationWrapper] = {
    queryPersonForEmail(Json.toJson(GetPersonByEmailRequest(email.text)))
  }

  private def queryPersonForEmail(requestBody: JsValue)(implicit hc: HeaderCarrier): Future[DeskproLinkedOrganisationWrapper] = {
    metrics.record(api) {
      http
        .post(url"${requestUrl(s"/api/v2/people")}")
        .withProxy
        .setHeader(AUTHORIZATION -> config.deskproApiKey)
        .setHeader(DESKPRO_QUERY_HEADER -> DESKPRO_QUERY_MODE_ON)
        .withBody(requestBody)
        .execute[DeskproLinkedOrganisationWrapper]
    }
  }

  def getTicketsForPersonId(personId: Int)(implicit hc: HeaderCarrier): Future[DeskproTicketsWrapperResponse] = metrics.record(api) {
    val queryParams = Seq("person" -> personId)
    http
      .get(url"${requestUrl("/api/v2/tickets")}?$queryParams")
      .withProxy
      .setHeader(AUTHORIZATION -> config.deskproApiKey)
      .execute[DeskproTicketsWrapperResponse]
  }

  def fetchTicket(ticketId: Int)(implicit hc: HeaderCarrier): Future[Option[DeskproTicketWrapperResponse]] = metrics.record(api) {
    http
      .get(url"${requestUrl(s"/api/v2/tickets/$ticketId")}")
      .withProxy
      .setHeader(AUTHORIZATION -> config.deskproApiKey)
      .execute[Option[DeskproTicketWrapperResponse]]
  }

  def getTicketMessages(ticketId: Int)(implicit hc: HeaderCarrier): Future[DeskproMessagesWrapperResponse] = metrics.record(api) {
    http
      .get(url"${requestUrl(s"/api/v2/tickets/$ticketId/messages")}")
      .withProxy
      .setHeader(AUTHORIZATION -> config.deskproApiKey)
      .execute[DeskproMessagesWrapperResponse]
  }

  private def requestUrl[B, A](uri: String): String = s"$serviceBaseUrl$uri"
}
