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

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.connector.DeskproConnector
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.CreateTicketResponseRequest
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.UploadStatus.UploadedSuccessfully
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{BlobDetails, DeskproMessageFileAttachment, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{CreateTicketRequest, DeskproTicketCreationFailed, _}
import uk.gov.hmrc.apiplatformdeskpro.repository.{DeskproMessageFileAttachmentRepository, UploadedFileRepository}
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

@Singleton
class TicketService @Inject() (
    deskproConnector: DeskproConnector,
    personService: PersonService,
    deskproMessageFileAttachmentRepository: DeskproMessageFileAttachmentRepository,
    uploadedFileRepository: UploadedFileRepository,
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

  def createMessage(ticketId: Int, request: CreateTicketResponseRequest)(implicit hc: HeaderCarrier): Future[DeskproCreateMessageResponse] = {
    for {
      uploadedFiles        <- getUploadedFileDetails(request.fileReferences)
      createResponseResult <- deskproConnector.createMessageWithAttachments(ticketId, request.userEmail, request.message, getBlobDetails(uploadedFiles))
      _                    <- saveMessageFileDetails(ticketId, createResponseResult.data.id, request.fileReferences)
      _                    <- deskproConnector.updateTicketStatus(ticketId, request.status)
    } yield createResponseResult.data
  }

  private def saveMessageFileDetails(ticketId: Int, messageId: Int, fileReferences: List[String]): Future[Option[DeskproMessageFileAttachment]] = {
    if (!fileReferences.isEmpty) {
      deskproMessageFileAttachmentRepository.create(DeskproMessageFileAttachment(ticketId, messageId, fileReferences, instant())) map { resp => Some(resp) }
    } else {
      Future.successful(None)
    }
  }

  private def getBlobDetails(uploadedFiles: List[UploadedFile]): List[BlobDetails] = {
    uploadedFiles.map(uploadedFile =>
      uploadedFile.uploadStatus match {
        case success: UploadedSuccessfully => Some(success.blobDetails)
        case _                             => None
      }
    ).flatten
  }

  private def getUploadedFileDetails(fileReferences: List[String]): Future[List[UploadedFile]] = {
    for {
      uploadFiles <- Future.sequence(fileReferences.map(fileRef => uploadedFileRepository.fetchByFileReference(fileRef)))
    } yield uploadFiles.flatten
  }

  def updateMessageAddAttachmentIfRequired(fileReference: String, blobDetails: BlobDetails)(implicit hc: HeaderCarrier): Future[DeskproTicketMessageResult] = {
    for {
      maybeMessage <- deskproMessageFileAttachmentRepository.fetchByFileReference(fileReference)
      result       <- updateMessageIfAlreadyCreated(fileReference, maybeMessage, blobDetails)
    } yield result
  }

  private def updateMessageIfAlreadyCreated(fileReference: String, maybeMessage: Option[DeskproMessageFileAttachment], blobDetails: BlobDetails)(implicit hc: HeaderCarrier)
      : Future[DeskproTicketMessageResult] = {
    maybeMessage match {
      case Some(message) => updateMessageAttachments(fileReference, message.ticketId, message.messageId, blobDetails)
      case _             => Future.successful(DeskproTicketMessageSuccess)
    }
  }

  private def updateMessageAttachments(fileReference: String, ticketId: Int, messageId: Int, blobDetails: BlobDetails)(implicit hc: HeaderCarrier)
      : Future[DeskproTicketMessageResult] = {
    def checkAttachmentsContains(attachments: DeskproAttachmentsWrapperResponse, blobDetails: BlobDetails) = {
      attachments.data.exists(attachment => (attachment.blob.blob_id == blobDetails.blobId && attachment.blob.blob_auth == blobDetails.blobAuth))
    }
    def addNewAttachmentIfNotPresent(
        ticketId: Int,
        messageId: Int,
        existingAttachments: DeskproAttachmentsWrapperResponse,
        blobDetails: BlobDetails
      )(implicit hc: HeaderCarrier
      ) = {
      if (!checkAttachmentsContains(existingAttachments, blobDetails)) {
        logger.debug(s"Updating message attachments for ticketId: $ticketId, messageId: $messageId - adding file ref: $fileReference")
        deskproConnector.updateMessageAttachments(ticketId, messageId, existingAttachments.data, blobDetails.blobId, blobDetails.blobAuth)
      } else {
        Future.successful(DeskproTicketMessageSuccess)
      }
    }

    logger.debug(s"Checking message attachments for ticketId: $ticketId, messageId: $messageId")

    for {
      attachments <- deskproConnector.getMessageAttachments(ticketId, messageId)
      result      <- addNewAttachmentIfNotPresent(ticketId, messageId, attachments, blobDetails)
    } yield result
  }

  def deleteTicket(ticketId: Int)(implicit hc: HeaderCarrier): Future[DeskproTicketUpdateResult] = {
    deskproConnector.deleteTicket(ticketId)
  }
}
