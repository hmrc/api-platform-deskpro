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

import java.net.URL
import scala.concurrent.Future

import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.{ErrorDetails, FailedCallbackBody, ReadyCallbackBody, Reference, UploadDetails}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{UploadStatus, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.repository.UploadedFileRepository
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class UpscanCallbackDispatcherSpec extends AsyncHmrcSpec with FixedClock {

  trait Setup {
    val mockUploadedFileRepository = mock[UploadedFileRepository]

    val fileReference  = Reference("fileRef")
    val url            = new URL("https://example.com/file/0001")
    val uploadDetails  = UploadDetails(instant, "checksum", "text/plain", "filename", 1000)
    val callbackReady  = ReadyCallbackBody(fileReference, url, uploadDetails)
    val errorDetails   = ErrorDetails("failureReason", "message")
    val callbackFailed = FailedCallbackBody(fileReference, errorDetails)

    val uploadSuccess   = UploadStatus.UploadedSuccessfully("filename", "text/plain", url, 1000)
    val uploadedSuccess = UploadedFile(fileReference.value, uploadSuccess, instant)
    val uploadFailed    = UploadStatus.Failed("message", "failureReason")
    val uploadedFailed  = UploadedFile(fileReference.value, uploadFailed, instant)

    val underTest = new UpscanCallbackDispatcher(mockUploadedFileRepository, clock)
  }

  "handleCallback" should {
    "successfully add a ready callback to the uploaded files repository" in new Setup {

      when(mockUploadedFileRepository.create(*)).thenReturn(Future.successful(uploadedSuccess))

      val result = await(underTest.handleCallback(callbackReady))

      result shouldBe uploadedSuccess
      verify(mockUploadedFileRepository).create(eqTo(uploadedSuccess))
    }

    "successfully add a failed callback to the uploaded files repository" in new Setup {

      when(mockUploadedFileRepository.create(*)).thenReturn(Future.successful(uploadedFailed))

      val result = await(underTest.handleCallback(callbackFailed))

      result shouldBe uploadedFailed
      verify(mockUploadedFileRepository).create(eqTo(uploadedFailed))
    }
  }
}
