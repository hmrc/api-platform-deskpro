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

import play.api.libs.json._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproOrganisation, DeskproPerson, OrganisationId}

case class DeskproOrganisationResponse(id: Int, name: String)

object DeskproOrganisationResponse {
  implicit val format: OFormat[DeskproOrganisationResponse] = Json.format[DeskproOrganisationResponse]
}

case class DeskproPersonResponse(primary_email: Option[String], name: String)

object DeskproPersonResponse {
  implicit val format: OFormat[DeskproPersonResponse] = Json.format[DeskproPersonResponse]
}

case class DeskproLinkedObject(person: Map[String, DeskproPersonResponse], organization: Map[String, DeskproOrganisationResponse])

object DeskproLinkedObject {
  implicit val format: OFormat[DeskproLinkedObject] = Json.format[DeskproLinkedObject]
}

case class DeskproResponse(linked: DeskproLinkedObject)

object DeskproResponse {
  implicit val format: OFormat[DeskproResponse]                             = Json.format[DeskproResponse]

  def toDeskproOrganisation(response: DeskproResponse): DeskproOrganisation = {
    val organisation: DeskproOrganisationResponse = response.linked.organization.values.head
    val persons: List[DeskproPersonResponse]      = response.linked.person.values.toList.filter(_.primary_email.isDefined)

    DeskproOrganisation(OrganisationId(organisation.id.toString), organisation.name, persons.map(per => DeskproPerson(per.name, per.primary_email.get)))
  }
}
