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

package uk.gov.hmrc.apiplatformdeskpro.repository

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.NoMaterializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{BlobDetails, ReadyFile, UploadStatus}
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class ReadyFileRepositoryISpec extends AsyncHmrcSpec
    with GuiceOneAppPerSuite
    with DefaultPlayMongoRepositorySupport[ReadyFile]
    with FixedClock {

  implicit val materializer: Materializer = NoMaterializer

  private val readyFileRepo                                         = new ReadyFileRepository(mongoComponent, FixedClock.clock)
  override protected val repository: PlayMongoRepository[ReadyFile] = readyFileRepo

  trait Setup {
    val fileReference = "fileRef"
    val uploadStatus  = UploadStatus.UploadedSuccessfully("name", "text/plain", new URL("https://example/com/callback"), 100, BlobDetails(1234, "auth"))
    val messageId     = 789
  }

  "ReadyFileRepository" when {
    "create" should {
      "save the ReadyFile successfully" in new Setup {
        val readyFile = ReadyFile(fileReference, "name", "text/plain", new URL("https://example/com/callback"), 100, instant)
        val result    = await(readyFileRepo.create(readyFile))
        result shouldBe readyFile
      }

      "replace existing in event of duplicates" in new Setup {
        val readyFile = ReadyFile(fileReference, "name", "text/plain", new URL("https://example/com/callback"), 100, instant)
        await(readyFileRepo.create(readyFile))

        val result = await(readyFileRepo.create(readyFile))
        result shouldBe readyFile
      }
    }

    "fetchByFileReference" should {
      "find ReadyFile when exists in db" in new Setup {
        val readyFile = ReadyFile(fileReference, "name", "text/plain", new URL("https://example/com/callback"), 100, instant)
        await(readyFileRepo.create(readyFile))

        await(readyFileRepo.fetchByFileReference(fileReference)) shouldBe Some(readyFile)
      }

      "not find ReadyFile not in db" in new Setup {
        await(readyFileRepo.fetchByFileReference(fileReference)) shouldBe None
      }
    }
  }
}
