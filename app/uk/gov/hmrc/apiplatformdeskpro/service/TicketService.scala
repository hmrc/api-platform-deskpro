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
import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.stream.scaladsl.{FileIO, Source}
import org.apache.pekko.util.ByteString

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.connector.DeskproConnector
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{CreateDeskproTicket, DeskproMessageResponse, DeskproTicketCreated, DeskproTicketMessage}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.CreateTicketResponseRequest
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.DeskproMessageFileAttachment
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{CreateTicketRequest, DeskproTicketCreationFailed, _}
import uk.gov.hmrc.apiplatformdeskpro.repository.DeskproMessageFileAttachmentRepository
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

@Singleton
class TicketService @Inject() (
    deskproConnector: DeskproConnector,
    personService: PersonService,
    deskproMessageFileAttachmentRepository: DeskproMessageFileAttachmentRepository,
    config: AppConfig,
    val clock: Clock
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger with ClockNow {

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

  def createMessage(ticketId: Int, request: CreateTicketResponseRequest)(implicit hc: HeaderCarrier): Future[DeskproMessageResponse] = {
    for {
      createResponseResult <- deskproConnector.createMessage(ticketId, request.userEmail.text, request.message)
      _                    <- saveMessageFileDetails(ticketId, createResponseResult.data.id, request.fileReference)
      _                    <- deskproConnector.updateTicketStatus(ticketId, request.status)
    } yield createResponseResult.data
  }

  private def saveMessageFileDetails(ticketId: Int, messageId: Int, fileReference: Option[String]): Future[Option[DeskproMessageFileAttachment]] = {
    fileReference match {
      case Some(fileRef) => deskproMessageFileAttachmentRepository.create(DeskproMessageFileAttachment(ticketId, messageId, fileRef, instant())) map { resp => Some(resp) }
      case _             => Future.successful(None)
    }
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
