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

import java.nio.file.Paths
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.stream.scaladsl.{FileIO, Source}
import org.apache.pekko.util.ByteString

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.connector.DeskproConnector
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{CreateDeskproTicket, DeskproTicketCreated, DeskproTicketMessage, TicketStatus}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.CreateTicketResponseRequest
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.DeskproResponse
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{CreateTicketRequest, DeskproTicketCreationFailed, _}
import uk.gov.hmrc.apiplatformdeskpro.repository.DeskproResponseRepository
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

@Singleton
class TicketService @Inject() (
    deskproConnector: DeskproConnector,
    personService: PersonService,
    deskproResponseRepository: DeskproResponseRepository,
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
    val maybeReasonKey       = request.reasonKey.fold(Map.empty[String, String])(v => Map(config.deskproReasonKey -> v))

    val fields = maybeOrganisation ++ maybeTeamMemberEmail ++ maybeApiName ++ maybeApplicationId ++ maybeSupportReason ++ maybeReasonKey
    val person = DeskproPerson(request.fullName, request.email)

    CreateDeskproTicket(
      person,
      request.subject,
      DeskproTicketMessage.fromRaw(request.message, person),
      config.deskproBrand,
      fields
    )
  }

  def getTicketsForPerson(personEmail: LaxEmailAddress, status: Option[String])(implicit hc: HeaderCarrier): Future[List[DeskproTicket]] = {
    for {
      personId       <- personService.getPersonIdForEmail(personEmail)
      ticketResponse <- deskproConnector.getTicketsForPersonId(personId, status)
    } yield ticketResponse.data.map(response => DeskproTicket.build(response, List.empty, List.empty))
  }

  def batchFetchTicket(ticketId: Int)(implicit hc: HeaderCarrier): Future[Option[DeskproTicket]] = {
    deskproConnector.batchFetchTicket(ticketId) map { response => DeskproTicket.build(response.responses.ticket, response.responses.messages, response.responses.attachments) }
  }

  def createResponse(ticketId: Int, request: CreateTicketResponseRequest)(implicit hc: HeaderCarrier): Future[DeskproTicketResponseResult] = {
    request.fileReference.fold(
      createMessage(ticketId, request.userEmail, request.message, request.status)
    )(fileRef => saveMessage(ticketId, request.userEmail, request.message, request.status, fileRef))
  }

  private def createMessage(ticketId: Int, userEmail: LaxEmailAddress, message: String, status: TicketStatus)(implicit hc: HeaderCarrier): Future[DeskproTicketResponseResult] = {
    for {
      createResponseResult <- deskproConnector.createResponse(ticketId, userEmail.text, message)
      _                    <- deskproConnector.updateTicketStatus(ticketId, status)
    } yield createResponseResult
  }

  private def saveMessage(ticketId: Int, userEmail: LaxEmailAddress, message: String, ticketStatus: TicketStatus, fileReference: String): Future[DeskproTicketResponseResult] = {
    deskproResponseRepository.create(DeskproResponse(fileReference, ticketId, userEmail, message, ticketStatus)) map { result => DeskproTicketResponseSuccess }
  }

  def deleteTicket(ticketId: Int)(implicit hc: HeaderCarrier): Future[DeskproTicketUpdateResult] = {
    deskproConnector.deleteTicket(ticketId)
  }

  def addAttachment(fileName: String, fileType: String, ticketId: Int, message: String, userEmail: String)(implicit hc: HeaderCarrier) = {
    val file: java.nio.file.Path   = Paths.get(fileName)
    val src: Source[ByteString, _] = FileIO.fromPath(file)

    for {
      blobResponse <- deskproConnector.createBlob(
                        fileName,
                        fileType,
                        src
                      )
      msgResponse  <- deskproConnector.createMessageWithAttachment(
                        ticketId,
                        userEmail,
                        message,
                        blobResponse.data.blob_id,
                        blobResponse.data.blob_auth
                      )
    } yield (blobResponse, msgResponse)
  }
}
