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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector._

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

case class DeskproAttachment(id: Int, filename: String, url: String, fileSize: String)

object DeskproAttachment {

  def build(response: DeskproAttachmentResponse): DeskproAttachment = {
    DeskproAttachment(response.id, response.blob.filename, response.blob.download_url, response.blob.filesize_readable)
  }
  implicit val format: OFormat[DeskproAttachment]                   = Json.format[DeskproAttachment]
}

case class DeskproMessage(
    id: Int,
    ticketId: Int,
    person: Int,
    dateCreated: Instant,
    isAgentNote: Boolean,
    message: String,
    attachments: List[DeskproAttachment]
  )

object DeskproMessage {

  def build(response: DeskproMessageResponse, attachments: List[DeskproAttachmentResponse]): DeskproMessage = {
    DeskproMessage(
      response.id,
      response.ticket,
      response.person,
      response.date_created,
      (response.is_agent_note > 0),
      response.message,
      response.attachments.flatMap(attachmentId => attachments.find(attachment => attachment.id == attachmentId).map(DeskproAttachment.build))
    )
  }

  implicit val format: OFormat[DeskproMessage] = Json.format[DeskproMessage]
}

case class DeskproTicket(
    id: Int,
    ref: String,
    person: Int,
    personEmail: LaxEmailAddress,
    status: String,
    dateCreated: Instant,
    dateLastUpdated: Instant,
    dateResolved: Option[Instant],
    subject: String,
    messages: List[DeskproMessage]
  )

object DeskproTicket {

  def build(ticketResponse: BatchTicketWrapperResponse, messagesResponse: BatchMessagesWrapperResponse, attachmentResponse: BatchAttachmentsWrapperResponse)
      : Option[DeskproTicket] = {
    ticketResponse.data match {
      case Some(data) => Some(build(data, messagesResponse.data.getOrElse(List.empty), attachmentResponse.data.getOrElse(List.empty)))
      case _          => None
    }
  }

  def build(response: DeskproTicketResponse, messagesResponse: List[DeskproMessageResponse], attachmentResponse: List[DeskproAttachmentResponse]): DeskproTicket = {
    DeskproTicket(
      response.id,
      response.ref,
      response.person,
      LaxEmailAddress(response.person_email),
      response.status,
      response.date_created,
      response.date_status,
      response.date_resolved,
      response.subject,
      messagesResponse.map(msg => DeskproMessage.build(msg, attachmentResponse))
    )
  }

  implicit val format: OFormat[DeskproTicket] = Json.format[DeskproTicket]
}
