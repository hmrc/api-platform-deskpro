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

package uk.gov.hmrc.apiplatformdeskpro.config

import javax.inject.{Inject, Singleton}

import play.api.Configuration

@Singleton
class AppConfig @Inject() (config: Configuration) {

  val appName: String                = config.get[String]("appName")
  val deskproUrl: String             = config.get[String]("deskpro.uri")
  val deskproApiKey: String          = config.get[String]("deskpro.api-key")
  val deskproBrand: Int              = config.get[Int]("deskpro.brand")
  val deskproOrganisation: String    = config.get[String]("deskpro.organisation")
  val deskproTeamMemberEmail: String = config.get[String]("deskpro.team-member-email")
  val deskproSupportReason: String   = config.get[String]("deskpro.support-reason")
  val deskproApplicationId: String   = config.get[String]("deskpro.application-id")
  val deskproApiName: String         = config.get[String]("deskpro.api-name")
}
