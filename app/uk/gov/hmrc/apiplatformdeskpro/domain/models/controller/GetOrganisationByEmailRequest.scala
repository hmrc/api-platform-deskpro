package uk.gov.hmrc.apiplatformdeskpro.domain.models.controller

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

case class GetOrganisationByEmailRequest(email: LaxEmailAddress)

object GetOrganisationByEmailRequest {
  implicit val getOrganisationByEmailRequest: OFormat[GetOrganisationByEmailRequest] = Json.format[GetOrganisationByEmailRequest]
}