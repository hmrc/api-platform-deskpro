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

import java.time.Instant

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

import uk.gov.hmrc.apiplatform.modules.common.domain.services.InstantJsonFormatter

case class DeskproOrganisationResponse(id: Int, name: String)

object DeskproOrganisationResponse {
  implicit val reads: Reads[DeskproOrganisationResponse] = Json.reads[DeskproOrganisationResponse]
}

case class DeskproOrganisationWrapperResponse(data: DeskproOrganisationResponse)

object DeskproOrganisationWrapperResponse {
  implicit val reads: Reads[DeskproOrganisationWrapperResponse] = Json.reads[DeskproOrganisationWrapperResponse]
}

case class DeskproPersonResponse(id: Int, primary_email: Option[String], name: String)

object DeskproPersonResponse {
  implicit val reads: Reads[DeskproPersonResponse] = Json.reads[DeskproPersonResponse]
}

case class DeskproLinkedOrganisationObject(organisations: Map[String, DeskproOrganisationResponse])

object DeskproLinkedOrganisationObject {

  implicit val reads: Reads[DeskproLinkedOrganisationObject] = (__ \ "organization")
    .readWithDefault(Map.empty[String, DeskproOrganisationResponse])
    .map(DeskproLinkedOrganisationObject(_))
}

case class DeskproLinkedOrganisationWrapper(data: List[DeskproPersonResponse], linked: DeskproLinkedOrganisationObject)

object DeskproLinkedOrganisationWrapper {
  implicit val format: Reads[DeskproLinkedOrganisationWrapper] = Json.reads[DeskproLinkedOrganisationWrapper]
}

case class DeskproPaginationResponse(currentPage: Int, totalPages: Int)

object DeskproPaginationResponse {

  implicit val reads: Reads[DeskproPaginationResponse] = (
    (__ \ "current_page").read[Int] and
      (__ \ "total_pages").read[Int]
  )((current, total) => DeskproPaginationResponse(current, total))
}

case class DeskproMetaResponse(pagination: DeskproPaginationResponse)

object DeskproMetaResponse {
  implicit val reads: Reads[DeskproMetaResponse] = Json.reads[DeskproMetaResponse]
}

case class DeskproPeopleResponse(data: List[DeskproPersonResponse], meta: DeskproMetaResponse)

object DeskproPeopleResponse {
  implicit val reads: Reads[DeskproPeopleResponse] = Json.reads[DeskproPeopleResponse]
}

case class DeskproTicketResponse(
    id: Int,
    ref: String,
    person: Int,
    person_email: String,
    status: String,
    date_created: Instant,
    date_last_agent_reply: Option[Instant],
    subject: String
  )

object DeskproTicketResponse {
  implicit val instantFormatter: Reads[Instant] = InstantJsonFormatter.lenientInstantReads

  implicit val reads: Reads[DeskproTicketResponse] = Json.reads[DeskproTicketResponse]
}

case class DeskproTicketsWrapperResponse(data: List[DeskproTicketResponse])

object DeskproTicketsWrapperResponse {
  implicit val reads: Reads[DeskproTicketsWrapperResponse] = Json.reads[DeskproTicketsWrapperResponse]
}

case class DeskproTicketWrapperResponse(data: DeskproTicketResponse)

object DeskproTicketWrapperResponse {
  implicit val reads: Reads[DeskproTicketWrapperResponse] = Json.reads[DeskproTicketWrapperResponse]
}

case class DeskproMessageResponse(id: Int, ticket: Int, person: Int, date_created: Instant, is_agent_note: Int, message_preview_text: String)

object DeskproMessageResponse {
  implicit val instantFormatter: Reads[Instant] = InstantJsonFormatter.lenientInstantReads

  implicit val reads: Reads[DeskproMessageResponse] = Json.reads[DeskproMessageResponse]
}

case class DeskproMessagesWrapperResponse(data: List[DeskproMessageResponse])

object DeskproMessagesWrapperResponse {
  implicit val reads: Reads[DeskproMessagesWrapperResponse] = Json.reads[DeskproMessagesWrapperResponse]
}

/*
 * Batch responses
 */

case class BatchHeadersResponse(
    `status-code`: Int
  )

object BatchHeadersResponse {
  implicit val reads: Reads[BatchHeadersResponse] = Json.reads[BatchHeadersResponse]
}

case class BatchTicketWrapperResponse(
    headers: BatchHeadersResponse,
    data: Option[DeskproTicketResponse]
  )

object BatchTicketWrapperResponse {
  implicit val reads: Reads[BatchTicketWrapperResponse] = Json.reads[BatchTicketWrapperResponse]
}

case class BatchMessagesWrapperResponse(
    headers: BatchHeadersResponse,
    data: Option[List[DeskproMessageResponse]]
  )

object BatchMessagesWrapperResponse {
  implicit val reads: Reads[BatchMessagesWrapperResponse] = Json.reads[BatchMessagesWrapperResponse]
}

case class BatchTicketResponse(
    ticket: BatchTicketWrapperResponse,
    messages: BatchMessagesWrapperResponse
  )

object BatchTicketResponse {
  implicit val reads: Reads[BatchTicketResponse] = Json.reads[BatchTicketResponse]
}

case class BatchResponse(
    responses: BatchTicketResponse
  )

object BatchResponse {
  implicit val reads: Reads[BatchResponse] = Json.reads[BatchResponse]
}
