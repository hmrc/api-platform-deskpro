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

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.UpscanCallbackBody
import uk.gov.hmrc.apiplatformdeskpro.service.UpscanCallbackDispatcher
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

@Singleton
class UpscanCallbackController @Inject() (
    upscanCallbackDispatcher: UpscanCallbackDispatcher,
    cc: ControllerComponents,
    auth: BackendAuthComponents
  )(implicit val ec: ExecutionContext
  ) extends BackendController(cc)
    with ApplicationLogger with JsonUtils {

  def callback(): Action[AnyContent] = Action.async { implicit request =>
    withJsonBodyFromAnyContent[UpscanCallbackBody] { parsedRequest =>
      upscanCallbackDispatcher.handleCallback(parsedRequest).map(_ => Ok)
    }
  }
}
