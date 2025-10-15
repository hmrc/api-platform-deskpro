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
import uk.gov.hmrc.apiplatformdeskpro.domain.models._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.DeskproCreateBlobWrapperResponse
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.{AddAttachmentRequest, CreateTicketResponseRequest, GetTicketsByEmailRequest}
import uk.gov.hmrc.apiplatformdeskpro.service.TicketService
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.internalauth.client._
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
            } recover personNotFoundRecovery
        }
    }

  private def personNotFoundRecovery: PartialFunction[Throwable, Result] = {
    case pnf: DeskproPersonNotFound =>
      val tickets: List[DeskproTicket] = List.empty
      Ok(Json.toJson(tickets)) // Return empty list if person not found
    case e: Throwable               =>
      logger.error(s"Error occurred: ${e.getMessage}", e)
      handleException(e)
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

  def createMessage(ticketId: Int): Action[AnyContent] =
    auth.authorizedAction(predicate = Predicate.Permission(Resource.from("api-platform-deskpro", "tickets/all"), IAAction("WRITE"))).async {
      implicit request: AuthenticatedRequest[AnyContent, Unit] =>
        withJsonBodyFromAnyContent[CreateTicketResponseRequest] { parsedRequest =>
          ticketService.createMessage(ticketId, parsedRequest)
            .map { msg =>
              Ok
            } recover recovery
        }
    }

  def addAttachment(ticketId: Int): Action[AnyContent] = Action.async { implicit request =>
    withJsonBodyFromAnyContent[AddAttachmentRequest] { parsedRequest =>
      ticketService.addAttachment(parsedRequest.fileName, parsedRequest.fileType, ticketId, parsedRequest.message, parsedRequest.userEmail).map {
        case (DeskproCreateBlobWrapperResponse(data), DeskproTicketResponseSuccess) => Ok(s"File id: ${data.blob_id}, auth: ${data.blob_auth}")
        case _                                                                      => InternalServerError
      }
    }
  }

  private def recovery: PartialFunction[Throwable, Result] = {
    case UpstreamErrorResponse(message, 404, _, _) => handleNotFound(message)
    case e: Throwable                              =>
      logger.error(s"Error occurred: ${e.getMessage}", e)
      handleException(e)
  }

  private[controller] def handleNotFound(message: String): Result = {
    NotFound(Json.obj(
      "code"    -> "TICKET_NOT_FOUND",
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
