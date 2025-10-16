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
import scala.concurrent.Future

import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.{FailedCallbackBody, ReadyCallbackBody, UpscanCallbackBody}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{UploadStatus, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.repository.UploadedFileRepository
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger

import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

@Singleton
class UpscanCallbackDispatcher @Inject() (
    uploadedFileRepository: UploadedFileRepository,
    val clock: Clock
  ) extends ApplicationLogger with ClockNow {

  def handleCallback(callback: UpscanCallbackBody): Future[UploadedFile] = {
    val uploadStatus = callback match {
      case r: ReadyCallbackBody  =>
        logger.info(s"Upscan callback upload ready: $r")
        UploadStatus.UploadedSuccessfully(
          name = r.uploadDetails.fileName,
          mimeType = r.uploadDetails.fileMimeType,
          downloadUrl = r.downloadUrl,
          size = r.uploadDetails.size
        )
      case f: FailedCallbackBody =>
        logger.info(s"Upscan callback upload failed: $f")
        UploadStatus.Failed(
          message = f.failureDetails.message,
          reason = f.failureDetails.failureReason
        )
    }
    uploadedFileRepository.create(UploadedFile(callback.reference.value, uploadStatus, instant()))
  }
}
