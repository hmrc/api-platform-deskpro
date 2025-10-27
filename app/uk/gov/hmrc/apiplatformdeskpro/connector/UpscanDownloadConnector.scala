/*
 * Copyright 2025 HM Revenue & Customs
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

import java.net.URL
import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

class UpscanDownloadConnector @Inject() (http: HttpClientV2, val clock: Clock)(implicit materializer: Materializer, val ec: ExecutionContext)
    extends ApplicationLogger with ClockNow {

  def stream(downloadUrl: URL)(implicit hc: HeaderCarrier): Future[Source[ByteString, ?]] =
    http
      .get(downloadUrl)
      .stream[Source[ByteString, ?]]
}
