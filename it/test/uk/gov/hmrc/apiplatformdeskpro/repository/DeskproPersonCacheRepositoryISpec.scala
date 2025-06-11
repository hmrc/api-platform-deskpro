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

package uk.gov.hmrc.apiplatformdeskpro.repository

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.NoMaterializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.DeskproPersonCache
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class DeskproPersonCacheRepositoryISpec extends AsyncHmrcSpec
    with GuiceOneAppPerSuite
    with DefaultPlayMongoRepositorySupport[DeskproPersonCache]
    with FixedClock {

  implicit val materializer: Materializer = NoMaterializer

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.deskproPersonCacheTimeout).thenReturn(Duration(24, TimeUnit.HOURS))

  private val deskproPersonCacheRepository                                   = new DeskproPersonCacheRepository(mongoComponent, mockAppConfig)
  override protected val repository: PlayMongoRepository[DeskproPersonCache] = deskproPersonCacheRepository

  trait Setup {
    val personId: Int = 232
    val email         = LaxEmailAddress("newUser@company.com")
  }

  "DeskproPersonCacheRepository" when {
    "saveDeskproPersonCache" should {
      "save the person successfully" in new Setup {
        val deskproPerson = DeskproPersonCache(email, personId, instant)
        val result        = await(deskproPersonCacheRepository.saveDeskproPersonCache(deskproPerson))
        result shouldBe Some(deskproPerson)
      }

      "not save duplicates" in new Setup {
        val deskproPerson = DeskproPersonCache(email, personId, instant)
        await(deskproPersonCacheRepository.saveDeskproPersonCache(deskproPerson))
        val result        = await(deskproPersonCacheRepository.saveDeskproPersonCache(deskproPerson))
        result shouldBe None
      }
    }

    "fetchByEmailAddress" should {
      "find person when exists in db" in new Setup {
        val deskproPerson = DeskproPersonCache(email, personId, instant)
        await(deskproPersonCacheRepository.saveDeskproPersonCache(deskproPerson))

        val result = await(deskproPersonCacheRepository.fetchByEmailAddress(email))
        result shouldBe Some(deskproPerson)
      }

      "not find person not in db" in new Setup {
        val result = await(deskproPersonCacheRepository.fetchByEmailAddress(email))
        result shouldBe None
      }
    }
  }
}
