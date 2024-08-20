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
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, StubControllerComponentsFactory, StubPlayBodyParsersFactory}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproPerson, OrganisationId, _}
import uk.gov.hmrc.apiplatformdeskpro.service.OrganisationService
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

class OrganisationControllerSpec extends AsyncHmrcSpec with StubControllerComponentsFactory with StubPlayBodyParsersFactory {

  trait Setup {
    implicit val hc: HeaderCarrier        = HeaderCarrier()
    implicit val cc: ControllerComponents = Helpers.stubControllerComponents()

    val mockService = mock[OrganisationService]

    val objToTest = new OrganisationController(mockService, cc)

    val orgName                        = "Test Orgname"
    val personName                     = "Bob Emu"
    val personEmail                    = "email@address.com"
    val organisationId: OrganisationId = OrganisationId("2")

    val response = DeskproOrganisation(
      organisationId = organisationId,
      organisationName = orgName,
      persons = List(DeskproPerson(personName, personEmail))
    )

  }

  "OrganisationController" should {
    "return 200 with organisation when data returned from service" in new Setup {

      when(mockService.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.successful(response))

      val request = FakeRequest(GET, s"/organisation/$organisationId")

      val result: Future[Result] = objToTest.getOrganisation(organisationId)(request)

      status(result) shouldBe OK
      contentAsJson(result).as[DeskproOrganisation] shouldBe response

      verify(mockService).getOrganisationById(eqTo(organisationId))(*)
    }
    "return 404 with organisation when no Data" in new Setup {

      when(mockService.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.failed(UpstreamErrorResponse("Not found", 404)))

      val request = FakeRequest(GET, s"/organisation/$organisationId")

      val result: Future[Result] = objToTest.getOrganisation(organisationId)(request)

      status(result) shouldBe NOT_FOUND
      contentAsString(result) shouldBe "{\"code\":\"ORGANISATION_NOT_FOUND\",\"message\":\"Not found\"}"
    }

    "return 500 with organisation when non 404" in new Setup {

      when(mockService.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.failed(UpstreamErrorResponse("Unauthorized", 401)))

      val request = FakeRequest(GET, s"/organisation/$organisationId")

      val result: Future[Result] = objToTest.getOrganisation(organisationId)(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) shouldBe "{\"code\":\"UNKNOWN_ERROR\",\"message\":\"Unknown error occurred\"}"

    }
  }
}
