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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{DeskproLinkedObject, DeskproOrganisationResponse, DeskproPersonResponse, DeskproResponse}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproOrganisation, DeskproPerson, OrganisationId}
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

class OrganisationServiceSpec extends AsyncHmrcSpec {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockDeskproConnector: DeskproConnector = mock[DeskproConnector]
    val underTest                              = new OrganisationService(mockDeskproConnector)

  }

  "OrganisationService" should {
    "successfully return  DeskproOrganisation when returned from connector" in new Setup {
      private val orgName                        = "Test Orgname"
      private val personName                     = "Bob Emu"
      private val personEmail                    = "email@address.com"
      private val organisationId: OrganisationId = OrganisationId("2")
      val response                               = DeskproResponse(
        DeskproLinkedObject(
          person = Map(
            "1" -> DeskproPersonResponse(Some(personEmail), personName),
            "2" -> DeskproPersonResponse(None, personName)
          ),
          organization = Map("2" -> DeskproOrganisationResponse(2, orgName))
        )
      )

      when(mockDeskproConnector.getOrganisationById(*[OrganisationId])(*)).thenReturn(Future.successful(response))

      val result = await(underTest.getOrganisationById(organisationId))

      result shouldBe DeskproOrganisation(
        organisationId = organisationId,
        organisationName = orgName,
        persons = List(DeskproPerson(personName, personEmail))
      )
    }

  }
}
