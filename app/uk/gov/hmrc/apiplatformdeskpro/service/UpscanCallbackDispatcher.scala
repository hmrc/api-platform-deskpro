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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.DeskproCreateBlobResponse
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.{FailedCallbackBody, ReadyCallbackBody, UpscanCallbackBody}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{BlobDetails, UploadStatus, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.repository.UploadedFileRepository
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

@Singleton
class UpscanCallbackDispatcher @Inject() (
    uploadedFileRepository: UploadedFileRepository,
    deskproConnector: DeskproConnector,
    upscanDownloadConnector: UpscanDownloadConnector,
    val clock: Clock
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger with ClockNow {

  def handleCallback(callback: UpscanCallbackBody)(implicit hc: HeaderCarrier): Future[UploadedFile] = {
    def getUploadStatus(maybeBlobResponse: Option[DeskproCreateBlobResponse]): UploadStatus = {
      (callback, maybeBlobResponse) match {
        case (r: ReadyCallbackBody, Some(blobResponse)) =>
          logger.info(s"Upscan callback upload ready: $r")
          UploadStatus.UploadedSuccessfully(
            name = r.uploadDetails.fileName,
            mimeType = r.uploadDetails.fileMimeType,
            downloadUrl = r.downloadUrl,
            size = r.uploadDetails.size,
            blobDetails = Some(BlobDetails(blobResponse.blob_id, blobResponse.blob_auth))
          )
        case (f: FailedCallbackBody, _)                 =>
          logger.info(s"Upscan callback upload failed: $f")
          UploadStatus.Failed(
            message = f.failureDetails.message,
            reason = f.failureDetails.failureReason
          )
      }
    }

    for {
      maybeBlobResponse <- createBlob(callback)
      uploadStatus       = getUploadStatus(maybeBlobResponse)
      uploadedFile      <- uploadedFileRepository.create(UploadedFile(callback.reference.value, uploadStatus, instant()))
    } yield uploadedFile
  }

  def createBlob(callback: UpscanCallbackBody)(implicit hc: HeaderCarrier): Future[Option[DeskproCreateBlobResponse]] = {
    callback match {
      case r: ReadyCallbackBody  => {
        for {
          source       <- upscanDownloadConnector.stream(r.downloadUrl)
          blobResponse <- deskproConnector.createBlob(r.uploadDetails.fileName, r.uploadDetails.fileMimeType, source)
        } yield Some(blobResponse.data)
      }
      case f: FailedCallbackBody => {
        Future.successful(None)
      }
    }
  }
}
