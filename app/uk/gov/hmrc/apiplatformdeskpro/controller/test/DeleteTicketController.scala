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

package uk.gov.hmrc.apiplatformdeskpro.controller.test

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.apiplatformdeskpro.controller.JsonUtils
import uk.gov.hmrc.apiplatformdeskpro.domain.models._
import uk.gov.hmrc.apiplatformdeskpro.service.TicketService
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class DeleteTicketController @Inject() (ticketService: TicketService, cc: ControllerComponents, auth: BackendAuthComponents)(implicit val ec: ExecutionContext)
    extends BackendController(cc)
    with ApplicationLogger with JsonUtils {

  def deleteTicket(ticketId: Int): Action[AnyContent] =
    auth.authorizedAction(predicate = Predicate.Permission(Resource.from("api-platform-deskpro", "tickets/all"), IAAction("WRITE"))).async {
      implicit request: AuthenticatedRequest[AnyContent, Unit] =>
        ticketService.deleteTicket(ticketId)
          .map {
            case DeskproTicketUpdateSuccess  => Ok
            case DeskproTicketUpdateNotFound => NotFound
            case DeskproTicketUpdateFailure  => InternalServerError
          }
    }
}
