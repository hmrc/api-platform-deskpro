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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.{MarkPersonInactiveRequest, UpdatePersonByEmailRequest}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproPersonNotFound, DeskproPersonUpdateFailure, DeskproPersonUpdateSuccess}
import uk.gov.hmrc.apiplatformdeskpro.service.PersonService
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, _}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class PersonController @Inject() (personService: PersonService, cc: ControllerComponents, auth: BackendAuthComponents)(implicit val ec: ExecutionContext)
    extends BackendController(cc) with ApplicationLogger with JsonUtils {

  def updatePersonByEmail(): Action[AnyContent] =
    auth.authorizedAction(predicate = Predicate.Permission(Resource.from("api-platform-deskpro", "people/all"), IAAction("WRITE"))).async {
      implicit request: AuthenticatedRequest[AnyContent, Unit] =>
        withJsonBodyFromAnyContent[UpdatePersonByEmailRequest] { parsedRequest =>
          personService.updatePersonByEmail(parsedRequest.email, parsedRequest.name)
            .map {
              case DeskproPersonUpdateSuccess => Ok
              case DeskproPersonUpdateFailure => InternalServerError
            } recover recovery
        }
    }

  def markPersonInactive(): Action[AnyContent] =
    auth.authorizedAction(predicate = Predicate.Permission(Resource.from("api-platform-deskpro", "people/all"), IAAction("WRITE"))).async {
      implicit request: AuthenticatedRequest[AnyContent, Unit] =>
        withJsonBodyFromAnyContent[MarkPersonInactiveRequest] { parsedRequest =>
          personService.markPersonInactive(parsedRequest.email)
            .map {
              case DeskproPersonUpdateSuccess => Ok
              case DeskproPersonUpdateFailure => InternalServerError
            } recover recovery
        }
    }

  private def recovery: PartialFunction[Throwable, Result] = {
    case DeskproPersonNotFound(message) => handleNotFound(message)
    case e: Throwable                   => handleException(e)
  }

  private[controller] def handleNotFound(message: String): Result = {
    logger.info(message)
    NotFound(Json.obj(
      "code"    -> "PERSON_NOT_FOUND",
      "message" -> message
    ))
  }

  private[controller] def handleException(e: Throwable): Result = {
    logger.error(s"An unexpected error occurred: ${e.getMessage}", e)
    InternalServerError(Json.obj(
      "code"    -> "UNKNOWN_ERROR",
      "message" -> "Unknown error occurred"
    ))
  }
}
