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

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import com.mongodb.MongoWriteException
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions}

import uk.gov.hmrc.apiplatformdeskpro.repository.models.MigratedUser
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow

@Singleton
class MigratedUserRepository @Inject() (mongo: MongoComponent, val clock: Clock)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[MigratedUser](
      collectionName = "migratedUsers",
      mongoComponent = mongo,
      domainFormat = MigratedUser.format,
      indexes = Seq(
        IndexModel(
          ascending("emailAddress"),
          IndexOptions()
            .name("emailAddressIndex")
            .unique(true)
            .background(true)
        ),
        IndexModel(
          ascending("userId"),
          IndexOptions()
            .name("userIdIndex")
            .unique(true)
            .background(true)
        )
      ),
      replaceIndexes = true
    )
    with ClockNow
    with ApplicationLogger
    with MongoJavatimeFormats.Implicits {

  override lazy val requiresTtlIndex: Boolean = false // Entries are managed by scheduled jobs

  def saveMigratedUser(user: MigratedUser): Future[Unit] = {
    collection.insertOne(user).headOption().map(_ => ()) recoverWith {
      case e: MongoWriteException if e.getError.getCode == 11000 =>
        logger.info(s"User ${user.userId} already in db")
        Future.successful(())
    }
  }

  def findByUserId(userId: UserId): Future[Option[MigratedUser]] = {
    collection.find(Filters.eq("userId", Codecs.toBson(userId))).headOption()
  }

}
