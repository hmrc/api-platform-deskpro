package uk.gov.hmrc.apiplatformdeskpro.domain.models

import play.api.libs.json.Json

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class OrganisationId(value: String) extends AnyVal {
  override def toString()= value
}
object OrganisationId{
  implicit val format: Format[OrganisationId] = Json.valueFormat[OrganisationId]
}
case class DeskProOrganisation (organisationId: OrganisationId, organisationName: String, persons: List[DeskproPerson])
object DeskProOrganisation{
  implicit val format: OFormat[DeskProOrganisation] = Json.format[DeskProOrganisation]
}
