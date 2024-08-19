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

import play.api.libs.json._

case class CreateTicketRequest(
    person: DeskproPerson,
    subject: String,
    message: String,
    apiName: Option[String],
    applicationId: Option[String],
    organisation: Option[String],
    supportReason: Option[String],
    teamMemberEmailAddress: Option[String]
  )

object CreateTicketRequest {
  implicit val createPersonFormat: OFormat[DeskproPerson]       = Json.format[DeskproPerson]
  implicit val createTicketFormat: OFormat[CreateTicketRequest] = Json.format[CreateTicketRequest]

}
