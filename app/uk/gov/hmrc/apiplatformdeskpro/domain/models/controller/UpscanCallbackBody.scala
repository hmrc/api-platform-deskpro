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

package uk.gov.hmrc.apiplatformdeskpro.domain.models.controller

import java.net.URL
import java.time.Instant

import play.api.libs.json.{Format, JsError, Reads, _}
import uk.gov.hmrc.apiplatformdeskpro.utils.HttpUrlFormat

sealed trait UpscanCallbackBody {
  def reference: Reference
}

case class ReadyCallbackBody(
    reference: Reference,
    downloadUrl: URL,
    uploadDetails: UploadDetails
  ) extends UpscanCallbackBody

case class FailedCallbackBody(
    reference: Reference,
    failureDetails: ErrorDetails
  ) extends UpscanCallbackBody

object UpscanCallbackBody {

  implicit val uploadDetailsFormat: Reads[UploadDetails] = Json.reads[UploadDetails]
  implicit val errorDetailsFormat: Reads[ErrorDetails]   = Json.reads[ErrorDetails]

  implicit val readyCallbackBodyFormat: Reads[ReadyCallbackBody] = {
    implicit val urlFormat: Format[URL] = HttpUrlFormat.format
    Json.reads[ReadyCallbackBody]
  }

  implicit val failedCallbackBodyFormat: Reads[FailedCallbackBody] = Json.reads[FailedCallbackBody]

  implicit val callbackBodyFormat: Reads[UpscanCallbackBody] =
    (json: JsValue) =>
      json \ "fileStatus" match {
        case JsDefined(JsString("READY"))  => json.validate[ReadyCallbackBody]
        case JsDefined(JsString("FAILED")) => json.validate[FailedCallbackBody]
        case JsDefined(value)              => JsError(s"Invalid type discriminator: $value")
        case _                             => JsError(s"Missing type discriminator")
      }
}

case class UploadDetails(
    uploadTimestamp: Instant,
    checksum: String,
    fileMimeType: String,
    fileName: String,
    size: Long
  )

case class ErrorDetails(
    failureReason: String,
    message: String
  )

case class Reference(value: String) extends AnyVal

object Reference {
  implicit val referenceFormat: Reads[Reference] = Reads.StringReads.map(Reference(_))
}
