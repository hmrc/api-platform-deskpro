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
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result, Results}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.DeskproTicket
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.GetTicketsByEmailRequest
import uk.gov.hmrc.apiplatformdeskpro.service.TicketService
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.internalauth.client.{BackendAuthComponents, _}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class TicketController @Inject() (ticketService: TicketService, cc: ControllerComponents, auth: BackendAuthComponents)(implicit val ec: ExecutionContext)
    extends BackendController(cc)
    with ApplicationLogger with JsonUtils {

  def getTicketsByPersonEmail(): Action[AnyContent] =
    auth.authorizedAction(predicate = Predicate.Permission(Resource.from("api-platform-deskpro", "tickets/all"), IAAction("READ"))).async {
      implicit request: AuthenticatedRequest[AnyContent, Unit] =>
        withJsonBodyFromAnyContent[GetTicketsByEmailRequest] { parsedRequest =>
          ticketService.getTicketsForPerson(parsedRequest.email, parsedRequest.status)
            .map { tickets =>
              Ok(Json.toJson(tickets))
            } recover recovery
        }
    }

  def fetchTicket(ticketId: Int): Action[AnyContent] =
    auth.authorizedAction(predicate = Predicate.Permission(Resource.from("api-platform-deskpro", "tickets/all"), IAAction("READ"))).async {
      implicit request: AuthenticatedRequest[AnyContent, Unit] =>
        {
          lazy val failed = NotFound(Results.EmptyContent())

          val success = (t: DeskproTicket) => Ok(Json.toJson(t))
          ticketService.batchFetchTicket(ticketId).map(_.fold(failed)(success))
        }
    }

  private def recovery: PartialFunction[Throwable, Result] = {
    case e: Throwable =>
      logger.error(s"Error occurred: ${e.getMessage}", e)
      handleException(e)
  }

  private[controller] def handleException(e: Throwable) = {
    logger.error(s"An unexpected error occurred: ${e.getMessage}", e)
    InternalServerError(Json.obj(
      "code"    -> "UNKNOWN_ERROR",
      "message" -> "Unknown error occurred"
    ))
  }
}
