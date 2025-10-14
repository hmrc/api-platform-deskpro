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

import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.TicketStatus
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.DeskproResponse
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class DeskproResponseRepositoryISpec extends AsyncHmrcSpec
    with GuiceOneAppPerSuite
    with DefaultPlayMongoRepositorySupport[DeskproResponse]
    with FixedClock {

  implicit val materializer: Materializer = NoMaterializer

  private val deskproResponseRepository                                   = new DeskproResponseRepository(mongoComponent, FixedClock.clock)
  override protected val repository: PlayMongoRepository[DeskproResponse] = deskproResponseRepository

  trait Setup {
    val fileReference = "fileRef"
    val ticketId      = 3281
    val email         = LaxEmailAddress("bob@example.com")
    val message       = "message"
  }

  "DeskproResponseRepository" when {
    "saveResponse" should {
      "save the response successfully" in new Setup {
        val response = DeskproResponse(fileReference, ticketId, email, message, TicketStatus.AwaitingAgent)
        val result   = await(deskproResponseRepository.create(response))
        result shouldBe response
      }

      "not save duplicates" in new Setup {
        val response = DeskproResponse(fileReference, ticketId, email, message, TicketStatus.AwaitingAgent)
        await(deskproResponseRepository.create(response))

        intercept[MongoWriteException] {
          await(deskproResponseRepository.create(response))
        }
      }
    }

    "fetchByFileReference" should {
      "find response when exists in db" in new Setup {
        val response = DeskproResponse(fileReference, ticketId, email, message, TicketStatus.AwaitingAgent)
        await(deskproResponseRepository.create(response))

        await(deskproResponseRepository.fetchByFileReference(fileReference)) shouldBe Some(response)
      }

      "not find response not in db" in new Setup {
        await(deskproResponseRepository.fetchByFileReference(fileReference)) shouldBe None
      }
    }
  }
}
