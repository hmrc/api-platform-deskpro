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

package uk.gov.hmrc.apiplatformdeskpro.controller.test

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import uk.gov.hmrc.apiplatformdeskpro.domain.models._
import uk.gov.hmrc.apiplatformdeskpro.service.TicketService
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class DeleteTicketControllerSpec extends AsyncHmrcSpec with StubControllerComponentsFactory with StubPlayBodyParsersFactory with FixedClock {

  trait Setup {
    implicit val hc: HeaderCarrier        = HeaderCarrier()
    implicit val cc: ControllerComponents = Helpers.stubControllerComponents()

    val mockService       = mock[TicketService]
    val mockStubBehaviour = mock[StubBehaviour]

    val objToTest = new DeleteTicketController(mockService, cc, BackendAuthComponentsStub(mockStubBehaviour))

    val ticketId: Int = 123
  }

  "deleteTicket" should {
    val expectedPredicate = Permission(Resource(ResourceType("api-platform-deskpro"), ResourceLocation("tickets/all")), IAAction("WRITE"))
    "return 200 when ticket deleted successfully" in new Setup {

      when(mockService.deleteTicket(*)(*)).thenReturn(Future.successful(DeskproTicketUpdateSuccess))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(DELETE, s"/test-only/ticket/$ticketId")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")

      val result: Future[Result] = objToTest.deleteTicket(ticketId)(request)

      status(result) shouldBe OK
    }

    "return 404 when ticket not found" in new Setup {

      when(mockService.deleteTicket(*)(*)).thenReturn(Future.successful(DeskproTicketUpdateNotFound))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(DELETE, s"/test-only/ticket/$ticketId")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")

      val result: Future[Result] = objToTest.deleteTicket(ticketId)(request)

      status(result) shouldBe NOT_FOUND
    }

    "return 500 when failed" in new Setup {

      when(mockService.deleteTicket(*)(*)).thenReturn(Future.successful(DeskproTicketUpdateFailure))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(DELETE, s"/test-only/ticket/$ticketId")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")

      val result: Future[Result] = objToTest.deleteTicket(ticketId)(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return UpstreamErrorResponse for invalid token" in new Setup {
      val request = FakeRequest(DELETE, s"/test-only/ticket/$ticketId")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")

      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

      intercept[UpstreamErrorResponse] {
        await(objToTest.deleteTicket(ticketId)(request))
      }
    }
  }
}
