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
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.http.Status._
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}
import uk.gov.hmrc.apiplatformdeskpro.stubs.DeskproStub
import uk.gov.hmrc.apiplatformdeskpro.utils.{AsyncHmrcSpec, ConfigBuilder, WireMockSupport}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class UpscanDownloadConnectorISpec
    extends AsyncHmrcSpec
    with WireMockSupport
    with GuiceOneServerPerSuite
    with ConfigBuilder
    with FixedClock {

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig(wireMockPort))
      .in(Mode.Test)
      .overrides(bind[Clock].toInstance(FixedClock.clock))
      .build()

  implicit val hc: HeaderCarrier = HeaderCarrier()

  trait Setup extends DeskproStub {

    val objInTest: UpscanDownloadConnector  = app.injector.instanceOf[UpscanDownloadConnector]
    implicit val materializer: Materializer = app.injector.instanceOf[Materializer]

  }

  "upscanDownloadConnector" when {
    "stream" should {
      "return contents of file" in new Setup {
        val downloadUrl  = "/download"
        val responseBody = "file content"

        stubFor(
          get(urlPathEqualTo(downloadUrl))
            .willReturn(
              aResponse()
                .withBody(responseBody)
                .withStatus(OK)
            )
        )

        val result = await(objInTest.stream(new URL(s"$wireMockBaseUrlAsString$downloadUrl")).flatMap(_.runWith(Sink.seq)))
        result.map(_.utf8String).mkString("") shouldBe responseBody
      }
    }
  }
}
