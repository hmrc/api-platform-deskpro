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

import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import uk.gov.hmrc.apiplatformdeskpro.domain.models._
import uk.gov.hmrc.apiplatformdeskpro.service.PersonService
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

class PersonControllerSpec extends AsyncHmrcSpec with StubControllerComponentsFactory with StubPlayBodyParsersFactory {

  trait Setup {
    implicit val hc: HeaderCarrier        = HeaderCarrier()
    implicit val cc: ControllerComponents = Helpers.stubControllerComponents()

    val mockService       = mock[PersonService]
    val mockStubBehaviour = mock[StubBehaviour]

    val objToTest = new PersonController(mockService, cc, BackendAuthComponentsStub(mockStubBehaviour))

    val expectedPredicate = Permission(Resource(ResourceType("api-platform-deskpro"), ResourceLocation("people/all")), IAAction("WRITE"))

    val personId1: Int = 1
    val personName     = "Bob Emu"
    val personEmail    = LaxEmailAddress("email@address.com")
  }

  "PersonController" when {
    "updatePersonByEmail" should {
      "return 200 when person found and updated successfully" in new Setup {
        when(mockService.updatePersonByEmail(*[LaxEmailAddress], *)(*)).thenReturn(Future.successful(DeskproPersonUpdateSuccess))
        when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

        val request = FakeRequest(PUT, "/person")
          .withJsonBody(Json.parse(s"""{"email": "${personEmail.text}", "name": "$personName"}"""))
          .withHeaders("Authorization" -> "123456")

        val result: Future[Result] = objToTest.updatePersonByEmail()(request)

        status(result) shouldBe OK

        verify(mockService).updatePersonByEmail(eqTo(personEmail), eqTo(personName))(*)
      }

      "return 500 when person found but update failed" in new Setup {
        when(mockService.updatePersonByEmail(*[LaxEmailAddress], *)(*)).thenReturn(Future.successful(DeskproPersonUpdateFailure))
        when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

        val request = FakeRequest(PUT, "/person")
          .withJsonBody(Json.parse(s"""{"email": "${personEmail.text}", "name": "$personName"}"""))
          .withHeaders("Authorization" -> "123456")

        val result: Future[Result] = objToTest.updatePersonByEmail()(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "return 500 when downstream call returns 401" in new Setup {
        when(mockService.updatePersonByEmail(*[LaxEmailAddress], *)(*)).thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", 401)))
        when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

        val request = FakeRequest(PUT, "/person")
          .withJsonBody(Json.parse(s"""{"email": "${personEmail.text}", "name": "$personName"}"""))
          .withHeaders("Authorization" -> "123456")

        val result: Future[Result] = objToTest.updatePersonByEmail()(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsString(result) shouldBe """{"code":"UNKNOWN_ERROR","message":"Unknown error occurred"}"""
      }

      "return 404 when no person found for specified email" in new Setup {
        when(mockService.updatePersonByEmail(*[LaxEmailAddress], *)(*)).thenReturn(Future.failed(DeskproPersonNotFound("Person not found")))
        when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

        val request = FakeRequest(PUT, "/person")
          .withJsonBody(Json.parse(s"""{"email": "${personEmail.text}", "name": "$personName"}"""))
          .withHeaders("Authorization" -> "123456")

        val result: Future[Result] = objToTest.updatePersonByEmail()(request)

        status(result) shouldBe NOT_FOUND
        contentAsString(result) shouldBe """{"code":"PERSON_NOT_FOUND","message":"Person not found"}"""
      }

      "return UpstreamErrorResponse with an invalid token" in new Setup {
        when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

        val request = FakeRequest(PUT, "/person")
          .withJsonBody(Json.parse(s"""{"email": "${personEmail.text}", "name": "$personName"}"""))
          .withHeaders("Authorization" -> "123456")

        intercept[UpstreamErrorResponse] {
          await(objToTest.updatePersonByEmail()(request))
        }

        verify(mockService, never).updatePersonByEmail(eqTo(personEmail), eqTo(personName))(*)
      }
    }
  }
}
