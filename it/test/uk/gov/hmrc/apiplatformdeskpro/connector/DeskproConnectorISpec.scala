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

import java.time.Clock
import java.time.format.DateTimeFormatter

import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproTicketCreationFailed, _}
import uk.gov.hmrc.apiplatformdeskpro.stubs.DeskproStub
import uk.gov.hmrc.apiplatformdeskpro.utils.{AsyncHmrcSpec, ConfigBuilder, WireMockSupport}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class DeskproConnectorISpec
    extends AsyncHmrcSpec
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

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup extends DeskproStub {

    val objInTest: DeskproConnector = app.injector.instanceOf[DeskproConnector]

    val name            = "Bob Holness"
    val email           = "bob@exmaple.com"
    val subject         = "Subject of the ticket"
    val message         = "This is where the message for the ticket goes"
    val apiName         = "apiName"
    val applicationId   = ApplicationId.random.toString()
    val organisation    = "organisation"
    val supportReason   = "supportReason"
    val teamMemberEmail = "frank@example.com"
    val brand           = 1

    val fields: Map[String, String]                  = Map("2" -> apiName, "3" -> applicationId, "4" -> organisation, "5" -> supportReason, "6" -> teamMemberEmail)
    val deskproPerson: DeskproPerson                 = DeskproPerson(name, email)
    val deskproPersonUpdate: DeskproPersonUpdate     = DeskproPersonUpdate(name)
    val deskproInactivePerson: DeskproInactivePerson = DeskproInactivePerson(Map("5" -> "1", "4" -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(now)))
    val deskproTicket: DeskproTicket                 = DeskproTicket(deskproPerson, subject, DeskproTicketMessage(message), brand, fields)

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
        result shouldBe DeskproPersonExistsInDeskpro
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

    "updatePerson" should {
      "return DeskproPersonUpdateSuccess when 204 returned from deskpro" in new Setup {
        val personId: Int = 1
        UpdatePerson.stubSuccess(personId, deskproPersonUpdate)

        val result: DeskproPersonUpdateResult = await(objInTest.updatePerson(personId, deskproPerson.name))
        result shouldBe DeskproPersonUpdateSuccess
      }

      "return DeskproPersonUpdateFailure returned in response body when 400" in new Setup {
        val personId: Int = 1
        UpdatePerson.stubBadRequest(personId)

        val result: DeskproPersonUpdateResult = await(objInTest.updatePerson(personId, deskproPerson.name))
        result shouldBe DeskproPersonUpdateFailure
      }

      "return DeskproPersonUpdateFailure when 500 returned from deskpro" in new Setup {
        val personId: Int = 1
        UpdatePerson.stubInternalServerError(personId)

        val error: DeskproPersonUpdateResult = await(objInTest.updatePerson(personId, deskproPerson.name))
        error shouldBe DeskproPersonUpdateFailure
      }
    }

    "markPersonInactive" should {
      "return DeskproPersonUpdateSuccess when 204 returned from deskpro" in new Setup {
        val personId: Int = 1
        MarkPersonInactive.stubSuccess(personId, deskproInactivePerson)

        val result: DeskproPersonUpdateResult = await(objInTest.markPersonInactive(personId))
        result shouldBe DeskproPersonUpdateSuccess
      }

      "return DeskproPersonUpdateFailure returned in response body when 400" in new Setup {
        val personId: Int = 1
        MarkPersonInactive.stubBadRequest(personId)

        val result: DeskproPersonUpdateResult = await(objInTest.markPersonInactive(personId))
        result shouldBe DeskproPersonUpdateFailure
      }

      "return DeskproPersonUpdateFailure when 500 returned from deskpro" in new Setup {
        val personId: Int = 1
        MarkPersonInactive.stubInternalServerError(personId)

        val error: DeskproPersonUpdateResult = await(objInTest.markPersonInactive(personId))
        error shouldBe DeskproPersonUpdateFailure
      }
    }

    "getOrganisationWithPeopleById" should {
      val defaultMeta: DeskproMetaResponse = DeskproMetaResponse(DeskproPaginationResponse(1, 1))

      "return DeskproLinkedPersonWrapper when 200 returned from deskpro with response body" in new Setup {
        val orgId: OrganisationId         = OrganisationId("1")
        GetPeopleByOrganisationId.stubSuccess(orgId)
        val result: DeskproPeopleResponse = await(objInTest.getPeopleByOrganisationId(orgId))

        val expectedResponse = DeskproPeopleResponse(
          List(DeskproPersonResponse(3, None, "Jeff Smith"), DeskproPersonResponse(63, Some("bob@example.com"), "Bob Emu")),
          defaultMeta
        )
        result shouldBe expectedResponse
      }

      "return DeskproLinkedPersonWrapper when 200 returned from deskpro with response body when asking for 2nd page" in new Setup {
        val orgId: OrganisationId = OrganisationId("1")
        private val pageWanted    = 2
        GetPeopleByOrganisationId.stubSuccess(orgId, pageWanted)
        val result                = await(objInTest.getPeopleByOrganisationId(orgId, pageWanted))

        val expectedResponse = DeskproPeopleResponse(
          List(DeskproPersonResponse(3, None, "Jeff Smith"), DeskproPersonResponse(63, Some("bob@example.com"), "Bob Emu")),
          defaultMeta
        )
        result shouldBe expectedResponse
      }

      "return DeskproLinkedPersonWrapper when 200 returned from deskpro with response body without people in" in new Setup {
        val orgId: OrganisationId         = OrganisationId("1")
        GetPeopleByOrganisationId.stubSuccessNoPerson(orgId)
        val result: DeskproPeopleResponse = await(objInTest.getPeopleByOrganisationId(orgId))

        val expectedResponse: DeskproPeopleResponse = DeskproPeopleResponse(
          List.empty,
          defaultMeta
        )
        result shouldBe expectedResponse
      }

      "throw UpstreamErrorResponse when error response returned from deskpro" in new Setup {
        val orgId: OrganisationId = OrganisationId("1")
        GetPeopleByOrganisationId.stubFailure(orgId)
        intercept[UpstreamErrorResponse] {
          await(objInTest.getPeopleByOrganisationId(orgId))
        }
      }
    }
  }

  "getOrganisation" should {
    "return DeskproOrganisationWrapperResponse when 200 returned from deskpro with response body" in new Setup {
      val orgId: OrganisationId                      = OrganisationId("1")
      GetOrganisationById.stubSuccess(orgId)
      val result: DeskproOrganisationWrapperResponse = await(objInTest.getOrganisationById(orgId))

      val expectedResponse: DeskproOrganisationWrapperResponse = DeskproOrganisationWrapperResponse(
        DeskproOrganisationResponse(1, "Example Accounting")
      )
      result shouldBe expectedResponse
    }

    "throw UpstreamErrorResponse when error response returned from deskpro" in new Setup {
      val orgId: OrganisationId = OrganisationId("1")
      GetOrganisationById.stubFailure(orgId)
      intercept[UpstreamErrorResponse] {
        await(objInTest.getOrganisationById(orgId))
      }
    }
  }

  "getOrganisationForPersonEmail" should {
    "return DeskproLinkedOrganisationWrapper when 200 returned from deskpro with response body" in new Setup {
      val emailAddress = LaxEmailAddress("bob@example.com")
      GetOrganisationsByEmail.stubSuccess(emailAddress)

      val result = await(objInTest.getOrganisationsForPersonEmail(emailAddress))

      val expectedResponse = DeskproLinkedOrganisationWrapper(
        List(DeskproPersonResponse(63, Some(emailAddress.text), "Andy Spaven")),
        DeskproLinkedOrganisationObject(Map(
          "1" -> DeskproOrganisationResponse(1, "Saga Accounting dkfjgh"),
          "3" -> DeskproOrganisationResponse(3, "Deans Demo Org")
        ))
      )

      result shouldBe expectedResponse
    }

    "throw UpstreamErrorResponse when error response returned from deskpro" in new Setup {
      val emailAddress = LaxEmailAddress("bob@example.com")
      GetOrganisationsByEmail.stubFailure(emailAddress)

      intercept[UpstreamErrorResponse] {
        await(objInTest.getOrganisationsForPersonEmail(emailAddress))
      }
    }
  }

  "getPersonForEmail" should {
    "return DeskproLinkedOrganisationWrapper when 200 returned from deskpro with response body" in new Setup {
      val emailAddress = LaxEmailAddress("bob@example.com")
      GetPersonForEmail.stubSuccess(emailAddress)

      val result = await(objInTest.getPersonForEmail(emailAddress))

      val expectedResponse = DeskproLinkedOrganisationWrapper(
        List(DeskproPersonResponse(63, Some(emailAddress.text), "Andy Spaven")),
        DeskproLinkedOrganisationObject(Map.empty)
      )

      result shouldBe expectedResponse
    }

    "throw UpstreamErrorResponse when error response returned from deskpro" in new Setup {
      val emailAddress = LaxEmailAddress("bob@example.com")
      GetPersonForEmail.stubFailure(emailAddress)

      intercept[UpstreamErrorResponse] {
        await(objInTest.getPersonForEmail(emailAddress))
      }
    }
  }
}
