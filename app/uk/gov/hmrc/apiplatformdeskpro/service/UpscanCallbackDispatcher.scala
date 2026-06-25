/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.DeskproTicketMessageResult
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

  def handleCallback(callback: UpscanCallbackBody)(implicit hc: HeaderCarrier): Future[DeskproTicketMessageResult] = {
    callback match {
      case r: ReadyCallbackBody  => handleSuccessfulCallback(r)
      case f: FailedCallbackBody => handleFailedCallback(f)
    }
  }

  private def handleSuccessfulCallback(readyCallBack: ReadyCallbackBody)(implicit hc: HeaderCarrier): Future[DeskproTicketMessageResult] = {
    logger.info(s"Upscan callback upload ready: ${readyCallBack.reference.value} - fileName: ${readyCallBack.uploadDetails.fileName}, fileType: ${readyCallBack.uploadDetails.fileMimeType}, size: ${readyCallBack.uploadDetails.size}")
    def getUploadStatusSuccess(blobDetails: BlobDetails)                        = {
      UploadStatus.UploadedSuccessfully(
        name = readyCallBack.uploadDetails.fileName,
        mimeType = readyCallBack.uploadDetails.fileMimeType,
        downloadUrl = readyCallBack.downloadUrl,
        size = readyCallBack.uploadDetails.size,
        blobDetails = blobDetails
      )
    }
    def getUploadStatusPending(maybePreviousUploadedFile: Option[UploadedFile]) = {
      val attempt: Int = maybePreviousUploadedFile match {
        case Some(uploadedFile) => uploadedFile.uploadStatus match {
            case pending: UploadStatus.PendingUploadToDeskpro => pending.attempt + 1
            case _                                            => 1
          }
        case _                  => 1
      }
      UploadStatus.PendingUploadToDeskpro(
        name = readyCallBack.uploadDetails.fileName,
        mimeType = readyCallBack.uploadDetails.fileMimeType,
        downloadUrl = readyCallBack.downloadUrl,
        size = readyCallBack.uploadDetails.size,
        attempt = attempt
      )
    }

    for {
      maybePreviousUploadedFile <- uploadedFileRepository.fetchByFileReference(readyCallBack.reference.value)
      _                         <- uploadedFileRepository.createOrUpdate(UploadedFile(readyCallBack.reference.value, getUploadStatusPending(maybePreviousUploadedFile), instant))
      source                    <- upscanDownloadConnector.stream(readyCallBack.downloadUrl)
      blobResponseWrapper       <- deskproConnector.createBlob(readyCallBack.uploadDetails.fileName, readyCallBack.uploadDetails.fileMimeType, readyCallBack.uploadDetails.size, source)
      blobDetails                = BlobDetails(blobResponseWrapper.data.blob_id, blobResponseWrapper.data.blob_auth)
      newUploadedFile           <- uploadedFileRepository.createOrUpdate(UploadedFile(readyCallBack.reference.value, getUploadStatusSuccess(blobDetails), instant))
      result                    <- ticketService.updateMessageAttachmentsIfRequired(readyCallBack.reference.value, Some(blobDetails))
    } yield result
  }

  private def handleFailedCallback(failedCallBack: FailedCallbackBody)(implicit hc: HeaderCarrier): Future[DeskproTicketMessageResult] = {
    logger.info(s"Upscan callback upload failed: ${failedCallBack.reference.value} - ${failedCallBack.failureDetails.message}, ${failedCallBack.failureDetails.failureReason}")
    for {
      uploadedFile <- uploadedFileRepository.createOrUpdate(UploadedFile(
                        failedCallBack.reference.value,
                        UploadStatus.Failed(failedCallBack.failureDetails.message, failedCallBack.failureDetails.failureReason),
                        instant
                      ))
      result       <- ticketService.updateMessageAttachmentsIfRequired(failedCallBack.reference.value, None)
    } yield result
  }
}
