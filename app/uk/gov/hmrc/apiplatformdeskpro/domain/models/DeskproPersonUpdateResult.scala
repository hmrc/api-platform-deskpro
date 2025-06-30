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

package uk.gov.hmrc.apiplatformdeskpro.domain.models

sealed trait DeskproPersonUpdateResult
object DeskproPersonUpdateSuccess extends DeskproPersonUpdateResult
object DeskproPersonUpdateFailure extends DeskproPersonUpdateResult

sealed trait DeskproTicketUpdateResult
object DeskproTicketUpdateSuccess  extends DeskproTicketUpdateResult
object DeskproTicketUpdateNotFound extends DeskproTicketUpdateResult
object DeskproTicketUpdateFailure  extends DeskproTicketUpdateResult

sealed trait DeskproTicketResponseResult
object DeskproTicketResponseSuccess  extends DeskproTicketResponseResult
object DeskproTicketResponseNotFound extends DeskproTicketResponseResult
object DeskproTicketResponseFailure  extends DeskproTicketResponseResult

case class DeskproPersonNotFound(
    message: String
  ) extends Exception(message)
