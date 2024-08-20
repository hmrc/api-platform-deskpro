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
import uk.gov.hmrc.apiplatformdeskpro.connector.{DeskproConnector, DeveloperConnector}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.RegisteredUser
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproPersonCreationFailure, DeskproPersonCreationSuccess}
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}

class CreatePersonServiceSpec extends AsyncHmrcSpec {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockDeskproConnector: DeskproConnector     = mock[DeskproConnector]
    val mockDeveloperConnector: DeveloperConnector = mock[DeveloperConnector]
    val mockAppConfig: AppConfig                   = mock[AppConfig]

    val firstName1 = "Bob"
    val lastName1  = "Holness"
    val email1     = "bob@exmaple.com"

    val firstName2 = "John"
    val lastName2  = "Doe"
    val email2     = "jb@exmaple.com"

    val user1 = RegisteredUser(LaxEmailAddress(email1), UserId.random, firstName1, lastName1)
    val user2 = RegisteredUser(LaxEmailAddress(email2), UserId.random, firstName2, lastName2)

    val underTest = new CreatePersonService(mockDeskproConnector, mockDeveloperConnector, mockAppConfig)

    val getUsersResponse = List(user1, user2)

  }

  "CreatePersonService" should {
    "successfully create users when users are returned from developerConnector" in new Setup {
      when(mockDeveloperConnector.searchDevelopers()(*)).thenReturn(Future.successful(getUsersResponse))
      when(mockDeskproConnector.createPerson(*[UserId], *, *)(*)).thenReturn(Future.successful(DeskproPersonCreationSuccess))

      val result = await(underTest.pushNewUsersToDeskpro())
      result shouldBe List(DeskproPersonCreationSuccess, DeskproPersonCreationSuccess)
      verify(mockDeskproConnector).createPerson(eqTo(user1.userId), eqTo(s"$firstName1 $lastName1"), eqTo(email1))(*)
      verify(mockDeskproConnector).createPerson(eqTo(user2.userId), eqTo(s"$firstName2 $lastName2"), eqTo(email2))(*)
    }

    "successfully processes all users when 1st create fails" in new Setup {
      when(mockDeveloperConnector.searchDevelopers()(*)).thenReturn(Future.successful(getUsersResponse))
      when(mockDeskproConnector.createPerson(eqTo(user1.userId), eqTo(s"$firstName1 $lastName1"), eqTo(email1))(*)).thenReturn(Future.successful(DeskproPersonCreationFailure))
      when(mockDeskproConnector.createPerson(eqTo(user2.userId), eqTo(s"$firstName2 $lastName2"), eqTo(email2))(*)).thenReturn(Future.successful(DeskproPersonCreationSuccess))

      val result = await(underTest.pushNewUsersToDeskpro())
      result shouldBe List(DeskproPersonCreationFailure, DeskproPersonCreationSuccess)
      verify(mockDeskproConnector).createPerson(eqTo(user1.userId), eqTo(s"$firstName1 $lastName1"), eqTo(email1))(*)
      verify(mockDeskproConnector).createPerson(eqTo(user2.userId), eqTo(s"$firstName2 $lastName2"), eqTo(email2))(*)
    }

  }
}
