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

import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.{Json, JsonConfiguration, OFormat}

sealed trait QueryPersonRequest

case class GetOrganisationByPersonEmailRequest(
    primaryEmail: String,
    include: String = "organization_member,organization"
  )

object GetOrganisationByPersonEmailRequest extends QueryPersonRequest {
  implicit val config                                               = JsonConfiguration(SnakeCase)
  implicit val format: OFormat[GetOrganisationByPersonEmailRequest] = Json.format[GetOrganisationByPersonEmailRequest]
}

case class GetPersonByEmailRequest(
    primaryEmail: String
  )

object GetPersonByEmailRequest extends QueryPersonRequest {
  implicit val config                                   = JsonConfiguration(SnakeCase)
  implicit val format: OFormat[GetPersonByEmailRequest] = Json.format[GetPersonByEmailRequest]
}