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

package uk.gov.hmrc.apiplatformdeskpro.service

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.connector.DeskproConnector
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{CreateDeskproTicket, DeskproTicketCreated, DeskproTicketMessage}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{CreateTicketRequest, DeskproTicketCreationFailed, _}
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

@Singleton
class TicketService @Inject() (
    deskproConnector: DeskproConnector,
    config: AppConfig
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger {

  def submitTicket(createTicketRequest: CreateTicketRequest)(implicit hc: HeaderCarrier): Future[Either[DeskproTicketCreationFailed, DeskproTicketCreated]] = {

    val deskproTicket: CreateDeskproTicket = createDeskproTicket(createTicketRequest)
    deskproConnector.createTicket(deskproTicket)
  }

  private def createDeskproTicket(request: CreateTicketRequest): CreateDeskproTicket = {

    val maybeOrganisation    = request.organisation.fold(Map.empty[String, String])(v => Map(config.deskproOrganisation -> v))
    val maybeTeamMemberEmail = request.teamMemberEmail.fold(Map.empty[String, String])(v => Map(config.deskproTeamMemberEmail -> v))
    val maybeApiName         = request.apiName.fold(Map.empty[String, String])(v => Map(config.deskproApiName -> v))
    val maybeApplicationId   = request.applicationId.fold(Map.empty[String, String])(v => Map(config.deskproApplicationId -> v))
    val maybeSupportReason   = request.supportReason.fold(Map.empty[String, String])(v => Map(config.deskproSupportReason -> v))

    val fields = maybeOrganisation ++ maybeTeamMemberEmail ++ maybeApiName ++ maybeApplicationId ++ maybeSupportReason

    CreateDeskproTicket(
      DeskproPerson(request.fullName, request.email),
      request.subject,
      DeskproTicketMessage.fromRaw(request.message),
      config.deskproBrand,
      fields
    )
  }

  def getTicketsForPerson(personEmail: LaxEmailAddress, status: Option[String])(implicit hc: HeaderCarrier): Future[List[DeskproTicket]] = {
    for {
      personResponse <- deskproConnector.getPersonForEmail(personEmail)
      personId        = personResponse.data.headOption.getOrElse(throw new DeskproPersonNotFound("Person not found")).id
      ticketResponse <- deskproConnector.getTicketsForPersonId(personId, status)
    } yield ticketResponse.data.map(response => DeskproTicket.build(response, List.empty))
  }

  def fetchTicket(ticketId: Int)(implicit hc: HeaderCarrier): Future[Option[DeskproTicket]] = {
    for {
      ticketResponse   <- deskproConnector.fetchTicket(ticketId)
      messagesResponse <- deskproConnector.getTicketMessages(ticketId)
    } yield ticketResponse map { response => DeskproTicket.build(response.data, messagesResponse.data) }
  }

  // def fetchTicket2(ticketId: Int)(implicit hc: HeaderCarrier): Future[Option[DeskproTicket]] = {
  //   deskproConnector.fetchTicket(ticketId) map {
  //      _.map(response => DeskproTicket.build(response.data, List.empty))
  //    }
  // }
}
