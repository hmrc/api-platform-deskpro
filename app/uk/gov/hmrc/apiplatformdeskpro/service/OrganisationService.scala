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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.DeskproPersonResponse
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproOrganisation, DeskproPerson, OrganisationId}
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

@Singleton
class OrganisationService @Inject() (deskproConnector: DeskproConnector)(implicit val ec: ExecutionContext) extends ApplicationLogger {

  def getOrganisationById(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[DeskproOrganisation] = {

    for {
      orgResponse    <- deskproConnector.getOrganisationById(organisationId)
      org             = orgResponse.data
      peopleResponse <- getAllPeople(organisationId)
      people          = peopleResponse.flatMap(per => per.primary_email.map(email => DeskproPerson(per.name, email)))
    } yield DeskproOrganisation(organisationId, org.name, people)
  }

  private def getAllPeople(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[List[DeskproPersonResponse]] = {
    deskproConnector.getPeopleByOrganisationId(organisationId).flatMap(peopleResponse =>
      if (peopleResponse.meta.pagination.totalPages > 1) {
        Future.sequence(
          (2 to peopleResponse.meta.pagination.totalPages)
            .map(pageWanted => deskproConnector.getPeopleByOrganisationId(organisationId, pageWanted)).toList
        ).map(listOfResponses => listOfResponses.flatMap(_.data)).map(_.concat(peopleResponse.data).distinct)
      } else Future.successful(peopleResponse.data.distinct)
    )
  }

  def getOrganisationsByEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[List[DeskproOrganisation]] = {
    deskproConnector.getOrganisationsForPersonEmail(email)
      .map { organisationWrapper =>
        organisationWrapper.linked.organisations.values.map { org =>
          DeskproOrganisation(OrganisationId(org.id.toString), org.name, List.empty)
        }.toList
      }
  }
}
