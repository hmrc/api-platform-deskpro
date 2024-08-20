package uk.gov.hmrc.apiplatformdeskpro.domain.models.connector
import play.api.libs.json._

case class DeskproOrganisation(id:Int, name: String)

object DeskproOrganisation{
  implicit val format: OFormat[DeskproOrganisation] = Json.format[DeskproOrganisation]
}


case class DeskproPersonResponse(primary_email: Option[String], name: String)

object DeskproPersonResponse{
  implicit val format: OFormat[DeskproPersonResponse] = Json.format[DeskproPersonResponse]
}


case class DeskproLinkedObject(person: Map[String, DeskproPersonResponse], organization: Map[String, DeskproOrganisation])

object DeskproLinkedObject{
  implicit val format: OFormat[DeskproLinkedObject] = Json.format[DeskproLinkedObject]
}


case class DeskproResponse(linked: DeskproLinkedObject)

object DeskproResponse{
  implicit val format: OFormat[DeskproResponse] = Json.format[DeskproResponse]
}