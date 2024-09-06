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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

case class DeskproOrganisationResponse(id: Int, name: String)

object DeskproOrganisationResponse {
  implicit val reads: Reads[DeskproOrganisationResponse] = Json.reads[DeskproOrganisationResponse]
}

case class DeskproOrganisationWrapperResponse(data: DeskproOrganisationResponse)

object DeskproOrganisationWrapperResponse {
  implicit val reads: Reads[DeskproOrganisationWrapperResponse] = Json.reads[DeskproOrganisationWrapperResponse]
}

case class DeskproPersonResponse(primary_email: Option[String], name: String)

object DeskproPersonResponse {
  implicit val reads: Reads[DeskproPersonResponse] = Json.reads[DeskproPersonResponse]
}

case class DeskproLinkedOrganisationObject(organisations: Map[String, DeskproOrganisationResponse])

object DeskproLinkedOrganisationObject {

  implicit val reads: Reads[DeskproLinkedOrganisationObject] = (__ \ "organization")
    .readWithDefault(Map.empty[String, DeskproOrganisationResponse])
    .map(DeskproLinkedOrganisationObject(_))
}
case class DeskproLinkedOrganisationWrapper(linked: DeskproLinkedOrganisationObject)

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
