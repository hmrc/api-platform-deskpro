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

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.connector.DeskproConnector
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{DeskproTicket, DeskproTicketCreated, DeskproTicketMessage}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{CreateTicketRequest, DeskproPerson}
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId

class CreateTicketServiceSpec extends AsyncHmrcSpec {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockDeskproConnector = mock[DeskproConnector]
    val mockAppConfig        = mock[AppConfig]

    val fullName        = "Bob Holness"
    val email           = "bob@exmaple.com"
    val subject         = "Subject of the ticket"
    val message         = "This is where the message for the ticket goes"
    val apiName         = "apiName"
    val applicationId   = ApplicationId.random.toString()
    val organisation    = "organisation"
    val supportReason   = "supportReason"
    val teamMemberEmail = "frank@example.com"
    val ref             = "ref"
    val brand           = 1
    val underTest       = new CreateTicketService(mockDeskproConnector, mockAppConfig)
  }

  "CreateTicketService" should {
    "successfully create a new deskpro ticket with all custom fields" in new Setup {

      val createTicketRequest = CreateTicketRequest(
        fullName,
        email,
        subject,
        message,
        Some(apiName),
        Some(applicationId),
        Some(organisation),
        Some(supportReason),
        Some(teamMemberEmail)
      )

      val fields                = Map("2" -> apiName, "3" -> applicationId, "4" -> organisation, "5" -> supportReason, "6" -> teamMemberEmail)
      val expectedDeskproTicket = DeskproTicket(DeskproPerson(fullName, email), subject, DeskproTicketMessage(message), brand, fields)

      when(mockDeskproConnector.createTicket(*)(*)).thenReturn(Future.successful(Right(DeskproTicketCreated(ref))))

      when(mockAppConfig.deskproBrand).thenReturn(brand)
      when(mockAppConfig.deskproApiName).thenReturn("2")
      when(mockAppConfig.deskproApplicationId).thenReturn("3")
      when(mockAppConfig.deskproOrganisation).thenReturn("4")
      when(mockAppConfig.deskproSupportReason).thenReturn("5")
      when(mockAppConfig.deskproTeamMemberEmail).thenReturn("6")

      val result = await(underTest.submitTicket(createTicketRequest))

      result shouldBe Right(DeskproTicketCreated(ref))
      verify(mockDeskproConnector).createTicket(eqTo(expectedDeskproTicket))(*)
    }

    "successfully create a new deskpro ticket with no custom fields" in new Setup {

      val createTicketRequest = CreateTicketRequest(
        fullName,
        email,
        subject,
        message,
        None,
        None,
        None,
        None,
        None
      )

      val fields: Map[String, String] = Map.empty
      val expectedDeskproTicket       = DeskproTicket(DeskproPerson(fullName, email), subject, DeskproTicketMessage(message), brand, fields)

      when(mockDeskproConnector.createTicket(*)(*)).thenReturn(Future.successful(Right(DeskproTicketCreated(ref))))

      when(mockAppConfig.deskproBrand).thenReturn(brand)

      val result = await(underTest.submitTicket(createTicketRequest))

      result shouldBe Right(DeskproTicketCreated(ref))
      verify(mockDeskproConnector).createTicket(eqTo(expectedDeskproTicket))(*)
    }
  }
}
