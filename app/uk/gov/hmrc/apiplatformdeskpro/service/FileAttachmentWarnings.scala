/*
 * Copyright 2026 HM Revenue & Customs
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

import uk.gov.hmrc.apiplatformdeskpro.domain.models.FileAttachment
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.UploadStatus.{Failed, PendingUploadToDeskpro}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.UploadedFile

case class FileAttachmentFailed(
    fileAttachment: FileAttachment,
    failed: Failed
  )

case class FileAttachmentPending(
    fileAttachment: FileAttachment,
    pending: PendingUploadToDeskpro
  )

object FileAttachmentWarnings {

  private val fileAttachmentWarningLabel: String = "<h4 class='govuk-heading-s govuk-!-margin-bottom-1'>Attached files</h4>"

  def addMessageFileUploadWarnings(message: String, attachments: List[FileAttachment], uploadedFiles: List[UploadedFile]): String = {
    checkForNotUploadedFiles(attachments, uploadedFiles) match {
      case None          => message
      case Some(warning) => s"$message$fileAttachmentWarningLabel$warning"
    }
  }

  def amendMessageFileAttachmentWarnings(message: String, attachments: List[FileAttachment], uploadedFiles: List[UploadedFile]): String = {
    val userMessageWithoutWarning = if (message.contains(fileAttachmentWarningLabel)) {
      message.substring(0, message.indexOf(fileAttachmentWarningLabel))
    } else {
      message
    }
    addMessageFileUploadWarnings(userMessageWithoutWarning, attachments, uploadedFiles)
  }

  private def checkForNotUploadedFiles(attachments: List[FileAttachment], uploadedFiles: List[UploadedFile]): Option[String] = {
    val failedToUploadFiles: List[FileAttachmentFailed] = uploadedFiles.map(uploadedFile =>
      uploadedFile.uploadStatus match {
        case failed: Failed => getFileAttachmentFailed(uploadedFile.fileReference, attachments, failed)
        case _              => None
      }
    ).flatten
    val pendingUploadFiles: List[FileAttachmentPending] = uploadedFiles.map(uploadedFile =>
      uploadedFile.uploadStatus match {
        case pending: PendingUploadToDeskpro => getFileAttachmentPending(uploadedFile.fileReference, attachments, pending)
        case _                               => None
      }
    ).flatten
    val filesNotYetUploaded: List[FileAttachment]       = attachments.filterNot(requestedFileAttachment =>
      uploadedFiles.exists(file => file.fileReference == requestedFileAttachment.fileReference)
    )
    if (failedToUploadFiles.isEmpty && pendingUploadFiles.isEmpty && filesNotYetUploaded.isEmpty) {
      None
    } else {
      Some(failedToUploadFiles.map(file => getFileFailedToUploadMessage(file)).mkString ++
        pendingUploadFiles.map(file => getFilePendingUploadMessage(file)).mkString ++
        filesNotYetUploaded.map(file => getFileNotYetUploadedMessage(file)).mkString)
    }
  }

  private def getFileNotYetUploadedMessage(file: FileAttachment) = {
    s"${file.fileName}<br><p class='govuk-body govuk-!-margin-bottom-0 govuk-!-margin-top-1 govuk-!-font-size-16'>The file is in a queue to be scanned for viruses.</p><hr class='govuk-section-break govuk-!-margin-top-2 govuk-!-margin-bottom-3 govuk-section-break--visible'>"
  }

  private def getFileFailedToUploadMessage(file: FileAttachmentFailed) = {
    s"${file.fileAttachment.fileName}<br><p class='govuk-body govuk-!-margin-bottom-0 govuk-!-margin-top-1 govuk-!-font-size-16'>${getFailureMessage(
        file.failed
      )}</p><hr class='govuk-section-break govuk-!-margin-top-2 govuk-!-margin-bottom-3 govuk-section-break--visible'>"
  }

  private def getFilePendingUploadMessage(file: FileAttachmentPending) = {
    s"${file.fileAttachment.fileName}<br><p class='govuk-body govuk-!-margin-bottom-0 govuk-!-margin-top-1 govuk-!-font-size-16'>The file has passed virus scanning and is awaiting upload.</p><hr class='govuk-section-break govuk-!-margin-top-2 govuk-!-margin-bottom-3 govuk-section-break--visible'>"
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

  private def getFileAttachmentPending(fileReference: String, attachments: List[FileAttachment], pending: PendingUploadToDeskpro): Option[FileAttachmentPending] = {
    attachments.find(attachment => attachment.fileReference == fileReference).map(attachment => FileAttachmentPending(attachment, pending))
  }
}
