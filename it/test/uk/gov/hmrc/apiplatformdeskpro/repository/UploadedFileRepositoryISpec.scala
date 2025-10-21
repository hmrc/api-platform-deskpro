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

import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{BlobDetails, UploadStatus, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class UploadedFileRepositoryISpec extends AsyncHmrcSpec
    with GuiceOneAppPerSuite
    with DefaultPlayMongoRepositorySupport[UploadedFile]
    with FixedClock {

  implicit val materializer: Materializer = NoMaterializer

  private val uploadedFileRepo                                         = new UploadedFileRepository(mongoComponent, FixedClock.clock)
  override protected val repository: PlayMongoRepository[UploadedFile] = uploadedFileRepo

  trait Setup {
    val fileReference = "fileRef"
    val uploadStatus  = UploadStatus.UploadedSuccessfully("name", "text/plain", new URL("https://example/com/callback"), 100, BlobDetails(1234, "auth"))
    val messageId     = 789
  }

  "UploadedFileRepository" when {
    "create" should {
      "save the UploadedFile successfully" in new Setup {
        val uploadedFile = UploadedFile(fileReference, uploadStatus, instant)
        val result       = await(uploadedFileRepo.create(uploadedFile))
        result shouldBe uploadedFile
      }

      "replace existing in event of duplicates" in new Setup {
        val uploadedFile = UploadedFile(fileReference, uploadStatus, instant)
        await(uploadedFileRepo.create(uploadedFile))

        val result = await(uploadedFileRepo.create(uploadedFile))
        result shouldBe uploadedFile
      }
    }

    "fetchByFileReference" should {
      "find UploadedFile when exists in db" in new Setup {
        val uploadedFile = UploadedFile(fileReference, uploadStatus, instant)
        await(uploadedFileRepo.create(uploadedFile))

        await(uploadedFileRepo.fetchByFileReference(fileReference)) shouldBe Some(uploadedFile)
      }

      "not find UploadedFile not in db" in new Setup {
        await(uploadedFileRepo.fetchByFileReference(fileReference)) shouldBe None
      }
    }
  }
}
