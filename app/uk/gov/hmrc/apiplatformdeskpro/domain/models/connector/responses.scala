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

case class DeskproOrganisationResponse(id: Int, name: String)

object DeskproOrganisationResponse {
  implicit val format: Reads[DeskproOrganisationResponse] = Json.reads[DeskproOrganisationResponse]
}

case class DeskproOrganisationWrapperResponse(data: DeskproOrganisationResponse)

object DeskproOrganisationWrapperResponse {
  implicit val format: Reads[DeskproOrganisationWrapperResponse] = Json.reads[DeskproOrganisationWrapperResponse]
}

case class DeskproPersonResponse(primary_email: Option[String], name: String)

object DeskproPersonResponse {
  implicit val format: Reads[DeskproPersonResponse] = Json.reads[DeskproPersonResponse]
}

case class DeskproLinkedObject(person: Map[String, DeskproPersonResponse])

object DeskproLinkedObject {

  implicit val reads: Reads[DeskproLinkedObject] = (__ \ "person")
    .readWithDefault(Map.empty[String, DeskproPersonResponse])
    .map(DeskproLinkedObject(_))

}

case class DeskproResponse(linked: DeskproLinkedObject)

object DeskproResponse {
  implicit val format: Reads[DeskproResponse] = Json.reads[DeskproResponse]
}
