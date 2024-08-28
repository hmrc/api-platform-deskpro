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

package uk.gov.hmrc.apiplatformdeskpro.controller

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.OrganisationId
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.GetOrganisationByEmailRequest
import uk.gov.hmrc.apiplatformdeskpro.service.OrganisationService
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class OrganisationController @Inject() (organisationService: OrganisationService, cc: ControllerComponents)(implicit val ec: ExecutionContext)
    extends BackendController(cc) with ApplicationLogger with JsonUtils {

  def getOrganisation(organisationId: OrganisationId): Action[AnyContent] = Action.async { implicit request =>
    organisationService.getOrganisationById(organisationId)
      .map { deskproOrganisation =>
        Ok(Json.toJson(deskproOrganisation))
      } recover recovery
  }

  def getOrganisationByPersonEmail(): Action[AnyContent] = Action.async { implicit request =>
    withJsonBodyFromAnyContent[GetOrganisationByEmailRequest] { parsedRequest =>
      organisationService.getOrganisationByEmail(parsedRequest.email)
        .map { deskproOrganisation =>
          Ok(Json.toJson(deskproOrganisation))
        } recover recovery
    }
  }

  def recovery: PartialFunction[Throwable, Result] = {
    case UpstreamErrorResponse(message, 404, _, _) => handleNotFound(message)
    case e: Throwable                              =>
      logger.error(s"Error occurred: ${e.getMessage}", e)
      handleException(e)
  }

  private[controller] def handleNotFound(message: String): Result = {
    NotFound(Json.obj(
      "code"    -> "ORGANISATION_NOT_FOUND",
      "message" -> message
    ))
  }

  private[controller] def handleException(e: Throwable) = {
    logger.error(s"An unexpected error occurred: ${e.getMessage}", e)
    InternalServerError(Json.obj(
      "code"    -> "UNKNOWN_ERROR",
      "message" -> "Unknown error occurred"
    ))
  }
}
