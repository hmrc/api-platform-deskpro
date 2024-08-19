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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.connector.{DeskproConnector, DeveloperConnector}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.DeskproPersonCreationResult
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

class CreatePersonService @Inject() (deskproConnector: DeskproConnector, developerConnector: DeveloperConnector, config: AppConfig)(implicit val ec: ExecutionContext)
    extends ApplicationLogger {

  def pushNewUsersToDeskpro(): Future[List[DeskproPersonCreationResult]] = {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    developerConnector
      .searchDevelopers()
      .flatMap(users => Future.sequence(users.map(u => deskproConnector.createPerson(u.userId, s"${u.firstName} ${u.lastName}", u.email.text))))
  }

}
