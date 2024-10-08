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

package uk.gov.hmrc.apiplatformdeskpro.connector

import java.time._
import java.time.format.DateTimeFormatter

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.RegisteredUser
import uk.gov.hmrc.apiplatformdeskpro.utils.{AsyncHmrcSpec, ConfigBuilder, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class DeveloperConnectorISpec extends AsyncHmrcSpec
    with WireMockSupport
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with FixedClock {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig(wireMockPort))
      .in(Mode.Test)
      .overrides(bind[Clock].toInstance(FixedClock.clock))
      .build()

  trait Setup {

    val objInTest: DeveloperConnector = app.injector.instanceOf[DeveloperConnector]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    def stubGetDevelopersSuccess() = {
      stubFor(
        get(urlPathEqualTo("/developers"))
          .withQueryParam("status", equalTo("VERIFIED"))
          .willReturn(
            aResponse()
              .withBody("""[{"email":"bob.hope@business.com","userId":"6f0c4afa-1e10-44a1-8538-f5d436e7b615", "firstName":"Bob", "lastName":"Hope"},{"email":"emu.hull@business.com","userId":"a86857b3-732a-41c7-8d37-d29d5f416404", "firstName":"Emu", "lastName":"Hull"}]""")
              .withStatus(OK)
          )
      )
    }

    def stubGetDevelopersWithDaysToLookBackSuccess(days: Int) = {
      stubFor(
        get(urlPathEqualTo("/developers"))
          .withQueryParam("status", equalTo("VERIFIED"))
          .withQueryParam("limit", equalTo("10000"))
          .withQueryParam("createdAfter", equalTo(DateTimeFormatter.BASIC_ISO_DATE.format(now.minusDays(days))))
          .willReturn(
            aResponse()
              .withBody("""[{"email":"bob.hope@business.com","userId":"6f0c4afa-1e10-44a1-8538-f5d436e7b615", "firstName":"Bob", "lastName":"Hope"},{"email":"emu.hull@business.com","userId":"a86857b3-732a-41c7-8d37-d29d5f416404", "firstName":"Emu", "lastName":"Hull"}]""")
              .withStatus(OK)
          )
      )
    }

    def stubGetDevelopersEmptyList() = {
      stubFor(
        get(urlPathEqualTo("/developers"))
          .withQueryParam("status", equalTo("VERIFIED"))
          .willReturn(
            aResponse()
              .withBody("""[]""")
              .withStatus(OK)
          )
      )
    }

    def stubGetDevelopersInternalServerError() = {
      stubFor(
        get(urlPathEqualTo("/developers"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
    }
  }

  "DeveloperConnector" should {
    "return list of registered users from TPD" in new Setup {
      stubGetDevelopersSuccess()

      val result = await(objInTest.searchDevelopers())
      result shouldBe List(
        RegisteredUser(LaxEmailAddress("bob.hope@business.com"), UserId.unsafeApply("6f0c4afa-1e10-44a1-8538-f5d436e7b615"), "Bob", "Hope"),
        RegisteredUser(LaxEmailAddress("emu.hull@business.com"), UserId.unsafeApply("a86857b3-732a-41c7-8d37-d29d5f416404"), "Emu", "Hull")
      )
    }

    "return list of registered users from TPD when daysToLookBack is specified" in new Setup {
      stubGetDevelopersWithDaysToLookBackSuccess(5)

      val result = await(objInTest.searchDevelopers(Some(5)))
      result shouldBe List(
        RegisteredUser(LaxEmailAddress("bob.hope@business.com"), UserId.unsafeApply("6f0c4afa-1e10-44a1-8538-f5d436e7b615"), "Bob", "Hope"),
        RegisteredUser(LaxEmailAddress("emu.hull@business.com"), UserId.unsafeApply("a86857b3-732a-41c7-8d37-d29d5f416404"), "Emu", "Hull")
      )
    }

    "return empty List and not error when no developers returned from TPD" in new Setup {
      stubGetDevelopersEmptyList()

      val result = await(objInTest.searchDevelopers())
      result shouldBe Nil
    }

    "throw error if TPD throws internal server error" in new Setup {
      stubGetDevelopersInternalServerError()

      intercept[UpstreamErrorResponse] {
        await(objInTest.searchDevelopers())
      }
    }
  }
}
