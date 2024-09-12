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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{DeskproTicket, DeskproTicketMessage}
import uk.gov.hmrc.apiplatformdeskpro.stubs.{DeskproStub, InternalAuthStub}
import uk.gov.hmrc.apiplatformdeskpro.utils.{AsyncHmrcSpec, ConfigBuilder}
import uk.gov.hmrc.http.test.WireMockSupport

class CreateTicketControllerISpec extends AsyncHmrcSpec with WireMockSupport with ConfigBuilder with GuiceOneAppPerSuite {

  override def fakeApplication() = GuiceApplicationBuilder()
    .configure(stubConfig(wireMockPort))
    .build()

  trait Setup extends DeskproStub with InternalAuthStub {

    val token     = "123456"
    val underTest = app.injector.instanceOf[CreateTicketController]
  }

  "createTicket" should {
    "return 201 on the agreed route" in new Setup {
      Authenticate.returns(token)
      val createTicketRequest = CreateTicketRequest(
        DeskproPerson("Dave", "dave@example.com"),
        "subject",
        "message",
        None,
        None,
        None,
        None,
        None
      )

      val deskproTicket = DeskproTicket(DeskproPerson("Dave", "dave@example.com"), "subject", DeskproTicketMessage("message"), 1)
      CreateTicket.stubSuccess(deskproTicket)

      val fakeRequest = FakeRequest("POST", "/ticket").withJsonBody(Json.toJson(createTicketRequest)).withHeaders("Authorization" -> token)

      val result = route(app, fakeRequest).get

      status(result) mustBe CREATED
    }
  }
}
