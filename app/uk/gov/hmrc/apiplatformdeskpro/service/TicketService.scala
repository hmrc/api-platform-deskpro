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
import uk.gov.hmrc.apiplatformdeskpro.domain.models._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.CreateTicketResponseRequest
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.UploadStatus.{Failed, UploadedSuccessfully}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{BlobDetails, DeskproMessageFileAttachment, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.repository.{DeskproMessageFileAttachmentRepository, UploadedFileRepository}
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.services.{ClockNow, EitherTHelper}

case class FileAttachmentFailed(
    fileAttachment: FileAttachment,
    failed: Failed
  )

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

  private val ET = EitherTHelper.make[DeskproTicketCreationFailed]

  private val fileAttachmentWarningLabel: String = "<p><strong>File attachment warnings</strong>"

  def submitTicket(request: CreateTicketRequest)(implicit hc: HeaderCarrier): Future[Either[DeskproTicketCreationFailed, DeskproTicketCreated]] = {
    (
      for {
        uploadedFiles      <- ET.liftF(getUploadedFileDetails(request.attachments))
        messageWithWarnings = addMessageFileUploadWarnings(request.message, request.attachments, uploadedFiles)
        createDeskproTicket = createDeskproTicketRequest(request, messageWithWarnings, uploadedFiles)
        ticket             <- ET.fromEitherF(deskproConnector.createTicket(createDeskproTicket))
        ticketId            = ticket.data.id
        ticketMessages     <- ET.liftF(deskproConnector.getTicketMessages(ticketId))
        messageId           = ticketMessages.data.head.id
        _                  <- ET.liftF(saveMessageFileDetails(ticketId, messageId, request.attachments))
      } yield ticket
    ).value
  }

  private def createDeskproTicketRequest(request: CreateTicketRequest, message: String, uploadedFiles: List[UploadedFile]): CreateDeskproTicket = {
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
      DeskproTicketMessage.fromRaw(message, person, getBlobDetails(uploadedFiles)),
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
      uploadedFiles        <- getUploadedFileDetails(request.attachments)
      messageWithWarnings   = addMessageFileUploadWarnings(request.message, request.attachments, uploadedFiles)
      createResponseResult <- deskproConnector.createMessageWithAttachments(ticketId, request.userEmail, messageWithWarnings, getBlobDetails(uploadedFiles))
      _                    <- saveMessageFileDetails(ticketId, createResponseResult.data.id, request.attachments)
      _                    <- deskproConnector.updateTicketStatus(ticketId, request.status)
    } yield createResponseResult.data
  }

  private def saveMessageFileDetails(ticketId: Int, messageId: Int, attachments: List[FileAttachment]): Future[Option[DeskproMessageFileAttachment]] = {
    if (!attachments.isEmpty) {
      deskproMessageFileAttachmentRepository.create(DeskproMessageFileAttachment(ticketId, messageId, attachments, instant())) map { resp => Some(resp) }
    } else {
      Future.successful(None)
    }
  }

  private def addMessageFileUploadWarnings(message: String, attachments: List[FileAttachment], uploadedFiles: List[UploadedFile]): String = {
    checkForNotUploadedFiles(attachments, uploadedFiles) match {
      case None          => message
      case Some(warning) => s"$message$fileAttachmentWarningLabel<ul>$warning</ul></p>"
    }
  }

  private def checkForNotUploadedFiles(attachments: List[FileAttachment], uploadedFiles: List[UploadedFile]): Option[String] = {
    val failedToUploadFiles: List[FileAttachmentFailed] = uploadedFiles.map(uploadedFile =>
      uploadedFile.uploadStatus match {
        case failed: Failed => getFileAttachmentFailed(uploadedFile.fileReference, attachments, failed)
        case _              => None
      }
    ).flatten
    val filesNotYetUploaded: List[FileAttachment]       = attachments.filterNot(requestedFileAttachment =>
      uploadedFiles.exists(file => file.fileReference == requestedFileAttachment.fileReference)
    )
    (failedToUploadFiles.isEmpty, filesNotYetUploaded.isEmpty) match {
      case (true, true)   => None
      case (false, true)  => Some(failedToUploadFiles.map(file => getFileFailedToUploadMessage(file)).mkString)
      case (true, false)  => Some(filesNotYetUploaded.map(file => getFileNotYetUploadedMessage(file)).mkString)
      case (false, false) => Some(failedToUploadFiles.map(file => getFileFailedToUploadMessage(file)).mkString ++
          filesNotYetUploaded.map(file => getFileNotYetUploadedMessage(file)).mkString)
    }
  }

  private def getFileNotYetUploadedMessage(file: FileAttachment) = {
    s"<li><strong>${file.fileName}</strong> - The file is in a queue to be scanned for viruses.</li>"
  }

  private def getFileFailedToUploadMessage(file: FileAttachmentFailed) = {
    s"<li><strong>${file.fileAttachment.fileName}</strong> - ${getFailureMessage(file.failed)}</li>"
  }

  private def getFailureMessage(failed: Failed): String = {
    if (failed.reason == "REJECTED" && failed.message.contains("MIME type")) {
      "The file is not one of the accepted file types and has not been received."
    } else if (failed.reason == "QUARANTINE") {
      "The file contains a virus and has not been received."
    } else {
      "The file could not be uploaded - try again."
    }
  }

  private def getFileAttachmentFailed(fileReference: String, attachments: List[FileAttachment], failed: Failed): Option[FileAttachmentFailed] = {
    attachments.find(attachment => attachment.fileReference == fileReference).map(attachment => FileAttachmentFailed(attachment, failed))
  }

  private def getBlobDetails(uploadedFiles: List[UploadedFile]): List[BlobDetails] = {
    uploadedFiles.map(uploadedFile =>
      uploadedFile.uploadStatus match {
        case success: UploadedSuccessfully => Some(success.blobDetails)
        case _                             => None
      }
    ).flatten
  }

  private def getUploadedFileDetails(attachments: List[FileAttachment]): Future[List[UploadedFile]] = {
    for {
      uploadFiles <- Future.sequence(attachments.map(attachment => uploadedFileRepository.fetchByFileReference(attachment.fileReference)))
    } yield uploadFiles.flatten
  }

  def updateMessageAttachmentsIfRequired(fileReference: String, maybeBlobDetails: Option[BlobDetails])(implicit hc: HeaderCarrier): Future[DeskproTicketMessageResult] = {
    for {
      maybeMessage <- deskproMessageFileAttachmentRepository.fetchByFileReference(fileReference)
      result       <- updateMessageIfAlreadyCreated(fileReference, maybeMessage, maybeBlobDetails)
    } yield result
  }

  private def updateMessageIfAlreadyCreated(
      fileReference: String,
      maybeMessage: Option[DeskproMessageFileAttachment],
      maybeBlobDetails: Option[BlobDetails]
    )(implicit hc: HeaderCarrier
    ): Future[DeskproTicketMessageResult] = {
    maybeMessage match {
      case Some(messageFileAttachment) => updateMessage(fileReference, messageFileAttachment, maybeBlobDetails)
      case _                           => Future.successful(DeskproTicketMessageSuccess)
    }
  }

  private def amendMessageFileAttachmentWarnings(message: String, attachments: List[FileAttachment], uploadedFiles: List[UploadedFile]): String = {
    val userMessageWithoutWarning = if (message.contains(fileAttachmentWarningLabel)) {
      message.substring(0, message.indexOf(fileAttachmentWarningLabel))
    } else {
      message
    }
    addMessageFileUploadWarnings(userMessageWithoutWarning, attachments, uploadedFiles)
  }

  private def updateMessage(fileReference: String, messageFileAttachment: DeskproMessageFileAttachment, maybeBlobDetails: Option[BlobDetails])(implicit hc: HeaderCarrier)
      : Future[DeskproTicketMessageResult] = {
    logger.info(s"Updating message for ticketId: ${messageFileAttachment.ticketId}, messageId: ${messageFileAttachment.messageId}")

    for {
      attachments   <- deskproConnector.getMessageAttachments(messageFileAttachment.ticketId, messageFileAttachment.messageId)
      message       <- deskproConnector.getMessage(messageFileAttachment.ticketId, messageFileAttachment.messageId)
      uploadedFiles <- getUploadedFileDetails(messageFileAttachment.attachments)
      amendedMessage = amendMessageFileAttachmentWarnings(message.data.message, messageFileAttachment.attachments, uploadedFiles)
      result        <- deskproConnector.updateMessage(
                         messageFileAttachment.ticketId,
                         messageFileAttachment.messageId,
                         amendedMessage,
                         attachments.data,
                         maybeBlobDetails
                       )
    } yield result
  }

  def deleteTicket(ticketId: Int)(implicit hc: HeaderCarrier): Future[DeskproTicketUpdateResult] = {
    deskproConnector.deleteTicket(ticketId)
  }
}
