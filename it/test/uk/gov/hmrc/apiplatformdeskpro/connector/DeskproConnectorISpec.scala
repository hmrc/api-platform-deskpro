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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.{Application, Mode}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.DeskproTicketCreationFailed
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{DeskproTicket, DeskproTicketMessage, DeskproTicketPerson, _}
import uk.gov.hmrc.apiplatformdeskpro.utils.{AsyncHmrcSpec, ConfigBuilder, WireMockSupport}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId

class DeskproConnectorISpec
    extends AsyncHmrcSpec
    with WireMockSupport
    with GuiceOneServerPerSuite
    with ConfigBuilder {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig(wireMockPort))
      .in(Mode.Test)
      .build()

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup {

    val objInTest: DeskproConnector = app.injector.instanceOf[DeskproConnector]

    val name                   = "Bob Holness"
    val email                  = "bob@exmaple.com"
    val subject                = "Subject of the ticket"
    val message                = "This is where the message for the ticket goes"
    val apiName                = "apiName"
    val applicationId          = ApplicationId.random.toString()
    val organisation           = "organisation"
    val supportReason          = "supportReason"
    val teamMemberEmailAddress = "frank@example.com"
    val brand                  = 1

    val fields        = Map("2" -> apiName, "3" -> applicationId, "4" -> organisation, "5" -> supportReason, "6" -> teamMemberEmailAddress)
    val deskproTicket = DeskproTicket(DeskproTicketPerson(name, email), subject, DeskproTicketMessage(message), brand, fields)

    def stubCreateTicketSuccess() = {
      stubFor(
        post(urlMatching("/api/v2/tickets"))
          .withRequestBody(equalTo(Json.toJson(deskproTicket).toString))
          .willReturn(
            aResponse()
              .withBody("""{"data":{"id":12345,"ref":"SDST-1234"}}""")
              .withStatus(CREATED)
          )
      )
    }

    def stubCreateTicketUnauthorised() = {
      stubFor(
        post(urlMatching("/api/v2/tickets"))
          .willReturn(
            aResponse()
              .withStatus(UNAUTHORIZED)
          )
      )
    }

    def stubCreateTicketInternalServerError() = {
      stubFor(
        post(urlMatching("/api/v2/tickets"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
    }
  }

  "deskproConnector" should {
    "return DeskproTicketCreated with reference returned in response body when 201 returned with valid response from deskpro" in new Setup {
      stubCreateTicketSuccess()

      val result: DeskproTicketCreated = await(objInTest.createTicket(deskproTicket))
      result shouldBe DeskproTicketCreated("SDST-1234")
    }

    "return DeskproTicketCreationFailed with 'missing authorisation' when 401 returned from deskpro" in new Setup {
      stubCreateTicketUnauthorised()

      val error: DeskproTicketCreationFailed = intercept[DeskproTicketCreationFailed] {
        await(objInTest.createTicket(deskproTicket))
      }
      error.getMessage() shouldBe "Failed to create deskpro ticket: Missing authorization"
    }

    "return DeskproTicketCreationFailed with 'Unknown reason' when 500 returned from deskpro" in new Setup {
      stubCreateTicketInternalServerError()

      val error: DeskproTicketCreationFailed = intercept[DeskproTicketCreationFailed] {
        await(objInTest.createTicket(deskproTicket))
      }
      error.getMessage() shouldBe "Failed to create deskpro ticket: Unknown reason"
    }
  }
}
