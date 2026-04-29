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

package uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo

import java.net.URL
import java.time.Instant

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.apiplatformdeskpro.utils.HttpUrlFormat
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

case class ReadyFile(fileReference: String, name: String, mimeType: String, downloadUrl: URL, size: Long, createdAt: Instant)

object ReadyFile {
  implicit val urlFormat: Format[URL]         = HttpUrlFormat.format
  implicit val formatInstant: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val format: OFormat[ReadyFile]     = Json.format[ReadyFile]
}
