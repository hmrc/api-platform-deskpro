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

package uk.gov.hmrc.apiplatformdeskpro.service

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import uk.gov.hmrc.apiplatformdeskpro.connector.DeskproConnector
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproOrganisation, DeskproPerson, OrganisationId}
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

class OrganisationServiceSpec extends AsyncHmrcSpec {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockDeskproConnector: DeskproConnector = mock[DeskproConnector]
    val underTest                              = new OrganisationService(mockDeskproConnector)
    val orgName1                               = "Test Orgname"
    val orgName2                               = "Test Orgname2"
    val personName1                            = "Bob Emu"
    val personName2                            = "Hope Ostritch"
    val personName3                            = "Jimmy Emu"
    val personEmail1                           = "email@address.com"
    val personEmail3                           = "email3@address.com"

    val organisationId1: OrganisationId                 = OrganisationId("1")
    val organisationId2: OrganisationId                 = OrganisationId("2")
    val orgResponse: DeskproOrganisationWrapperResponse = DeskproOrganisationWrapperResponse(DeskproOrganisationResponse(organisationId1.value.toInt, orgName1))
    val defaultMeta: DeskproMetaResponse                = DeskproMetaResponse(DeskproPaginationResponse(1, 1))
  }

  "OrganisationService" when {
    "getOrganisationById" should {
      "successfully return DeskproOrganisation when both organisation and people are returned from connector" in new Setup {

        val peopleResponse = DeskproPeopleResponse(
          List(
            DeskproPersonResponse(Some(personEmail1), personName1),
            DeskproPersonResponse(None, personName2)
          ),
          defaultMeta
        )

        when(mockDeskproConnector.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.successful(orgResponse))
        when(mockDeskproConnector.getPeopleByOrganisationId(*[OrganisationId], *)(*)).thenReturn(Future.successful(peopleResponse))

        val result = await(underTest.getOrganisationById(organisationId1))

        result shouldBe DeskproOrganisation(
          organisationId = organisationId1,
          organisationName = orgName1,
          people = List(DeskproPerson(personName1, personEmail1))
        )
      }

      "successfully return DeskproOrganisation when both organisation and multiple pages of people are returned from connector" in new Setup {

        val firstPageResponse  = DeskproPeopleResponse(
          List(
            DeskproPersonResponse(Some(personEmail1), personName1),
            DeskproPersonResponse(None, personName2)
          ),
          DeskproMetaResponse(DeskproPaginationResponse(1, 3))
        )
        val secondPageResponse = DeskproPeopleResponse(
          List(
            DeskproPersonResponse(Some(personEmail3), personName3)
          ),
          DeskproMetaResponse(DeskproPaginationResponse(2, 3))
        )

        when(mockDeskproConnector.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.successful(orgResponse))
        when(mockDeskproConnector.getPeopleByOrganisationId(*[OrganisationId], eqTo(1))(*)).thenReturn(Future.successful(firstPageResponse))
        when(mockDeskproConnector.getPeopleByOrganisationId(*[OrganisationId], eqTo(2))(*)).thenReturn(Future.successful(secondPageResponse))
        when(mockDeskproConnector.getPeopleByOrganisationId(*[OrganisationId], eqTo(3))(*)).thenReturn(Future.successful(secondPageResponse))

        val result = await(underTest.getOrganisationById(organisationId1))

        result.organisationId shouldBe organisationId1
        result.organisationName shouldBe orgName1
        result.people shouldBe List(DeskproPerson(personName3, personEmail3), DeskproPerson(personName1, personEmail1))

      }

      "successfully return DeskproOrganisation when an organisation but no people are returned from connector" in new Setup {
        val peopleResponse = DeskproPeopleResponse(List.empty, defaultMeta)
        when(mockDeskproConnector.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.successful(orgResponse))

        when(mockDeskproConnector.getPeopleByOrganisationId(*[OrganisationId], *)(*)).thenReturn(Future.successful(peopleResponse))

        val result = await(underTest.getOrganisationById(organisationId1))

        result shouldBe DeskproOrganisation(
          organisationId = organisationId1,
          organisationName = orgName1,
          people = List()
        )
      }

      "propagate an Upstream error in getOrganisationById" in new Setup {
        when(mockDeskproConnector.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.failed(UpstreamErrorResponse("not found", 404)))

        intercept[UpstreamErrorResponse] {
          await(underTest.getOrganisationById(organisationId1))
        }

      }
      "propagate an Upstream error in getOrganisationWithPeopleById" in new Setup {
        when(mockDeskproConnector.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.successful(orgResponse))
        when(mockDeskproConnector.getPeopleByOrganisationId(*[OrganisationId], *)(*)).thenReturn(Future.failed(UpstreamErrorResponse("not found", 404)))

        intercept[UpstreamErrorResponse] {
          await(underTest.getOrganisationById(organisationId1))
        }

      }
    }
  }

  "getOrganisationsByEmail" should {
    "successfully return DeskproOrganisation when organisations are returned from connector" in new Setup {

      val organisationWrapper = DeskproLinkedOrganisationWrapper(
        DeskproLinkedOrganisationObject(Map(
          organisationId1.value -> DeskproOrganisationResponse(organisationId1.value.toInt, orgName1),
          organisationId2.value -> DeskproOrganisationResponse(organisationId2.value.toInt, orgName2)
        ))
      )

      when(mockDeskproConnector.getOrganisationsForPersonEmail(*[LaxEmailAddress])(*))
        .thenReturn(Future.successful(organisationWrapper))

      val result = await(underTest.getOrganisationsByEmail(LaxEmailAddress(personEmail1)))

      val expectedResult = List(
        DeskproOrganisation(organisationId1, orgName1, List()),
        DeskproOrganisation(organisationId2, orgName2, List())
      )

      result shouldBe expectedResult
    }

    "return empty list when no organisations are returned from connector" in new Setup {
      val organisationWrapper = DeskproLinkedOrganisationWrapper(DeskproLinkedOrganisationObject(Map.empty))

      when(mockDeskproConnector.getOrganisationsForPersonEmail(*[LaxEmailAddress])(*))
        .thenReturn(Future.successful(organisationWrapper))

      val result = await(underTest.getOrganisationsByEmail(LaxEmailAddress(personEmail1)))

      result shouldBe List.empty
    }

    "propagate an Upstream error in getOrganisationsForPersonEmail" in new Setup {
      when(mockDeskproConnector.getOrganisationsForPersonEmail(*[LaxEmailAddress])(*))
        .thenReturn(Future.failed(UpstreamErrorResponse("auth fail", 401)))

      intercept[UpstreamErrorResponse] {
        await(underTest.getOrganisationsByEmail(LaxEmailAddress(personEmail1)))
      }
    }
  }
}
