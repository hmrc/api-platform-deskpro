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

import play.api.http.Status.CREATED
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsText, ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.DeskproTicketCreated
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{CreateTicketRequest, CreateTicketResponse, DeskproPerson, DeskproTicketCreationFailed}
import uk.gov.hmrc.apiplatformdeskpro.service.CreateTicketService
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId

class CreateTicketControllerSpec extends AsyncHmrcSpec with StubControllerComponentsFactory with StubPlayBodyParsersFactory {

  trait Setup {
    implicit val hc: HeaderCarrier        = HeaderCarrier()
    implicit val cc: ControllerComponents = Helpers.stubControllerComponents()

    val mockService = mock[CreateTicketService]

    val objToTest = new CreateTicketController(mockService, cc)

    val name                   = "Bob Holness"
    val email                  = "bob@exmaple.com"
    val subject                = "Subject of the ticket"
    val message                = "This is where the message for the ticket goes"
    val apiName                = "apiName"
    val applicationId          = ApplicationId.random.toString()
    val organisation           = "organisation"
    val supportReason          = "supportReason"
    val teamMemberEmailAddress = "frank@example.com"

    val createTicketRequest = CreateTicketRequest(
      DeskproPerson(name, email),
      subject,
      message,
      Some(apiName),
      Some(applicationId),
      Some(organisation),
      Some(supportReason),
      Some(teamMemberEmailAddress)
    )

    def stubServiceSuccess(ref: String) = {
      when(mockService.submitTicket(*)(*)).thenReturn(Future.successful(Right(DeskproTicketCreated(ref))))
    }
  }

  "CreateDeskproTicketController" should {
    "return 201 with a valid payload" in new Setup {
      val ref     = "123456"
      stubServiceSuccess(ref)
      val body    = Json.toJson(createTicketRequest)
      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json")
        .withJsonBody(body)

      val result: Future[Result] = objToTest.createTicket()(request)

      status(result) shouldBe CREATED
      contentAsJson(result).as[CreateTicketResponse] shouldBe CreateTicketResponse(ref)
    }

    "return 400 for an invalid payload" in new Setup {
      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json")
        .withJsonBody(Json.parse("""{ "invalidfield": "value" }"""))

      val result: Future[Result] = objToTest.createTicket()(request)

      status(result) shouldBe BAD_REQUEST
    }

    "return 400 for non Json" in new Setup {
      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json")
        .withBody(AnyContentAsText("""Not JSON"""))

      val result: Future[Result] = objToTest.createTicket()(request)

      status(result) shouldBe BAD_REQUEST
    }

    "return 500 when the call to deskpro fails" in new Setup {
      when(mockService.submitTicket(*)(*)).thenReturn(Future.successful(Left(DeskproTicketCreationFailed("failed"))))
      val body    = Json.toJson(createTicketRequest)
      val request = FakeRequest(POST, "/ticket")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json")
        .withJsonBody(body)

      val result: Future[Result] = objToTest.createTicket()(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
