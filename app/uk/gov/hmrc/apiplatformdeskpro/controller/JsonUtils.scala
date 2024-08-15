/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import play.api.libs.json.{JsError, JsSuccess, JsValue, Reads}
import play.api.mvc.{AnyContent, Request, Result, Results}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

trait JsonUtils extends Results {
  self: BackendController =>

  def withJsonBodyFromAnyContent[T](f: T => Future[Result])(implicit request: Request[AnyContent], reads: Reads[T], d: DummyImplicit): Future[Result] = {
    request.body.asJson match {
      case Some(json) => withJson(json)(f)
      case _          => Future.successful(BadRequest("Invalid payload"))
    }
  }

  private def withJson[T](json: JsValue)(f: T => Future[Result])(implicit reads: Reads[T]): Future[Result] = {
    Try(json.validate[T]) match {
      case Success(JsSuccess(payload, _)) => f(payload)
      case Success(JsError(errs))         => Future.successful(BadRequest("Invalid payload: " + JsError.toJson(errs)))
      // $COVERAGE-OFF$ We wont be able to reach here as play json parser will scoop up any non json errors
      case Failure(e)                     => Future.successful(BadRequest("Invalid payload: " + e.getMessage))
      // $COVERAGE-ON$
    }
  }
}
