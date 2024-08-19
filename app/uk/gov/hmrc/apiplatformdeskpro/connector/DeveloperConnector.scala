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
import scala.concurrent.ExecutionContext

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.RegisteredUser
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.play.http.metrics.common.API

import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

class DeveloperConnector @Inject() (http: HttpClientV2, config: AppConfig, metrics: ConnectorMetrics, val clock: Clock)(implicit val ec: ExecutionContext)
    extends ApplicationLogger with ClockNow {

  lazy val serviceBaseUrl: String = config.deskproUrl
  val api                         = API("third-party-developer")

  def searchDevelopers()(implicit hc: HeaderCarrier) = metrics.record(api) {
    val queryParams = Seq(
      "status"       -> "VERIFIED",
      "limit"        -> 100,
      "createdAfter" -> DateTimeFormatter.BASIC_ISO_DATE.format(now().toLocalDate().minusDays(config.lookBack))
    )
    http.get(url"${requestUrl("/developers")}?$queryParams").execute[List[RegisteredUser]]
  }

  private def requestUrl[B, A](uri: String): String = s"$serviceBaseUrl$uri"
}
