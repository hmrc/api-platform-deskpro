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

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

import uk.gov.hmrc.apiplatformdeskpro.connector.{DeskproConnector, UpscanDownloadConnector}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.DeskproCreateBlobResponse
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.{ErrorDetails, FailedCallbackBody, ReadyCallbackBody, Reference, UploadDetails}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{BlobDetails, UploadStatus, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproBlobCreationFailure, DeskproBlobCreationSuccess, DeskproTicketMessageSuccess}
import uk.gov.hmrc.apiplatformdeskpro.repository.UploadedFileRepository
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class UpscanCallbackDispatcherSpec extends AsyncHmrcSpec with FixedClock {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockUploadedFileRepository  = mock[UploadedFileRepository]
    val mockUpscanDownloadConnector = mock[UpscanDownloadConnector]
    val mockDeskproConnector        = mock[DeskproConnector]
    val mockTicketService           = mock[TicketService]

    val fileReference  = Reference("fileRef")
    val url            = new URL("https://example.com/file/0001")
    val uploadDetails  = UploadDetails(instant, "checksum", "text/plain", "filename", 1000)
    val callbackReady  = ReadyCallbackBody(fileReference, url, uploadDetails)
    val errorDetails   = ErrorDetails("failureReason", "message")
    val callbackFailed = FailedCallbackBody(fileReference, errorDetails)

    val uploadSuccess         = UploadStatus.UploadedSuccessfully("filename", "text/plain", url, 1000, BlobDetails(1234, "auth"))
    val uploadedSuccess       = UploadedFile(fileReference.value, uploadSuccess, instant)
    val uploadFailed          = UploadStatus.Failed("message", "failureReason")
    val uploadedFailed        = UploadedFile(fileReference.value, uploadFailed, instant)
    val uploadPending         = UploadStatus.PendingUploadToDeskpro("filename", "text/plain", url, 1000, 1)
    val uploadedPending       = UploadedFile(fileReference.value, uploadPending, instant)
    val deskproUploadFailed   = UploadStatus.FailedUploadToDeskpro("filename", "text/plain", url, 1000, 1, "error message")
    val deskproUploadedFailed = UploadedFile(fileReference.value, deskproUploadFailed, instant)

    val blobResponse      = DeskproCreateBlobResponse(1234, "auth")
    val blobReturnSuccess = DeskproBlobCreationSuccess(blobResponse)
    val stream            = mock[Source[ByteString, ?]]

    val underTest = new UpscanCallbackDispatcher(mockUploadedFileRepository, mockTicketService, mockDeskproConnector, mockUpscanDownloadConnector, clock)
  }

  "handleCallback" should {
    "successfully add a ready callback to the uploaded files repository" in new Setup {

      when(mockUploadedFileRepository.fetchByFileReference(*)).thenReturn(Future.successful(None))
      when(mockUploadedFileRepository.createOrUpdate(*)).thenReturn(Future.successful(uploadedSuccess))
      when(mockUpscanDownloadConnector.stream(*)(*)).thenReturn(Future.successful(stream))
      when(mockDeskproConnector.createBlob(*, *, *, *, *)(*)).thenReturn(Future.successful(blobReturnSuccess))
      when(mockTicketService.updateMessageAttachmentsIfRequired(*, *)(*)).thenReturn(Future.successful(DeskproTicketMessageSuccess))

      val result = await(underTest.handleCallback(callbackReady))

      result shouldBe DeskproTicketMessageSuccess
      verify(mockUploadedFileRepository).createOrUpdate(eqTo(uploadedPending))
      verify(mockUploadedFileRepository).createOrUpdate(eqTo(uploadedSuccess))
      verify(mockUpscanDownloadConnector).stream(eqTo(url))(*)
      verify(mockDeskproConnector).createBlob(eqTo("filename"), eqTo("text/plain"), eqTo(1000), eqTo(fileReference.value), eqTo(stream))(*)
      verify(mockTicketService).updateMessageAttachmentsIfRequired(eqTo(fileReference.value), eqTo(Some(BlobDetails(1234, "auth"))))(*)
    }

    "successfully add a ready callback to the uploaded files repository where one exists already" in new Setup {

      val uploadPending2   = UploadStatus.PendingUploadToDeskpro("filename", "text/plain", url, 1000, 2)
      val uploadedPending2 = UploadedFile(fileReference.value, uploadPending2, instant)

      when(mockUploadedFileRepository.fetchByFileReference(*)).thenReturn(Future.successful(Some(uploadedPending)))
      when(mockUploadedFileRepository.createOrUpdate(*)).thenReturn(Future.successful(uploadedSuccess))
      when(mockUpscanDownloadConnector.stream(*)(*)).thenReturn(Future.successful(stream))
      when(mockDeskproConnector.createBlob(*, *, *, *, *)(*)).thenReturn(Future.successful(blobReturnSuccess))
      when(mockTicketService.updateMessageAttachmentsIfRequired(*, *)(*)).thenReturn(Future.successful(DeskproTicketMessageSuccess))

      val result = await(underTest.handleCallback(callbackReady))

      result shouldBe DeskproTicketMessageSuccess
      verify(mockUploadedFileRepository).createOrUpdate(eqTo(uploadedPending2))
      verify(mockUploadedFileRepository).createOrUpdate(eqTo(uploadedSuccess))
      verify(mockUpscanDownloadConnector).stream(eqTo(url))(*)
      verify(mockDeskproConnector).createBlob(eqTo("filename"), eqTo("text/plain"), eqTo(1000), eqTo(fileReference.value), eqTo(stream))(*)
      verify(mockTicketService).updateMessageAttachmentsIfRequired(eqTo(fileReference.value), eqTo(Some(BlobDetails(1234, "auth"))))(*)
    }

    "add a failed to upload to deskpro to the uploaded files repository if create blob fails" in new Setup {

      when(mockUploadedFileRepository.fetchByFileReference(*)).thenReturn(Future.successful(None))
      when(mockUploadedFileRepository.createOrUpdate(*)).thenReturn(Future.successful(uploadedSuccess))
      when(mockUpscanDownloadConnector.stream(*)(*)).thenReturn(Future.successful(stream))
      when(mockDeskproConnector.createBlob(*, *, *, *, *)(*)).thenReturn(Future.successful(DeskproBlobCreationFailure("error message")))
      when(mockTicketService.updateMessageAttachmentsIfRequired(*, *)(*)).thenReturn(Future.successful(DeskproTicketMessageSuccess))

      val result = await(underTest.handleCallback(callbackReady))

      result shouldBe DeskproTicketMessageSuccess
      verify(mockUploadedFileRepository).createOrUpdate(eqTo(uploadedPending))
      verify(mockUploadedFileRepository).createOrUpdate(eqTo(deskproUploadedFailed))
      verify(mockUpscanDownloadConnector).stream(eqTo(url))(*)
      verify(mockDeskproConnector).createBlob(eqTo("filename"), eqTo("text/plain"), eqTo(1000), eqTo(fileReference.value), eqTo(stream))(*)
      verify(mockTicketService).updateMessageAttachmentsIfRequired(eqTo(fileReference.value), eqTo(None))(*)
    }

    "successfully add a failed callback to the uploaded files repository" in new Setup {

      when(mockUploadedFileRepository.createOrUpdate(*)).thenReturn(Future.successful(uploadedFailed))
      when(mockTicketService.updateMessageAttachmentsIfRequired(*, *)(*)).thenReturn(Future.successful(DeskproTicketMessageSuccess))

      val result = await(underTest.handleCallback(callbackFailed))

      result shouldBe DeskproTicketMessageSuccess
      verify(mockUploadedFileRepository).createOrUpdate(eqTo(uploadedFailed))
      verify(mockTicketService).updateMessageAttachmentsIfRequired(eqTo(fileReference.value), eqTo(None))(*)
      verify(mockUpscanDownloadConnector, never).stream(*)(*)
      verify(mockDeskproConnector, never).createBlob(*, *, *, *, *)(*)
    }
  }
}
