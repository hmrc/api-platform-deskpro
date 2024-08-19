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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.connector.DeskproConnector
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{DeskproTicket, DeskproTicketCreated, DeskproTicketMessage}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{CreateTicketRequest, DeskproTicketCreationFailed, _}
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class CreateTicketService @Inject() (
    deskproConnector: DeskproConnector,
    config: AppConfig
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger {

  def submitTicket(createTicketRequest: CreateTicketRequest)(implicit hc: HeaderCarrier): Future[Either[DeskproTicketCreationFailed, DeskproTicketCreated]] = {

    val deskproTicket: DeskproTicket = createDeskproTicket(createTicketRequest)
    deskproConnector.createTicket(deskproTicket)
  }

  private def createDeskproTicket(request: CreateTicketRequest): DeskproTicket = {

    val maybeOrganisation           = request.organisation.fold(Map.empty[String, String])(v => Map(config.deskproOrganisation -> v))
    val maybeTeamMemberEmailAddress = request.teamMemberEmailAddress.fold(Map.empty[String, String])(v => Map(config.deskproTeamMemberEmail -> v))
    val maybeApiName                = request.apiName.fold(Map.empty[String, String])(v => Map(config.deskproApiName -> v))
    val maybeApplicationId          = request.applicationId.fold(Map.empty[String, String])(v => Map(config.deskproApplicationId -> v))
    val maybeSupportReason          = request.supportReason.fold(Map.empty[String, String])(v => Map(config.deskproSupportReason -> v))

    val fields = maybeOrganisation ++ maybeTeamMemberEmailAddress ++ maybeApiName ++ maybeApplicationId ++ maybeSupportReason

    DeskproTicket(
      DeskproPerson(request.person.name, request.person.email),
      request.subject,
      DeskproTicketMessage.fromRaw(request.message),
      config.deskproBrand,
      fields
    )
  }
}
