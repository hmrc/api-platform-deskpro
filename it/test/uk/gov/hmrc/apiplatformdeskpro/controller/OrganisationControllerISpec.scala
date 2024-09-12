/*
 * Copyright 2023 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.apiplatformdeskpro.domain.models._
import uk.gov.hmrc.apiplatformdeskpro.stubs.{DeskproStub, InternalAuthStub}
import uk.gov.hmrc.apiplatformdeskpro.utils.{AsyncHmrcSpec, ConfigBuilder}
import uk.gov.hmrc.http.test.WireMockSupport

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

class OrganisationControllerISpec extends AsyncHmrcSpec with WireMockSupport with ConfigBuilder with GuiceOneAppPerSuite {

  override def fakeApplication() = GuiceApplicationBuilder()
    .configure(stubConfig(wireMockPort))
    .build()

  trait Setup extends DeskproStub with InternalAuthStub {

    val token     = "123456"
    val underTest = app.injector.instanceOf[OrganisationController]
  }

  "getOrganisation" should {
    "return 200 on the agreed route" in new Setup {
      Authenticate.returns(token)
      val orgId: OrganisationId = OrganisationId("1")
      GetOrganisationById.stubSuccess(orgId)
      GetPeopleByOrganisationId.stubSuccess(orgId)

      val fakeRequest = FakeRequest("GET", s"/organisation/$orgId").withHeaders("Authorization" -> token)

      val result = route(app, fakeRequest).get

      status(result) mustBe OK
    }

    "return 404 if not found" in new Setup {
      Authenticate.returns(token)
      val orgId: OrganisationId = OrganisationId("1")
      GetOrganisationById.stubFailure(orgId)

      val fakeRequest = FakeRequest("GET", s"/organisation/$orgId").withHeaders("Authorization" -> token)

      val result = route(app, fakeRequest).get

      status(result) mustBe NOT_FOUND
    }
  }

  "getOrganisationsByPersonEmail" should {
    "return 200 on the agreed route" in new Setup {
      Authenticate.returns(token)
      val personEmail = LaxEmailAddress("bob@example.com")

      GetOrganisationsByEmail.stubSuccess(personEmail)

      val fakeRequest = FakeRequest("POST", "/organisation/query").withJsonBody(Json.parse(s"""{"email": "${personEmail.text}"}""")).withHeaders("Authorization" -> token)

      val result = route(app, fakeRequest).get

      status(result) mustBe OK
    }
  }
}
