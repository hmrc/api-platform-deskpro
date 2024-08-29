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

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future, blocking}

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.connector.{DeskproConnector, DeveloperConnector}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.RegisteredUser
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.MigratedUser
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproPersonExistsInDb, DeskproPersonExistsInDeskpro, DeskproPersonCreationResult, DeskproPersonCreationSuccess}
import uk.gov.hmrc.apiplatformdeskpro.repository.MigratedUserRepository
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

@Singleton
class CreatePersonService @Inject() (
    migratedUserRepository: MigratedUserRepository,
    deskproConnector: DeskproConnector,
    developerConnector: DeveloperConnector,
    config: AppConfig,
    val clock: Clock
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger with ClockNow {

  // Not required currently. Retain for future reference
  //
  final def batchFutures[I](batchSize: Int, batchPause: Int, fn: I => Future[(UserId, DeskproPersonCreationResult)])(input: Seq[I])(implicit ec: ExecutionContext): Future[Unit] = {
    input.splitAt(batchSize) match {
      case (Nil, Nil)                       => Future.successful(())
      case (doNow: Seq[I], doLater: Seq[I]) =>
        Future.sequence(doNow.map(fn)).flatMap(_ =>
          Future(
            blocking({
              logger.info(s"batchsize is:$batchSize pausing batch for:$batchPause processing ${doNow.size} , ${doLater.size} still to process")
              Thread.sleep(batchPause)
            })
          )
            .flatMap(_ => batchFutures(batchSize, batchPause, fn)(doLater))
        )
    }
  }

  def pushNewUsersToDeskpro()(implicit ec: ExecutionContext): Future[Unit] = {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    def pushUserToDeskpro(u: RegisteredUser)(implicit ec: ExecutionContext): Future[(UserId, DeskproPersonCreationResult)] = {

      def handleDeskproResult(result: DeskproPersonCreationResult) = {
        result match {
          case DeskproPersonCreationSuccess   =>
            logger.info(s"User ${u.userId} sent to DeskPro successfully")
            migratedUserRepository.saveMigratedUser(MigratedUser(u.email, u.userId, Instant.now(clock)))
          case DeskproPersonExistsInDeskpro =>
            logger.warn(s"User ${u.userId} already existed in Deskpro")
            migratedUserRepository.saveMigratedUser(MigratedUser(u.email, u.userId, Instant.now(clock)))
          case _                              => Future.successful(())
        }
      }

      for {
        maybeMigratedUser <- migratedUserRepository.findByUserId(u.userId)
        deskproResult     <-
          maybeMigratedUser.fold(deskproConnector.createPerson(u.userId, s"${u.firstName} ${u.lastName}", u.email.text))(_ => Future.successful(DeskproPersonExistsInDb))
        _                 <- handleDeskproResult(deskproResult)
      } yield (u.userId, deskproResult)

    }

    for {
      users   <- developerConnector.searchDevelopers()
      results <- batchFutures(config.deskproBatchSize, config.deskproBatchPause, pushUserToDeskpro)(users)
    } yield results

  }

}
