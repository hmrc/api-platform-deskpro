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
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.WsTestClient
import uk.gov.hmrc.apiplatformdeskpro.domain.models._
import uk.gov.hmrc.apiplatformdeskpro.stubs.{DeskproStub, InternalAuthStub}
import uk.gov.hmrc.apiplatformdeskpro.utils.{AsyncHmrcSpec, ConfigBuilder}
import uk.gov.hmrc.http.test.WireMockSupport

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

class OrganisationControllerISpec extends AsyncHmrcSpec with WireMockSupport with ConfigBuilder with GuiceOneServerPerSuite with WsTestClient {

  override def fakeApplication() = GuiceApplicationBuilder()
    .configure(stubConfig(wireMockPort))
    .build()

  trait Setup extends DeskproStub with InternalAuthStub {

    val token            = "123456"
    val underTest        = app.injector.instanceOf[OrganisationController]
    implicit val appPort = port
  }

  "getOrganisation" should {
    "return 200 on the agreed route" in new Setup {
      Authenticate.returns(token)
      val orgId: OrganisationId = OrganisationId("1")
      GetOrganisationById.stubSuccess(orgId)
      GetPeopleByOrganisationId.stubSuccess(orgId)

      val response = await(wsUrl(s"/organisation/$orgId").addHttpHeaders("Authorization" -> token).get())

      response.status mustBe OK

      Json.parse(response.body).as[DeskproOrganisation] mustBe
        DeskproOrganisation(
          organisationId = OrganisationId("1"),
          organisationName = "Example Accounting",
          people = List(DeskproPerson("Bob Emu", "bob@example.com"))
        )
    }

    "return 404 if not found" in new Setup {
      Authenticate.returns(token)
      val orgId: OrganisationId = OrganisationId("1")
      GetOrganisationById.stubFailure(orgId)

      await(wsUrl(s"/organisation/$orgId").addHttpHeaders("Authorization" -> token).get()).status mustBe NOT_FOUND
    }
  }

  "getOrganisationsByPersonEmail" should {
    "return 200 on the agreed route" in new Setup {
      Authenticate.returns(token)
      val personEmail = LaxEmailAddress("bob@example.com")

      GetOrganisationsByEmail.stubSuccess(personEmail)

      val response = await(wsUrl(s"/organisation/query")
        .addHttpHeaders("Authorization" -> token)
        .post(Json.parse(s"""{"email": "${personEmail.text}"}""")))

      response.status mustBe OK
      Json.parse(response.body).as[List[DeskproOrganisation]] mustBe
        List(
          DeskproOrganisation(
            organisationId = OrganisationId("1"),
            organisationName = "Saga Accounting dkfjgh",
            people = List.empty
          ),
          DeskproOrganisation(
            organisationId = OrganisationId("3"),
            organisationName = "Deans Demo Org",
            people = List.empty
          )
        )
    }
  }
}
