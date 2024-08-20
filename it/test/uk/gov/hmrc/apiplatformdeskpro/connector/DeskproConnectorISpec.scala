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

import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{DeskproTicket, DeskproTicketMessage, _}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproTicketCreationFailed, _}
import uk.gov.hmrc.apiplatformdeskpro.stubs.DeskproStub
import uk.gov.hmrc.apiplatformdeskpro.utils.{AsyncHmrcSpec, ConfigBuilder, WireMockSupport}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, UserId}

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

  trait Setup extends DeskproStub {

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
    val deskproPerson = DeskproPerson(name, email)
    val deskproTicket = DeskproTicket(deskproPerson, subject, DeskproTicketMessage(message), brand, fields)

  }

  "deskproConnector" when {
    "createTicket" should {
      "return DeskproTicketCreated with reference returned in response body when 201 returned with valid response from deskpro" in new Setup {
        CreateTicket.stubSuccess(deskproTicket)

        val result: Either[DeskproTicketCreationFailed, DeskproTicketCreated] = await(objInTest.createTicket(deskproTicket))
        result shouldBe Right(DeskproTicketCreated("SDST-1234"))
      }

      "return DeskproTicketCreationFailed with 'missing authorisation' when 401 returned from deskpro" in new Setup {
        CreateTicket.stubUnauthorised()

        val error: Either[DeskproTicketCreationFailed, DeskproTicketCreated] = await(objInTest.createTicket(deskproTicket))

        error.left.getOrElse(fail("should not reach here")).message shouldBe "Failed to create deskpro ticket: Missing authorization"
      }

      "return DeskproTicketCreationFailed with 'Unknown reason' when 500 returned from deskpro" in new Setup {
        CreateTicket.stubInternalServerError()

        val error: Either[DeskproTicketCreationFailed, DeskproTicketCreated] = await(objInTest.createTicket(deskproTicket))

        error.left.getOrElse(fail("should not reach here")).message shouldBe "Failed to create deskpro ticket: Unknown reason"
      }
    }

    "createPerson" should {
      "return DeskproPersonCreationSuccess when 201 returned from deskpro" in new Setup {
        CreatePerson.stubSuccess(deskproPerson)

        val result: DeskproPersonCreationResult = await(objInTest.createPerson(UserId.random, deskproPerson.name, deskproPerson.email))
        result shouldBe DeskproPersonCreationSuccess
      }

      "return DeskproPersonCreationDuplicate returned in response body when 400 with dupe_email" in new Setup {
        CreatePerson.stubDupeEmailError()

        val result: DeskproPersonCreationResult = await(objInTest.createPerson(UserId.random, deskproPerson.name, deskproPerson.email))
        result shouldBe DeskproPersonCreationDuplicate
      }

      "return DeskproPersonCreationFailure returned in response body when 400" in new Setup {
        CreatePerson.stubBadRequest()

        val result: DeskproPersonCreationResult = await(objInTest.createPerson(UserId.random, deskproPerson.name, deskproPerson.email))
        result shouldBe DeskproPersonCreationFailure
      }

      "return DeskproPersonCreationFailure when 500 returned from deskpro" in new Setup {
        CreatePerson.stubInternalServerError()

        val error: DeskproPersonCreationResult = await(objInTest.createPerson(UserId.random, deskproPerson.name, deskproPerson.email))
        error shouldBe DeskproPersonCreationFailure
      }

    }

    "getOrganisation" should {
      "return DeskproPersonCreationSuccess when 200 returned from deskpro" in new Setup {
        val orgId = OrganisationId("1")
        GetOrganisation.stubSuccess(orgId)
        val result = await(objInTest.getOrganisationById(orgId))
        result.linked.person.size shouldBe 6
        result.linked.organization.size shouldBe 1
      }}
  }
}
