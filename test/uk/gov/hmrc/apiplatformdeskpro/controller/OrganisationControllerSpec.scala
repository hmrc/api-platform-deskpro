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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproPerson, OrganisationId, _}
import uk.gov.hmrc.apiplatformdeskpro.service.OrganisationService
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.internalauth.client.Predicate.Permission
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.internalauth.client.test.{BackendAuthComponentsStub, StubBehaviour}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

class OrganisationControllerSpec extends AsyncHmrcSpec with StubControllerComponentsFactory with StubPlayBodyParsersFactory {

  trait Setup {
    implicit val hc: HeaderCarrier        = HeaderCarrier()
    implicit val cc: ControllerComponents = Helpers.stubControllerComponents()

    val mockService       = mock[OrganisationService]
    val mockStubBehaviour = mock[StubBehaviour]

    val objToTest = new OrganisationController(mockService, cc, BackendAuthComponentsStub(mockStubBehaviour))

    val expectedPredicate = Permission(Resource(ResourceType("api-platform-deskpro"), ResourceLocation("organisations/all")), IAAction("READ"))

    val orgId1: OrganisationId = OrganisationId("1")
    val orgName1               = "Test Orgname 1"

    val orgId2: OrganisationId = OrganisationId("2")
    val orgName2               = "Test Orgname 2"

    val personName  = "Bob Emu"
    val personEmail = "email@address.com"

    val response = DeskproOrganisation(
      organisationId = orgId1,
      organisationName = orgName1,
      people = List(DeskproPerson(personName, personEmail))
    )

  }

  "OrganisationController" should {
    "return 200 with organisation when data returned from service" in new Setup {
      when(mockService.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.successful(response))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(GET, s"/organisation/$orgId1").withHeaders("Authorization" -> "123456")

      val result: Future[Result] = objToTest.getOrganisation(orgId1)(request)

      status(result) shouldBe OK
      contentAsJson(result).as[DeskproOrganisation] shouldBe response

      verify(mockService).getOrganisationById(eqTo(orgId1))(*)
    }

    "return 404 with organisation when no Data" in new Setup {
      when(mockService.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.failed(UpstreamErrorResponse("Not found", 404)))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(GET, s"/organisation/$orgId1").withHeaders("Authorization" -> "123456")

      val result: Future[Result] = objToTest.getOrganisation(orgId1)(request)

      status(result) shouldBe NOT_FOUND
      contentAsString(result) shouldBe "{\"code\":\"ORGANISATION_NOT_FOUND\",\"message\":\"Not found\"}"
    }

    "return 500 with organisation when non 404" in new Setup {
      when(mockService.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", 401)))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(GET, s"/organisation/$orgId1").withHeaders("Authorization" -> "123456")

      val result: Future[Result] = objToTest.getOrganisation(orgId1)(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) shouldBe """{"code":"UNKNOWN_ERROR","message":"Unknown error occurred"}"""
    }

    "return UpstreamErrorResponse with an invalid token" in new Setup {
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

      val request = FakeRequest(GET, s"/organisation/$orgId1").withHeaders("Authorization" -> "123456")

      intercept[UpstreamErrorResponse] {
        await(objToTest.getOrganisation(orgId1)(request))
      }
    }
  }

  "getOrganisationForPersonEmail" should {
    "return 200 with organisation when data returned from service" in new Setup {
      val org1 = DeskproOrganisation(orgId1, orgName1, List())
      val org2 = DeskproOrganisation(orgId2, orgName2, List())

      when(mockService.getOrganisationsByEmail(*[LaxEmailAddress])(*))
        .thenReturn(Future.successful(List(org1, org2)))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(POST, "/organisation/query")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(Json.parse(s"""{"email": "$personEmail"}"""))

      val result = objToTest.getOrganisationsByPersonEmail()(request)

      val expectedResponse = Json.parse(
        s"""
          [
            {
              "organisationId": "$orgId1",
              "organisationName": "$orgName1",
              "people": []
            },
            {
              "organisationId": "$orgId2",
              "organisationName": "$orgName2",
              "people": []
            }
          ]
          """
      )

      status(result) shouldBe OK
      contentAsJson(result) shouldBe expectedResponse

      verify(mockService).getOrganisationsByEmail(eqTo(LaxEmailAddress(personEmail)))(*)
    }

    "return empty array when no data returned from service" in new Setup {
      when(mockService.getOrganisationsByEmail(*[LaxEmailAddress])(*))
        .thenReturn(Future.successful(List.empty))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(POST, "/organisation/query")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(Json.parse(s"""{"email": "$personEmail"}"""))

      val result = objToTest.getOrganisationsByPersonEmail()(request)

      val expectedResponse = Json.parse("[]")

      status(result) shouldBe OK
      contentAsJson(result) shouldBe expectedResponse
    }

    "return 500 with when error returned from service" in new Setup {
      when(mockService.getOrganisationsByEmail(*[LaxEmailAddress])(*))
        .thenReturn(Future.failed(UpstreamErrorResponse("Error", 500)))
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.successful(Retrieval.Username("Bob")))

      val request = FakeRequest(POST, "/organisation/query")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(Json.parse(s"""{"email": "$personEmail"}"""))

      val result = objToTest.getOrganisationsByPersonEmail()(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) shouldBe """{"code":"UNKNOWN_ERROR","message":"Unknown error occurred"}"""
    }

    "return UpstreamErrorResponse with an invalid token" in new Setup {
      when(mockStubBehaviour.stubAuth(Some(expectedPredicate), Retrieval.EmptyRetrieval)).thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", UNAUTHORIZED)))

      val request = FakeRequest(POST, "/organisation/query")
        .withHeaders("Content-Type" -> "application/json", "Accept" -> "application/vnd.hmrc.1.0+json", "Authorization" -> "123456")
        .withJsonBody(Json.parse(s"""{"email": "$personEmail"}"""))

      intercept[UpstreamErrorResponse] {
        await(objToTest.getOrganisationsByPersonEmail()(request))
      }
    }
  }
}
