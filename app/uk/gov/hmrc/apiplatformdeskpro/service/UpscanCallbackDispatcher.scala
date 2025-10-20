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

import uk.gov.hmrc.apiplatformdeskpro.connector.{DeskproConnector, UpscanDownloadConnector}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.{FailedCallbackBody, ReadyCallbackBody, UpscanCallbackBody}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{BlobDetails, UploadStatus, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.repository.UploadedFileRepository
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

@Singleton
class UpscanCallbackDispatcher @Inject() (
    uploadedFileRepository: UploadedFileRepository,
    ticketService: TicketService,
    deskproConnector: DeskproConnector,
    upscanDownloadConnector: UpscanDownloadConnector,
    val clock: Clock
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger with ClockNow {

  def handleCallback(callback: UpscanCallbackBody)(implicit hc: HeaderCarrier): Future[UploadedFile] = {
    callback match {
      case r: ReadyCallbackBody  => handleSuccessfulCallback(r)
      case f: FailedCallbackBody => handleFailedCallback(f)
    }
  }

  private def handleSuccessfulCallback(readyCallBack: ReadyCallbackBody)(implicit hc: HeaderCarrier) = {
    logger.debug(s"Upscan callback upload ready: $readyCallBack")
    for {
      source       <- upscanDownloadConnector.stream(readyCallBack.downloadUrl)
      blobResponse <- deskproConnector.createBlob(readyCallBack.uploadDetails.fileName, readyCallBack.uploadDetails.fileMimeType, source)
      uploadStatus  = UploadStatus.UploadedSuccessfully(
                        name = readyCallBack.uploadDetails.fileName,
                        mimeType = readyCallBack.uploadDetails.fileMimeType,
                        downloadUrl = readyCallBack.downloadUrl,
                        size = readyCallBack.uploadDetails.size,
                        blobDetails = BlobDetails(blobResponse.data.blob_id, blobResponse.data.blob_auth)
                      )
      uploadedFile <- uploadedFileRepository.create(UploadedFile(readyCallBack.reference.value, uploadStatus, instant()))
      result       <- ticketService.updateMessageAddAttachmentIfRequired(readyCallBack.reference.value, uploadStatus.blobDetails)
    } yield uploadedFile
  }

  private def handleFailedCallback(failedCallBack: FailedCallbackBody) = {
    logger.info(s"Upscan callback upload failed: $failedCallBack")
    uploadedFileRepository.create(UploadedFile(
      failedCallBack.reference.value,
      UploadStatus.Failed(failedCallBack.failureDetails.message, failedCallBack.failureDetails.failureReason),
      instant()
    ))
  }
}
