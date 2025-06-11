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
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.DeskproPersonCache
import uk.gov.hmrc.apiplatformdeskpro.utils.ApplicationLogger
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

object DeskproPersonCacheRepository {
  import play.api.libs.json._
  implicit val dateFormat: Format[Instant]         = MongoJavatimeFormats.instantFormat
  implicit val format: OFormat[DeskproPersonCache] = Json.format[DeskproPersonCache]
}

@Singleton
class DeskproPersonCacheRepository @Inject() (mongo: MongoComponent, appConfig: AppConfig)(implicit val ec: ExecutionContext)
    extends PlayMongoRepository[DeskproPersonCache](
      collectionName = "deskproPersonCache",
      mongoComponent = mongo,
      domainFormat = DeskproPersonCacheRepository.format,
      indexes = Seq(
        IndexModel(
          ascending("emailAddress"),
          IndexOptions()
            .name("emailAddressIndex")
            .unique(true)
            .background(true)
        ),
        IndexModel(
          ascending("personId"),
          IndexOptions()
            .name("personIdIndex")
            .unique(true)
            .background(true)
        ),
        IndexModel(
          ascending("createdAt"),
          IndexOptions().name("createdAt_ttl_idx")
            .background(true)
            .expireAfter(appConfig.deskproPersonCacheTimeout.toSeconds, TimeUnit.SECONDS)
        )
      ),
      replaceIndexes = true
    )
    with ApplicationLogger
    with MongoJavatimeFormats.Implicits {

  override lazy val requiresTtlIndex: Boolean = true

  def saveDeskproPersonCache(person: DeskproPersonCache): Future[Option[DeskproPersonCache]] = {
    collection.find(equal("emailAddress", Codecs.toBson(person.emailAddress))).headOption().flatMap {
      case Some(value) => {
        logger.info(s"Cannot create deskpro person cache for person with email ${person.emailAddress.text} because a person already exists.")
        Future.successful(None)
      }
      case None        => {
        collection.insertOne(person).toFuture().map(_ => Some(person))
      }
    }
  }

  def fetchByEmailAddress(email: LaxEmailAddress): Future[Option[DeskproPersonCache]] = {
    collection.find(equal("emailAddress", email.toString)).headOption()
  }
}
