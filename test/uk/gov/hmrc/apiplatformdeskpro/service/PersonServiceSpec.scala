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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.DeskproPersonCache
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproPersonNotFound, DeskproPersonUpdateFailure, DeskproPersonUpdateSuccess}
import uk.gov.hmrc.apiplatformdeskpro.repository.DeskproPersonCacheRepository
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class PersonServiceSpec extends AsyncHmrcSpec with FixedClock {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockDeskproConnector: DeskproConnector                         = mock[DeskproConnector]
    val mockDeskproPersonCacheRepository: DeskproPersonCacheRepository = mock[DeskproPersonCacheRepository]

    val underTest      = new PersonService(mockDeskproConnector, mockDeskproPersonCacheRepository, clock)
    val personId1: Int = 1
    val personName1    = "Bob Emu"
    val personName2    = "Hope Ostritch"
    val personName3    = "Jimmy Emu"
    val personEmail1   = LaxEmailAddress("email@address.com")
    val personEmail3   = "email3@address.com"
  }

  "PersonService" when {

    "getPersonForEmail" should {
      "successfully return person id when person is found in cache" in new Setup {

        when(mockDeskproPersonCacheRepository.fetchByEmailAddress(eqTo(personEmail1)))
          .thenReturn(Future.successful(Some(DeskproPersonCache(personEmail1, personId1, instant))))

        val result = await(underTest.getPersonForEmail(personEmail1))

        result shouldBe personId1

        verify(mockDeskproPersonCacheRepository).fetchByEmailAddress(eqTo(personEmail1))
        verify(mockDeskproConnector, never).getPersonForEmail(eqTo(personEmail1))(*)
        verify(mockDeskproPersonCacheRepository, never).saveDeskproPersonCache(*)
      }

      "successfully return person id when person is found in deskpro but missing from cache" in new Setup {
        val wrapper = DeskproLinkedOrganisationWrapper(
          List(DeskproPersonResponse(personId1, Some(personEmail1.text), personName1)),
          DeskproLinkedOrganisationObject(Map.empty)
        )

        when(mockDeskproPersonCacheRepository.fetchByEmailAddress(eqTo(personEmail1)))
          .thenReturn(Future.successful(None))
        when(mockDeskproConnector.getPersonForEmail(eqTo(personEmail1))(*))
          .thenReturn(Future.successful(wrapper))
        when(mockDeskproPersonCacheRepository.saveDeskproPersonCache(*))
          .thenReturn(Future.successful(Some(DeskproPersonCache(personEmail1, personId1, instant))))

        val result = await(underTest.getPersonForEmail(personEmail1))

        result shouldBe personId1

        verify(mockDeskproPersonCacheRepository).fetchByEmailAddress(eqTo(personEmail1))
        verify(mockDeskproConnector).getPersonForEmail(eqTo(personEmail1))(*)
        verify(mockDeskproPersonCacheRepository).saveDeskproPersonCache(eqTo(DeskproPersonCache(personEmail1, personId1, instant)))
      }

      "throw an DeskproPersonNotFound exception when person is not found" in new Setup {
        val wrapper = DeskproLinkedOrganisationWrapper(
          List.empty,
          DeskproLinkedOrganisationObject(Map.empty)
        )

        when(mockDeskproPersonCacheRepository.fetchByEmailAddress(eqTo(personEmail1)))
          .thenReturn(Future.successful(None))
        when(mockDeskproConnector.getPersonForEmail(eqTo(personEmail1))(*))
          .thenReturn(Future.successful(wrapper))

        intercept[DeskproPersonNotFound] {
          await(underTest.getPersonForEmail(personEmail1))
        }

        verify(mockDeskproPersonCacheRepository).fetchByEmailAddress(eqTo(personEmail1))
        verify(mockDeskproConnector).getPersonForEmail(eqTo(personEmail1))(*)
      }
    }

    "updatePersonByEmail" should {
      "successfully return DeskproPersonUpdateSuccess when person is found and updated successfully" in new Setup {
        val wrapper = DeskproLinkedOrganisationWrapper(
          List(DeskproPersonResponse(personId1, Some(personEmail1.text), personName1)),
          DeskproLinkedOrganisationObject(Map.empty)
        )

        when(mockDeskproPersonCacheRepository.fetchByEmailAddress(eqTo(personEmail1)))
          .thenReturn(Future.successful(None))
        when(mockDeskproConnector.getPersonForEmail(eqTo(personEmail1))(*))
          .thenReturn(Future.successful(wrapper))
        when(mockDeskproPersonCacheRepository.saveDeskproPersonCache(*))
          .thenReturn(Future.successful(Some(DeskproPersonCache(personEmail1, personId1, instant))))
        when(mockDeskproConnector.updatePerson(eqTo(personId1), eqTo(personName2))(*))
          .thenReturn(Future.successful(DeskproPersonUpdateSuccess))

        val result = await(underTest.updatePersonByEmail(personEmail1, personName2))

        result shouldBe DeskproPersonUpdateSuccess

        verify(mockDeskproConnector).getPersonForEmail(eqTo(personEmail1))(*)
        verify(mockDeskproConnector).updatePerson(eqTo(personId1), eqTo(personName2))(*)
      }

      "return DeskproPersonUpdateFailure when person is found but failed to update" in new Setup {
        val wrapper = DeskproLinkedOrganisationWrapper(
          List(DeskproPersonResponse(personId1, Some(personEmail1.text), personName1)),
          DeskproLinkedOrganisationObject(Map.empty)
        )

        when(mockDeskproPersonCacheRepository.fetchByEmailAddress(eqTo(personEmail1)))
          .thenReturn(Future.successful(None))
        when(mockDeskproConnector.getPersonForEmail(eqTo(personEmail1))(*))
          .thenReturn(Future.successful(wrapper))
        when(mockDeskproPersonCacheRepository.saveDeskproPersonCache(*))
          .thenReturn(Future.successful(Some(DeskproPersonCache(personEmail1, personId1, instant))))
        when(mockDeskproConnector.updatePerson(eqTo(personId1), eqTo(personName2))(*))
          .thenReturn(Future.successful(DeskproPersonUpdateFailure))

        val result = await(underTest.updatePersonByEmail(personEmail1, personName2))

        result shouldBe DeskproPersonUpdateFailure

        verify(mockDeskproConnector).getPersonForEmail(eqTo(personEmail1))(*)
      }

      "throw an DeskproPersonNotFound exception when person is not found" in new Setup {
        val wrapper = DeskproLinkedOrganisationWrapper(
          List.empty,
          DeskproLinkedOrganisationObject(Map.empty)
        )

        when(mockDeskproPersonCacheRepository.fetchByEmailAddress(eqTo(personEmail1)))
          .thenReturn(Future.successful(None))
        when(mockDeskproConnector.getPersonForEmail(eqTo(personEmail1))(*))
          .thenReturn(Future.successful(wrapper))

        intercept[DeskproPersonNotFound] {
          await(underTest.updatePersonByEmail(personEmail1, personName2))
        }

        verify(mockDeskproConnector, never).updatePerson(*, *)(*)
      }

      "propagate an Upstream error in getPersonForEmail" in new Setup {
        when(mockDeskproPersonCacheRepository.fetchByEmailAddress(eqTo(personEmail1)))
          .thenReturn(Future.successful(None))
        when(mockDeskproConnector.getPersonForEmail(eqTo(personEmail1))(*))
          .thenReturn(Future.failed(UpstreamErrorResponse("auth fail", 401)))

        intercept[UpstreamErrorResponse] {
          await(underTest.updatePersonByEmail(personEmail1, personName2))
        }
      }
    }

    "markPersonInactive" should {
      "successfully return MarkPersonInactiveSuccess when person is found and updated successfully" in new Setup {
        val wrapper = DeskproLinkedOrganisationWrapper(
          List(DeskproPersonResponse(personId1, Some(personEmail1.text), personName1)),
          DeskproLinkedOrganisationObject(Map.empty)
        )

        when(mockDeskproPersonCacheRepository.fetchByEmailAddress(eqTo(personEmail1)))
          .thenReturn(Future.successful(None))
        when(mockDeskproConnector.getPersonForEmail(eqTo(personEmail1))(*))
          .thenReturn(Future.successful(wrapper))
        when(mockDeskproPersonCacheRepository.saveDeskproPersonCache(*))
          .thenReturn(Future.successful(Some(DeskproPersonCache(personEmail1, personId1, instant))))
        when(mockDeskproConnector.markPersonInactive(eqTo(personId1))(*))
          .thenReturn(Future.successful(DeskproPersonUpdateSuccess))

        val result = await(underTest.markPersonInactive(personEmail1))

        result shouldBe DeskproPersonUpdateSuccess

        verify(mockDeskproConnector).getPersonForEmail(eqTo(personEmail1))(*)
        verify(mockDeskproConnector).markPersonInactive(eqTo(personId1))(*)
      }

      "return DeskproPersonUpdateFailure when person is found but failed to update" in new Setup {
        val wrapper = DeskproLinkedOrganisationWrapper(
          List(DeskproPersonResponse(personId1, Some(personEmail1.text), personName1)),
          DeskproLinkedOrganisationObject(Map.empty)
        )

        when(mockDeskproPersonCacheRepository.fetchByEmailAddress(eqTo(personEmail1)))
          .thenReturn(Future.successful(None))
        when(mockDeskproConnector.getPersonForEmail(eqTo(personEmail1))(*))
          .thenReturn(Future.successful(wrapper))
        when(mockDeskproPersonCacheRepository.saveDeskproPersonCache(*))
          .thenReturn(Future.successful(Some(DeskproPersonCache(personEmail1, personId1, instant))))
        when(mockDeskproConnector.markPersonInactive(eqTo(personId1))(*))
          .thenReturn(Future.successful(DeskproPersonUpdateFailure))

        val result = await(underTest.markPersonInactive(personEmail1))

        result shouldBe DeskproPersonUpdateFailure

        verify(mockDeskproConnector).getPersonForEmail(eqTo(personEmail1))(*)
        verify(mockDeskproConnector).markPersonInactive(eqTo(personId1))(*)
      }

      "throw an DeskproPersonNotFound exception when person is not found" in new Setup {
        val wrapper = DeskproLinkedOrganisationWrapper(
          List.empty,
          DeskproLinkedOrganisationObject(Map.empty)
        )

        when(mockDeskproPersonCacheRepository.fetchByEmailAddress(eqTo(personEmail1)))
          .thenReturn(Future.successful(None))
        when(mockDeskproConnector.getPersonForEmail(eqTo(personEmail1))(*))
          .thenReturn(Future.successful(wrapper))

        intercept[DeskproPersonNotFound] {
          await(underTest.markPersonInactive(personEmail1))
        }

        verify(mockDeskproConnector, never).updatePerson(*, *)(*)
      }

      "propagate an Upstream error in getPersonForEmail" in new Setup {
        when(mockDeskproPersonCacheRepository.fetchByEmailAddress(eqTo(personEmail1)))
          .thenReturn(Future.successful(None))
        when(mockDeskproConnector.getPersonForEmail(eqTo(personEmail1))(*))
          .thenReturn(Future.failed(UpstreamErrorResponse("auth fail", 401)))

        intercept[UpstreamErrorResponse] {
          await(underTest.markPersonInactive(personEmail1))
        }
      }
    }
  }
}
