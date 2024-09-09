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

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.NoMaterializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.MigratedUser
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class MigratedUserRepositoryISpec extends AsyncHmrcSpec
    with GuiceOneAppPerSuite
    with DefaultPlayMongoRepositorySupport[MigratedUser]
    with FixedClock {

  implicit val materializer: Materializer = NoMaterializer

  private val migratedUserRepository                                   = new MigratedUserRepository(mongoComponent, FixedClock.clock)
  override protected val repository: PlayMongoRepository[MigratedUser] = migratedUserRepository

  "MigratedUserRepository" when {
    "saveMigratedUser" should {
      "save the migrated user successfully" in {
        val migratedUser = MigratedUser(LaxEmailAddress("newUser@compny.com"), UserId.random, Instant.now(clock))
        val result       = await(migratedUserRepository.saveMigratedUser(migratedUser))
        result shouldBe ()
      }

      "not save duplicates" in {
        val migratedUser = MigratedUser(LaxEmailAddress("newUser@compny.com"), UserId.random, Instant.now(clock))
        await(migratedUserRepository.saveMigratedUser(migratedUser))
        val result       = await(migratedUserRepository.saveMigratedUser(migratedUser))
        result shouldBe ()
      }
    }

    "findByUserId" should {
      "findUser when exists in db" in {
        val migratedUser = MigratedUser(LaxEmailAddress("newUser@compny.com"), UserId.random, Instant.now(clock))
        await(migratedUserRepository.saveMigratedUser(migratedUser))

        await(migratedUserRepository.userExists(migratedUser.userId)) shouldBe true
      }

      "not find user not in db" in {
        await(migratedUserRepository.userExists(UserId.random)) shouldBe false
      }
    }
  }
}
