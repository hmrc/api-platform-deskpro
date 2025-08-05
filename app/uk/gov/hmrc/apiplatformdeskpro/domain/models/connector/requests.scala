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

package uk.gov.hmrc.apiplatformdeskpro.domain.models.connector

import scala.util.Properties

import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Format, Json, JsonConfiguration, OFormat}

sealed trait QueryPersonRequest

case class GetOrganisationByPersonEmailRequest(
    primaryEmail: String,
    include: String = "organization_member,organization"
  )

object GetOrganisationByPersonEmailRequest extends QueryPersonRequest {
  implicit val config: Aux[Json.MacroOptions]                       = JsonConfiguration(SnakeCase)
  implicit val format: OFormat[GetOrganisationByPersonEmailRequest] = Json.format[GetOrganisationByPersonEmailRequest]
}

case class GetPersonByEmailRequest(
    primaryEmail: String
  )

object GetPersonByEmailRequest extends QueryPersonRequest {
  implicit val config: Aux[Json.MacroOptions]           = JsonConfiguration(SnakeCase)
  implicit val format: OFormat[GetPersonByEmailRequest] = Json.format[GetPersonByEmailRequest]
}

case class UpdateTicketStatusRequest(
    status: String
  )

object UpdateTicketStatusRequest {
  implicit val updateTicketStatusRequestFormat: Format[UpdateTicketStatusRequest] = Json.format[UpdateTicketStatusRequest]
}

case class CreateResponseRequest(
    person: String,
    message: String
  )

object CreateResponseRequest {
  def fromRaw(person: String, message: String): CreateResponseRequest = CreateResponseRequest(person, message.replaceAll(Properties.lineSeparator, "<br>"))

  implicit val createResponseRequestFormat: Format[CreateResponseRequest] = Json.format[CreateResponseRequest]
}

abstract class TicketStatus(val value: String)

object TicketStatus {
  case object AwaitingAgent extends TicketStatus("awaiting_agent")
  case object Resolved      extends TicketStatus("resolved")
}

/*
 * Batch requests
 */

case class BatchRequestDetails(
    url: String,
    method: String = "GET",
    payload: Option[String] = None
  )

object BatchRequestDetails {
  implicit val format: OFormat[BatchRequestDetails] = Json.format[BatchRequestDetails]
}

sealed trait BatchDetails

case class BatchTicketRequest(
    ticket: BatchRequestDetails,
    messages: BatchRequestDetails
  )

object BatchTicketRequest extends BatchDetails {
  implicit val format: OFormat[BatchTicketRequest] = Json.format[BatchTicketRequest]
}

case class BatchRequest(
    requests: BatchTicketRequest
  )

object BatchRequest {
  implicit val format: OFormat[BatchRequest] = Json.format[BatchRequest]
}
