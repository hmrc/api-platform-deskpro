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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{
  DeskproLinkedPersonObject,
  DeskproLinkedPersonWrapper,
  DeskproOrganisationResponse,
  DeskproOrganisationWrapperResponse,
  DeskproPersonResponse
}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproOrganisation, DeskproPerson, OrganisationId}
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

class OrganisationServiceSpec extends AsyncHmrcSpec {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockDeskproConnector: DeskproConnector          = mock[DeskproConnector]
    val underTest                                       = new OrganisationService(mockDeskproConnector)
    val orgName1                                        = "Test Orgname"
    val orgName2                                        = "Test Orgname2"
    val personName1                                     = "Bob Emu"
    val personName2                                     = "Hope Ostritch"
    val personEmail1                                    = "email@address.com"
    val organisationId1: OrganisationId                 = OrganisationId("1")
    val organisationId2: OrganisationId                 = OrganisationId("2")
    val orgResponse: DeskproOrganisationWrapperResponse = DeskproOrganisationWrapperResponse(DeskproOrganisationResponse(organisationId1.value.toInt, orgName1))

  }

  "OrganisationService" when {
    "getOrganisationById" should {
      "successfully return DeskproOrganisation when both organisation and people are returned from connector" in new Setup {

        val peopleResponse = DeskproLinkedPersonWrapper(
          DeskproLinkedPersonObject(
            person = Map(
              "1" -> DeskproPersonResponse(Some(personEmail1), personName1),
              "2" -> DeskproPersonResponse(None, personName2)
            )
          )
        )

        when(mockDeskproConnector.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.successful(orgResponse))
        when(mockDeskproConnector.getOrganisationWithPeopleById(*[OrganisationId])(*)).thenReturn(Future.successful(peopleResponse))

        val result = await(underTest.getOrganisationById(organisationId1))

        result shouldBe DeskproOrganisation(
          organisationId = organisationId1,
          organisationName = orgName1,
          people = List(DeskproPerson(personName1, personEmail1))
        )
      }

      "successfully return DeskproOrganisation when an organisation but no people are returned from connector" in new Setup {
        val peopleResponse = DeskproLinkedPersonWrapper(
          DeskproLinkedPersonObject(person = Map())
        )
        when(mockDeskproConnector.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.successful(orgResponse))

        when(mockDeskproConnector.getOrganisationWithPeopleById(*[OrganisationId])(*)).thenReturn(Future.successful(peopleResponse))

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
        when(mockDeskproConnector.getOrganisationWithPeopleById(*[OrganisationId])(*)).thenReturn(Future.failed(UpstreamErrorResponse("not found", 404)))

        intercept[UpstreamErrorResponse] {
          await(underTest.getOrganisationById(organisationId1))
        }

      }
    }
  }

//  "getOrganisationsByEmail" should {
//    "successfully return DeskproOrganisation when both organisation and people are returned from connector" in new Setup {
//
//      val peopleResponse = DeskproLinkedPersonWrapper(
//        DeskproLinkedPersonObject(
//          person = Map(
//            "1" -> DeskproPersonResponse(Some(personEmail1), personName1),
//            "2" -> DeskproPersonResponse(None, personName2)
//          )
//        )
//      )
//
//      when(mockDeskproConnector.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.successful(orgResponse))
//      when(mockDeskproConnector.getOrganisationWithPeopleById(*[OrganisationId])(*)).thenReturn(Future.successful(peopleResponse))
//
//      val result = await(underTest.getOrganisationById(organisationId1))
//
//      result shouldBe DeskproOrganisation(
//        organisationId = organisationId1,
//        organisationName = orgName1,
//        people = List(DeskproPerson(personName1, personEmail1))
//      )
//    }
//  }
}
