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

package uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo

import java.net.URL
import java.time.Instant

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.apiplatformdeskpro.utils.HttpUrlFormat
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

sealed trait UploadStatus

object UploadStatus {

  case class InProgress(
      percent: Option[Int]
    ) extends UploadStatus

  case class Failed(
      message: String,
      reason: String
    ) extends UploadStatus

  case class UploadedSuccessfully(
      name: String,
      mimeType: String,
      downloadUrl: URL,
      size: Long
    ) extends UploadStatus

  import uk.gov.hmrc.play.json.Union

  implicit val urlFormat: Format[URL] = HttpUrlFormat.format

  private implicit val formatInProgress: OFormat[InProgress]                     = Json.format[InProgress]
  private implicit val formatFailed: OFormat[Failed]                             = Json.format[Failed]
  private implicit val formatUploadedSuccessfully: OFormat[UploadedSuccessfully] = Json.format[UploadedSuccessfully]

  implicit val format: OFormat[UploadStatus] = Union.from[UploadStatus]("uploadStatus")
    .and[InProgress]("InProgress")
    .and[Failed]("Failed")
    .and[UploadedSuccessfully]("UploadedSuccessfully")
    .format
}

case class UploadedFile(fileReference: String, uploadStatus: UploadStatus, createdAt: Instant)

object UploadedFile {
  implicit val formatInstant: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit val format: OFormat[UploadedFile]  = Json.format[UploadedFile]
}
