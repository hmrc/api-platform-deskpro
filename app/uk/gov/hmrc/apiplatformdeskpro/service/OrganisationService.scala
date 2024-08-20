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

import uk.gov.hmrc.apiplatformdeskpro.connector.DeskproConnector
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.DeskproResponse
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproOrganisation, OrganisationId}
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class OrganisationService @Inject() (deskproConnector: DeskproConnector)(implicit val ec: ExecutionContext) extends ApplicationLogger {

  def getOrganisationById(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[DeskproOrganisation] = {
    deskproConnector.getOrganisationById(organisationId)
      .map(DeskproResponse.toDeskproOrganisation)
  }

}
