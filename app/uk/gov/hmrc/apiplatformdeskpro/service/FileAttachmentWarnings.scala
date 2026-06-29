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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.UploadStatus.{Failed, FailedUploadToDeskpro}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.UploadedFile

case class FileAttachmentUpscanFailed(
    fileAttachment: FileAttachment,
    failed: Failed
  )

case class FileAttachmentUploadFailed(
    fileAttachment: FileAttachment,
    pending: FailedUploadToDeskpro
  )

object FileAttachmentWarnings {

  private val fileAttachmentWarningLabel: String = "<h4 class=\"govuk-heading-s govuk-!-margin-bottom-1\">Attached files</h4>"

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
    val failedUpscanFiles: List[FileAttachmentUpscanFailed]          = uploadedFiles.map(uploadedFile =>
      uploadedFile.uploadStatus match {
        case failed: Failed => getFileAttachmentUpscanFailed(uploadedFile.fileReference, attachments, failed)
        case _              => None
      }
    ).flatten
    val failedUploadToDeskproFiles: List[FileAttachmentUploadFailed] = uploadedFiles.map(uploadedFile =>
      uploadedFile.uploadStatus match {
        case failed: FailedUploadToDeskpro => getFileAttachmentFailedUploadToDeskpro(uploadedFile.fileReference, attachments, failed)
        case _                             => None
      }
    ).flatten
    val filesNotYetUploaded: List[FileAttachment]                    = attachments.filterNot(requestedFileAttachment =>
      uploadedFiles.exists(file => file.fileReference == requestedFileAttachment.fileReference)
    )
    if (failedUpscanFiles.isEmpty && filesNotYetUploaded.isEmpty && failedUploadToDeskproFiles.isEmpty) {
      None
    } else {
      Some(failedUpscanFiles.map(file => getFileFailedUpscanMessage(file)).mkString ++
        failedUploadToDeskproFiles.map(file => getFileFailedUploadMessage(file)).mkString ++
        filesNotYetUploaded.map(file => getFileNotYetUploadedMessage(file)).mkString)
    }
  }

  private def getFileNotYetUploadedMessage(file: FileAttachment) = {
    s"${file.fileName}<br><p class='govuk-body govuk-!-margin-bottom-0 govuk-!-margin-top-1 govuk-!-font-size-16'>The file is in a queue to be scanned for viruses.</p><hr class='govuk-section-break govuk-!-margin-top-2 govuk-!-margin-bottom-3 govuk-section-break--visible'>"
  }

  private def getFileFailedUpscanMessage(file: FileAttachmentUpscanFailed) = {
    s"${file.fileAttachment.fileName}<br><p class='govuk-body govuk-!-margin-bottom-0 govuk-!-margin-top-1 govuk-!-font-size-16'>${getFailureMessage(
        file.failed
      )}</p><hr class='govuk-section-break govuk-!-margin-top-2 govuk-!-margin-bottom-3 govuk-section-break--visible'>"
  }

  private def getFileFailedUploadMessage(file: FileAttachmentUploadFailed) = {
    s"${file.fileAttachment.fileName}<br><p class='govuk-body govuk-!-margin-bottom-0 govuk-!-margin-top-1 govuk-!-font-size-16'>The file has passed virus scanning but has failed to upload.</p><hr class='govuk-section-break govuk-!-margin-top-2 govuk-!-margin-bottom-3 govuk-section-break--visible'>"
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

  private def getFileAttachmentUpscanFailed(fileReference: String, attachments: List[FileAttachment], failed: Failed): Option[FileAttachmentUpscanFailed] = {
    attachments.find(attachment => attachment.fileReference == fileReference).map(attachment => FileAttachmentUpscanFailed(attachment, failed))
  }

  private def getFileAttachmentFailedUploadToDeskpro(fileReference: String, attachments: List[FileAttachment], failed: FailedUploadToDeskpro)
      : Option[FileAttachmentUploadFailed] = {
    attachments.find(attachment => attachment.fileReference == fileReference).map(attachment => FileAttachmentUploadFailed(attachment, failed))
  }
}
