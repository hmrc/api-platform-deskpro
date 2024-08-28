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

package uk.gov.hmrc.apiplatformdeskpro.service

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.connector.{DeskproConnector, DeveloperConnector}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.DeskproPersonCreationResult
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.RegisteredUser
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import scala.util.control.NonFatal

@Singleton
class CreatePersonService @Inject() (deskproConnector: DeskproConnector, developerConnector: DeveloperConnector, config: AppConfig)(implicit val ec: ExecutionContext)
    extends ApplicationLogger {


  // Not required currently. Retain for future reference
  //
  // final def batchFutures[I](batchSize: Int, batchPause: Long, fn: I => Future[Unit])(input: Seq[I])(implicit ec: ExecutionContext): Future[Unit] = {
  //   input.splitAt(batchSize) match {
  //     case (Nil, Nil) => Future.successful(())
  //     case (doNow: Seq[I], doLater: Seq[I]) =>
  //       Future.sequence(doNow.map(fn)).flatMap( _ =>
  //         Future(
  //           blocking({logDebug("Done batch of items"); Thread.sleep(batchPause)})
  //         )
  //         .flatMap(_ => batchFutures(batchSize, batchPause, fn)(doLater))
  //       )
  //   }
  // }

  def functionToExecute()(implicit ec: ExecutionContext): Future[List[DeskproPersonCreationResult]] = {

    def pushUserToDeskpro(u: RegisteredUser)(implicit ec: ExecutionContext) ={
      deskproConnector.createPerson(u.userId, s"${u.firstName} ${u.lastName}", u.email.text)
    }

    implicit val hc: HeaderCarrier = HeaderCarrier()

    def executeBatch[A](list: List[Future[A]])(concurFactor: Int): Future[List[A]] = {
      list.grouped(concurFactor).foldLeft(Future.successful(List.empty[A])) { (r: Future[List[A]], c: List[Future[A]]) =>
        val batch = Future.sequence(c)
        r.flatMap(rs => r.map(values => rs ++ values))
      }
    }

    for {
      users   <- developerConnector.searchDevelopers()
      results <- executeBatch(users.map(pushUserToDeskpro))(100)
    } yield results
//    developerConnector
//      .searchDevelopers()
//      .flatMap(users => Future.sequence())
  }

}
