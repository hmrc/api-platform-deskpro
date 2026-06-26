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

import java.net.URL

import uk.gov.hmrc.apiplatformdeskpro.domain.models.FileAttachment
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.UploadStatus.{Failed, FailedUploadToDeskpro, PendingUploadToDeskpro, UploadedSuccessfully}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{BlobDetails, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class FileAttachmentWarningsSpec extends AsyncHmrcSpec with FixedClock {

  val message                   = "Here's a message"
  val fileReference             = "fileRef"
  val fileName                  = "fileName.txt"
  val fileAttachment            = FileAttachment(fileReference, fileName)
  val blobId                    = 67890
  val blobAuth                  = "FHFK558GGDGFD45465GHJGJ"
  val uploadStatusFailed        = Failed("MIME type [application/zip] is not allowed for service: [devhub-support-frontend]", "REJECTED")
  val uploadedFileFailed        = UploadedFile(fileReference, uploadStatusFailed, instant)
  val uploadStatusPending       = PendingUploadToDeskpro("fileName", "text/plain", new URL("https://example.com/file1"), 1000, 1)
  val uploadedFilePending       = UploadedFile(fileReference, uploadStatusPending, instant)
  val deskproUploadStatusFailed = FailedUploadToDeskpro("fileName", "text/plain", new URL("https://example.com/file1"), 1000, 1, "error message")
  val deskproUploadedFileFailed = UploadedFile(fileReference, deskproUploadStatusFailed, instant)
  val uploadStatusSuccess       = UploadedSuccessfully("fileName", "text/plain", new URL("https://example.com/file1"), 1000, BlobDetails(blobId, blobAuth))
  val uploadedFileSuccess       = UploadedFile(fileReference, uploadStatusSuccess, instant)

  "addMessageFileUploadWarnings" should {
    "successfully add warnings to the message where an upload has failed in upscan" in {
      val result = FileAttachmentWarnings.addMessageFileUploadWarnings(message, List(fileAttachment), List(uploadedFileFailed))

      result shouldBe s"${message}<h4 class='govuk-heading-s govuk-!-margin-bottom-1'>Attached files</h4>${fileName}<br><p class='govuk-body govuk-!-margin-bottom-0 govuk-!-margin-top-1 govuk-!-font-size-16'>The file is not one of the accepted file types and has not been received.</p><hr class='govuk-section-break govuk-!-margin-top-2 govuk-!-margin-bottom-3 govuk-section-break--visible'>"
    }

    "successfully add warnings to the message where an upload to Deskpro is pending" in {
      val result = FileAttachmentWarnings.addMessageFileUploadWarnings(message, List(fileAttachment), List(uploadedFilePending))

      result shouldBe s"${message}<h4 class='govuk-heading-s govuk-!-margin-bottom-1'>Attached files</h4>${fileName}<br><p class='govuk-body govuk-!-margin-bottom-0 govuk-!-margin-top-1 govuk-!-font-size-16'>The file has passed virus scanning and is awaiting upload.</p><hr class='govuk-section-break govuk-!-margin-top-2 govuk-!-margin-bottom-3 govuk-section-break--visible'>"
    }

    "successfully add warnings to the message where an upload to Deskpro has failed" in {
      val result = FileAttachmentWarnings.addMessageFileUploadWarnings(message, List(fileAttachment), List(deskproUploadedFileFailed))

      result shouldBe s"${message}<h4 class='govuk-heading-s govuk-!-margin-bottom-1'>Attached files</h4>${fileName}<br><p class='govuk-body govuk-!-margin-bottom-0 govuk-!-margin-top-1 govuk-!-font-size-16'>The file has passed virus scanning but has failed to upload.</p><hr class='govuk-section-break govuk-!-margin-top-2 govuk-!-margin-bottom-3 govuk-section-break--visible'>"
    }

    "successfully add warnings to the message where a file has not been uploaded to upscan yet" in {
      val result = FileAttachmentWarnings.addMessageFileUploadWarnings(message, List(fileAttachment), List.empty)

      result shouldBe s"${message}<h4 class='govuk-heading-s govuk-!-margin-bottom-1'>Attached files</h4>${fileName}<br><p class='govuk-body govuk-!-margin-bottom-0 govuk-!-margin-top-1 govuk-!-font-size-16'>The file is in a queue to be scanned for viruses.</p><hr class='govuk-section-break govuk-!-margin-top-2 govuk-!-margin-bottom-3 govuk-section-break--visible'>"
    }
  }

  "amendMessageFileAttachmentWarnings" should {
    "successfully remove warnings from the message where an upload which had previously not been uploaded to upscan has now succeeded" in {
      val messageWithWarnings =
        s"${message}<h4 class='govuk-heading-s govuk-!-margin-bottom-1'>Attached files</h4>${fileName}<br><p class='govuk-body govuk-!-margin-bottom-0 govuk-!-margin-top-1 govuk-!-font-size-16'>The file is in a queue to be scanned for viruses.</p><hr class='govuk-section-break govuk-!-margin-top-2 govuk-!-margin-bottom-3 govuk-section-break--visible'>"
      val result              = FileAttachmentWarnings.amendMessageFileAttachmentWarnings(messageWithWarnings, List(fileAttachment), List(uploadedFileSuccess))

      result shouldBe message
    }
  }
}
