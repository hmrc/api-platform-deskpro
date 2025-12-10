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

package uk.gov.hmrc.apiplatformdeskpro.controller

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.http.Status.{CREATED, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsText, ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{DeskproTicketCreated, DeskproTicketData}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproTicketCreatedDuplicate, DeskproTicketCreationError}
import uk.gov.hmrc.apiplatformdeskpro.service.TicketService
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId

class CreateTicketControllerSpec extends AsyncHmrcSpec with StubControllerComponentsFactory with StubPlayBodyParsersFactory {

  trait Setup {
    implicit val hc: HeaderCarrier        = HeaderCarrier()
    implicit val cc: ControllerComponents = Helpers.stubControllerComponents()

    val mockService       = mock[TicketService]
    val mockStubBehaviour = mock[StubBehaviour]

    val objToTest = new CreateTicketController(mockService, cc, BackendAuthComponentsStub(mockStubBehaviour))

    val expectedPredicate = Permission(Resource(ResourceType("api-platform-deskpro"), ResourceLocation("tickets/all")), IAAction("WRITE"))

    val fullName        = "Bob Holness"
    val email           = "bob@exmaple.com"
    val subject         = "Subject of the ticket"
    val message         = "This is where the message for the ticket goes"
    val apiName         = "apiName"
    val applicationId   = ApplicationId.random.toString()
    val organisation    = "organisation"
    val supportReason   = "supportReason"
    val teamMemberEmail = "frank@example.com"
    val ticketId        = 12345

    val createTicketRequestJson = Json.parse(
      s"""
          {
            "fullName": "$fullName",
            "email": "$email",
            "subject": "$subject",
            "message": "$message",
            "apiName": "$apiName",
            "applicationId": "$applicationId",
            "organisation": "$organisation",
            "supportReason": "$supportReason",
            "teamMemberEmail": "$teamMemberEmail",
            "attachments": []
          }
      """
    )

    def stubServiceSuccess(ref: String) = {
      when(mockService.submitTicket(*)(*)).thenReturn(Future.successful(Right(DeskproTicketCreated(DeskproTicketData(ticketId, ref)))))
    }
  }

  "CreateDeskproTicketController" should {
    "return 201 with a valid payload" in new Setup {

      val ref = "123456"
      stubServiceSuccess(ref)
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(createTicketRequestJson)

      val result: Future[Result] = objToTest.createTicket()(request)

      status(result) shouldBe CREATED
      contentAsJson(result) shouldBe Json.parse(s"""{"ref": "$ref"}""")
    }

    "return 400 for an invalid payload" in new Setup {
      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(Json.parse("""{ "invalidfield": "value" }"""))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val result: Future[Result] = objToTest.createTicket()(request)

      status(result) shouldBe BAD_REQUEST
    }

    "return 400 for non Json" in new Setup {
      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withBody(AnyContentAsText("""Not JSON"""))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val result: Future[Result] = objToTest.createTicket()(request)

      status(result) shouldBe BAD_REQUEST
    }

    "return 500 when the call to deskpro fails" in new Setup {
      when(mockService.submitTicket(*)(*)).thenReturn(Future.successful(Left(DeskproTicketCreationError("failed"))))
      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(createTicketRequestJson)
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val result: Future[Result] = objToTest.createTicket()(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return 201 with an empty json when the call to deskpro fails due to duplicate" in new Setup {
      when(mockService.submitTicket(*)(*)).thenReturn(Future.successful(Left(DeskproTicketCreatedDuplicate())))
      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(createTicketRequestJson)
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val result: Future[Result] = objToTest.createTicket()(request)

      status(result) shouldBe CREATED
      contentAsString(result) shouldBe "{}"
    }

    "return UpstreamErrorResponse for invalid token" in new Setup {
      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withBody(AnyContentAsText("""Not JSON"""))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

      intercept[UpstreamErrorResponse] {
        await(objToTest.createTicket()(request))
      }
    }
  }
}
