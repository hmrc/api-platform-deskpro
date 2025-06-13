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

import play.api.http.Status.UNAUTHORIZED
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsText, ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import uk.gov.hmrc.apiplatformdeskpro.domain.models._
import uk.gov.hmrc.apiplatformdeskpro.service.TicketService
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class TicketControllerSpec extends AsyncHmrcSpec with StubControllerComponentsFactory with StubPlayBodyParsersFactory with FixedClock {

  trait Setup {
    implicit val hc: HeaderCarrier        = HeaderCarrier()
    implicit val cc: ControllerComponents = Helpers.stubControllerComponents()

    val mockService       = mock[TicketService]
    val mockStubBehaviour = mock[StubBehaviour]

    val objToTest = new TicketController(mockService, cc, BackendAuthComponentsStub(mockStubBehaviour))

    val expectedPredicate = Permission(Resource(ResourceType("api-platform-deskpro"), ResourceLocation("tickets/all")), IAAction("READ"))

    val email         = "bob@example.com"
    val personId: Int = 16
    val subject       = "Subject of the ticket"

    val getTicketsByEmailRequestJson = Json.parse(
      s"""
          {
            "email": "$email"
          }
      """
    )

    val ticketId: Int = 123
    val message       = DeskproMessage(789, ticketId, personId, instant, false, "message")
    val ticket        = DeskproTicket(ticketId, "ref1", personId, LaxEmailAddress("bob@example.com"), "awaiting_user", instant, Some(instant), "subject 1", List(message))

    val listOfTickets = List(
      ticket,
      DeskproTicket(456, "ref2", personId, LaxEmailAddress("bob@example.com"), "awaiting_agent", instant, None, "subject 2", List.empty)
    )
  }

  "getTicketsForPerson" should {
    "return 200 with a list of tickets" in new Setup {

      when(mockService.getTicketsForPerson(*[LaxEmailAddress], *)(*)).thenReturn(Future.successful(listOfTickets))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(POST, "/ticket/query")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(getTicketsByEmailRequestJson)

      val result: Future[Result] = objToTest.getTicketsByPersonEmail()(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(listOfTickets)
    }

    "return 400 for an invalid payload" in new Setup {
      val request = FakeRequest(POST, "/ticket/query")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(Json.parse("""{ "invalidfield": "value" }"""))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val result: Future[Result] = objToTest.getTicketsByPersonEmail()(request)

      status(result) shouldBe BAD_REQUEST
    }

    "return 400 for non Json" in new Setup {
      val request = FakeRequest(POST, "/ticket/query")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withBody(AnyContentAsText("""Not JSON"""))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val result: Future[Result] = objToTest.getTicketsByPersonEmail()(request)

      status(result) shouldBe BAD_REQUEST
    }

    "return 500 when the call to deskpro fails" in new Setup {

      when(mockService.getTicketsForPerson(*[LaxEmailAddress], *)(*)).thenReturn(Future.failed(DeskproPersonNotFound("Person not found")))

      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(getTicketsByEmailRequestJson)
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val result: Future[Result] = objToTest.getTicketsByPersonEmail()(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return UpstreamErrorResponse for invalid token" in new Setup {
      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(getTicketsByEmailRequestJson)
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

      intercept[UpstreamErrorResponse] {
        await(objToTest.getTicketsByPersonEmail()(request))
      }
    }
  }

  "fetchTicket" should {
    "return 200 with a ticket" in new Setup {

      when(mockService.batchFetchTicket(*)(*)).thenReturn(Future.successful(Some(ticket)))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(GET, s"/ticket/$ticketId")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")

      val result: Future[Result] = objToTest.fetchTicket(ticketId)(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(ticket)
    }

    "return 404 when not found" in new Setup {

      when(mockService.batchFetchTicket(*)(*)).thenReturn(Future.successful(None))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(GET, s"/ticket/$ticketId")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")

      val result: Future[Result] = objToTest.fetchTicket(ticketId)(request)

      status(result) shouldBe NOT_FOUND
    }

    "return UpstreamErrorResponse for invalid token" in new Setup {
      val request = FakeRequest(GET, s"/ticket/$ticketId")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")

      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

      intercept[UpstreamErrorResponse] {
        await(objToTest.fetchTicket(ticketId)(request))
      }
    }
  }

  "closeTicket" should {
    "return 200 when ticket closed successfully" in new Setup {

      when(mockService.closeTicket(*)(*)).thenReturn(Future.successful(DeskproTicketCloseSuccess))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(POST, s"/ticket/$ticketId/close")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")

      val result: Future[Result] = objToTest.closeTicket(ticketId)(request)

      status(result) shouldBe OK
    }

    "return 404 when ticket to close not found" in new Setup {

      when(mockService.closeTicket(*)(*)).thenReturn(Future.successful(DeskproTicketCloseNotFound))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(POST, s"/ticket/$ticketId/close")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")

      val result: Future[Result] = objToTest.closeTicket(ticketId)(request)

      status(result) shouldBe NOT_FOUND
    }

    "return 500 when ticket close failed" in new Setup {

      when(mockService.closeTicket(*)(*)).thenReturn(Future.successful(DeskproTicketCloseFailure))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(POST, s"/ticket/$ticketId/close")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")

      val result: Future[Result] = objToTest.closeTicket(ticketId)(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return UpstreamErrorResponse for invalid token" in new Setup {
      val request = FakeRequest(POST, s"/ticket/$ticketId/close")
        .withHeaders("Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")

      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

      intercept[UpstreamErrorResponse] {
        await(objToTest.closeTicket(ticketId)(request))
      }
    }
  }
}
