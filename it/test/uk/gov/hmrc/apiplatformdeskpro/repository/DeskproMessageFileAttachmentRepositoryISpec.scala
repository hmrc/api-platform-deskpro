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

import scala.concurrent.ExecutionContext.Implicits.global

import com.mongodb.MongoWriteException
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.NoMaterializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.DeskproMessageFileAttachment
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class DeskproMessageFileAttachmentRepositoryISpec extends AsyncHmrcSpec
    with GuiceOneAppPerSuite
    with DefaultPlayMongoRepositorySupport[DeskproMessageFileAttachment]
    with FixedClock {

  implicit val materializer: Materializer = NoMaterializer

  private val messageFileAttachmentRepo                                                = new DeskproMessageFileAttachmentRepository(mongoComponent, FixedClock.clock)
  override protected val repository: PlayMongoRepository[DeskproMessageFileAttachment] = messageFileAttachmentRepo

  trait Setup {
    val fileReference = "fileRef"
    val ticketId      = 3281
    val messageId     = 789
  }

  "DeskproMessageFileAttachmentRepository" when {
    "create" should {
      "save the DeskproMessageFileAttachment successfully" in new Setup {
        val response = DeskproMessageFileAttachment(ticketId, messageId, fileReference, instant)
        val result   = await(messageFileAttachmentRepo.create(response))
        result shouldBe response
      }

      "not save duplicates" in new Setup {
        val response = DeskproMessageFileAttachment(ticketId, messageId, fileReference, instant)
        await(messageFileAttachmentRepo.create(response))

        intercept[MongoWriteException] {
          await(messageFileAttachmentRepo.create(response))
        }
      }
    }

    "fetchByFileReference" should {
      "find DeskproMessageFileAttachment when exists in db" in new Setup {
        val response = DeskproMessageFileAttachment(ticketId, messageId, fileReference, instant)
        await(messageFileAttachmentRepo.create(response))

        await(messageFileAttachmentRepo.fetchByFileReference(fileReference)) shouldBe Some(response)
      }

      "not find DeskproMessageFileAttachment not in db" in new Setup {
        await(messageFileAttachmentRepo.fetchByFileReference(fileReference)) shouldBe None
      }
    }
  }
}
