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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.{FailedCallbackBody, ReadyCallbackBody, UpscanCallbackBody}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{BlobDetails, UploadStatus, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproBlobCreationFailure, DeskproBlobCreationResult, DeskproBlobCreationSuccess, DeskproTicketMessageResult}
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
    def getAttempt(maybePreviousUploadedFile: Option[UploadedFile]): Int                                                = {
      maybePreviousUploadedFile match {
        case Some(uploadedFile) => uploadedFile.uploadStatus match {
            case failed: UploadStatus.FailedUploadToDeskpro => failed.attempt + 1
            case _                                          => 1
          }
        case _                  => 1
      }
    }
    def getUploadStatus(blobResponseResult: DeskproBlobCreationResult, maybePreviousUploadedFile: Option[UploadedFile]) = {
      blobResponseResult match {
        case DeskproBlobCreationSuccess(blobResponse) => UploadStatus.UploadedSuccessfully(
            name = readyCallBack.uploadDetails.fileName,
            mimeType = readyCallBack.uploadDetails.fileMimeType,
            downloadUrl = readyCallBack.downloadUrl,
            size = readyCallBack.uploadDetails.size,
            blobDetails = BlobDetails(blobResponse.blob_id, blobResponse.blob_auth)
          )
        case DeskproBlobCreationFailure(message)      => UploadStatus.FailedUploadToDeskpro(
            name = readyCallBack.uploadDetails.fileName,
            mimeType = readyCallBack.uploadDetails.fileMimeType,
            downloadUrl = readyCallBack.downloadUrl,
            size = readyCallBack.uploadDetails.size,
            attempt = getAttempt(maybePreviousUploadedFile),
            error = message
          )
      }
    }
    def getBlobDetails(blobResponseResult: DeskproBlobCreationResult): Option[BlobDetails]                              = {
      blobResponseResult match {
        case DeskproBlobCreationSuccess(blobResponse) => Some(BlobDetails(blobResponse.blob_id, blobResponse.blob_auth))
        case _                                        => None
      }
    }

    for {
      maybePreviousUploadedFile <- uploadedFileRepository.fetchByFileReference(readyCallBack.reference.value)
      _                          =
        logger.info(s"Uploading file to deskpro - : ${readyCallBack.reference.value} - fileName: ${readyCallBack.uploadDetails.fileName}, attempt: ${getAttempt(maybePreviousUploadedFile)}")
      source                    <- upscanDownloadConnector.stream(readyCallBack.downloadUrl)
      blobResponseResult        <- deskproConnector.createBlob(
                                     readyCallBack.uploadDetails.fileName,
                                     readyCallBack.uploadDetails.fileMimeType,
                                     readyCallBack.uploadDetails.size,
                                     readyCallBack.reference.value,
                                     source
                                   )
      newUploadedFile           <- uploadedFileRepository.createOrUpdate(UploadedFile(readyCallBack.reference.value, getUploadStatus(blobResponseResult, maybePreviousUploadedFile), instant))
      result                    <- ticketService.updateMessageAttachmentsIfRequired(readyCallBack.reference.value, getBlobDetails(blobResponseResult))
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
