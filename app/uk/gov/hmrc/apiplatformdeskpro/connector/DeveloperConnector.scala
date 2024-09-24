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

package uk.gov.hmrc.apiplatformdeskpro.connector

import java.time._
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.RegisteredUser
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

class DeveloperConnector @Inject() (http: HttpClientV2, config: AppConfig, metrics: ConnectorMetrics, val clock: Clock)(implicit val ec: ExecutionContext)
    extends ApplicationLogger with ClockNow {

  lazy val serviceBaseUrl: String = config.thirdPartyDeveloperUrl
  val api                         = API("third-party-developer")

  def searchDevelopers(daysToLookBack: Option[Int] = None)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = metrics.record(api) {
    val queryParams = daysToLookBack match {
      case Some(days) => Seq(
          "status"       -> "VERIFIED",
          "limit"        -> 10000,
          "createdAfter" -> DateTimeFormatter.BASIC_ISO_DATE.format(now().toLocalDate.minusDays(days))
        )
      case None       => Seq(
          "status" -> "VERIFIED"
        )
    }

    http.get(url"${requestUrl("/developers")}?$queryParams")
      .execute[List[RegisteredUser]]
      .map { users =>
        logger.info(s"${users.size} user(s) returned from DeveloperConnector")
        users
      }
  }

  private def requestUrl[B, A](uri: String): String = s"$serviceBaseUrl$uri"
}
