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

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatformdeskpro.connector.DeskproConnector
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.DeskproPersonCache
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproPersonNotFound, DeskproPersonUpdateResult}
import uk.gov.hmrc.apiplatformdeskpro.repository.DeskproPersonCacheRepository
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

@Singleton
class PersonService @Inject() (
    deskproConnector: DeskproConnector,
    deskproPersonCacheRepository: DeskproPersonCacheRepository,
    val clock: Clock
  )(implicit val ec: ExecutionContext
  ) extends ApplicationLogger with ClockNow {

  def updatePersonByEmail(email: LaxEmailAddress, name: String)(implicit hc: HeaderCarrier): Future[DeskproPersonUpdateResult] = {
    for {
      personId <- getPersonIdForEmail(email)
      result   <- deskproConnector.updatePerson(personId, name)
    } yield result
  }

  def markPersonInactive(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[DeskproPersonUpdateResult] = {
    for {
      personId <- getPersonIdForEmail(email)
      result   <- deskproConnector.markPersonInactive(personId)
    } yield result
  }

  private def fetchPersonIdFromDeskproAndCache(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[Int] = {
    for {
      response <- deskproConnector.getPersonForEmail(email)
      personId  = response.data.headOption.getOrElse(throw new DeskproPersonNotFound("Person not found")).id
      _        <- deskproPersonCacheRepository.saveDeskproPersonCache(DeskproPersonCache(email, personId, instant()))
    } yield personId
  }

  def getPersonIdForEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[Int] = {
    for {
      maybeCachePerson <- deskproPersonCacheRepository.fetchByEmailAddress(email)
      personId         <- maybeCachePerson match {
                            case Some(person) => Future.successful(person.personId)
                            case _            => fetchPersonIdFromDeskproAndCache(email)
                          }
    } yield personId

  }
}
