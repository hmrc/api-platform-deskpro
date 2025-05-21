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

package uk.gov.hmrc.apiplatformdeskpro.domain.models

import java.time.Instant

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.DeskproTicketResponse

case class DeskproTicket(
    id: Int,
    ref: String,
    person: Int,
    status: String,
    dateCreated: Instant,
    dateLastAgentReply: Option[Instant],
    subject: String
  )

object DeskproTicket {

  def build(response: DeskproTicketResponse): DeskproTicket = {
    DeskproTicket(
      response.id,
      response.ref,
      response.person,
      response.status,
      response.date_created,
      response.date_last_agent_reply,
      response.subject
    )
  }

  implicit val format: OFormat[DeskproTicket] = Json.format[DeskproTicket]
}
