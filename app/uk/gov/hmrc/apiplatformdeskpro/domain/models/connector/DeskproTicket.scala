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

// case class DeskproTicketPerson(
//     name: String,
//     email: String
//   )

case class DeskproTicketMessage(
    message: String,
    format: String = "html"
  )

object DeskproTicketMessage {
  def fromRaw(message: String): DeskproTicketMessage = DeskproTicketMessage(message.replaceAll(Properties.lineSeparator, "<br>"))
}

case class DeskproTicket(
    person: DeskproPerson,
    subject: String,
    message: DeskproTicketMessage,
    brand: Int,
    fields: Map[String, String] = Map.empty
  )

object DeskproTicket {
  implicit val ticketMessageFormat: OFormat[DeskproTicketMessage] = Json.format[DeskproTicketMessage]
  implicit val ticketFormat: OFormat[DeskproTicket]               = Json.format[DeskproTicket]

}

sealed trait TicketResult

case object TicketCreated                    extends TicketResult
case class DeskproTicketCreated(ref: String) extends TicketResult

case object DeskproTicketCreated {
  implicit val reader: Reads[DeskproTicketCreated] = (__ \ "data" \ "ref").read[String].map(DeskproTicketCreated(_))
}
