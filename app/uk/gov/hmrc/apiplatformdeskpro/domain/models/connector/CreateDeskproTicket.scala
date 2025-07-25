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

import play.api.libs.json._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.DeskproPerson

case class DeskproTicketMessage(
    message: String,
    person: DeskproPerson,
    format: String = "html"
  )

object DeskproTicketMessage {
  def fromRaw(message: String, person: DeskproPerson): DeskproTicketMessage = DeskproTicketMessage(message.replaceAll(Properties.lineSeparator, "<br>"), person)
}

case class CreateDeskproTicket(
    person: DeskproPerson,
    subject: String,
    message: DeskproTicketMessage,
    brand: Int,
    fields: Map[String, String] = Map.empty
  )

object CreateDeskproTicket {
  implicit val ticketMessageFormat: OFormat[DeskproTicketMessage] = Json.format[DeskproTicketMessage]
  implicit val ticketFormat: OFormat[CreateDeskproTicket]         = Json.format[CreateDeskproTicket]

}

sealed trait TicketResult

case object TicketCreated                    extends TicketResult
case class DeskproTicketCreated(ref: String) extends TicketResult

case object DeskproTicketCreated {
  implicit val reader: Reads[DeskproTicketCreated] = (__ \ "data" \ "ref").read[String].map(DeskproTicketCreated(_))
}
