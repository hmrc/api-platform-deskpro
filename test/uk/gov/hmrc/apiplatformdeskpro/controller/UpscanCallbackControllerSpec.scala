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

package uk.gov.hmrc.apiplatformdeskpro.controller

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsText, ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{BlobDetails, UploadStatus, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.service.UpscanCallbackDispatcher
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class UpscanCallbackControllerSpec extends AsyncHmrcSpec with StubControllerComponentsFactory with StubPlayBodyParsersFactory with FixedClock {

  trait Setup {
    implicit val hc: HeaderCarrier        = HeaderCarrier()
    implicit val cc: ControllerComponents = Helpers.stubControllerComponents()

    val mockService       = mock[UpscanCallbackDispatcher]
    val mockStubBehaviour = mock[StubBehaviour]

    val objToTest = new UpscanCallbackController(mockService, cc, BackendAuthComponentsStub(mockStubBehaviour))

    val fileReference = "507e60b3-0ee1-411f-9c6e-7261455056c3"
    val url           = new URL("https://example.com/file1")
    val uploadStatus  = UploadStatus.UploadedSuccessfully("filename.txt", "text/plain", url, 1000, BlobDetails(1234, "auth"))
    val uploadedFile  = UploadedFile(fileReference, uploadStatus, instant)

    val callbackReadyRequestJson = Json.parse(
      s"""
          {
            "fileStatus": "READY",
            "reference": "507e60b3-0ee1-411f-9c6e-7261455056c3",
            "downloadUrl": "https://example.com/file1",
            "uploadDetails": {
              "uploadTimestamp": "2025-10-15T14:20:40Z",
              "checksum": "checksum",
              "fileMimeType": "text/plain",
              "fileName": "filename.txt",
              "size": 1000
            }
          }
      """
    )

    val callbackFailedRequestJson = Json.parse(
      s"""
          {
            "fileStatus": "FAILED",
            "reference": "507e60b3-0ee1-411f-9c6e-7261455056c3",
            "failureDetails": {
              "failureReason": "failure reason",
              "message": "message"
            }
          }
      """
    )

    val callbackInvalidRequestJson = Json.parse(
      s"""
          {
            "fileStatus": "INVALID",
            "reference": "507e60b3-0ee1-411f-9c6e-7261455056c3"
          }
      """
    )
  }

  "callback" should {
    "return 200 with a ready callback" in new Setup {
      when(mockService.handleCallback(*)(*)).thenReturn(Future.successful(uploadedFile))

      val request = FakeRequest(POST, "/upscan-callback")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json")
        .withJsonBody(callbackReadyRequestJson)

      val result: Future[Result] = objToTest.callback()(request)

      status(result) shouldBe OK
    }

    "return 200 with a failure callback" in new Setup {
      when(mockService.handleCallback(*)(*)).thenReturn(Future.successful(uploadedFile))

      val request = FakeRequest(POST, "/upscan-callback")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json")
        .withJsonBody(callbackFailedRequestJson)

      val result: Future[Result] = objToTest.callback()(request)

      status(result) shouldBe OK
    }

    "return 400 with an invalid failure callback" in new Setup {
      val request = FakeRequest(POST, "/upscan-callback")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json")
        .withJsonBody(callbackInvalidRequestJson)

      val result: Future[Result] = objToTest.callback()(request)

      status(result) shouldBe BAD_REQUEST
    }

    "return 400 for an invalid payload" in new Setup {
      val request = FakeRequest(POST, "/upscan-callback")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json")
        .withJsonBody(Json.parse("""{ "invalidfield": "value" }"""))

      val result: Future[Result] = objToTest.callback()(request)

      status(result) shouldBe BAD_REQUEST
    }

    "return 400 for non Json" in new Setup {
      val request = FakeRequest(POST, "/upscan-callback")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json")
        .withBody(AnyContentAsText("""Not JSON"""))

      val result: Future[Result] = objToTest.callback()(request)

      status(result) shouldBe BAD_REQUEST
    }
  }
}
