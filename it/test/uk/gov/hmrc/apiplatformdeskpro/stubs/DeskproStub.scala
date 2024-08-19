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

package uk.gov.hmrc.apiplatformdeskpro.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import play.api.http.Status._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.DeskproTicket
import uk.gov.hmrc.apiplatformdeskpro.domain.models.DeskproPerson

trait DeskproStub {

  object CreateTicket {

    def stubSuccess(deskproTicket: DeskproTicket) = {
      stubFor(
        post(urlMatching("/api/v2/tickets"))
          .withRequestBody(equalTo(Json.toJson(deskproTicket).toString))
          .willReturn(
            aResponse()
              .withBody("""{"data":{"id":12345,"ref":"SDST-1234"}}""")
              .withStatus(CREATED)
          )
      )
    }

    def stubUnauthorised() = {
      stubFor(
        post(urlMatching("/api/v2/tickets"))
          .willReturn(
            aResponse()
              .withStatus(UNAUTHORIZED)
          )
      )
    }

    def stubInternalServerError() = {
      stubFor(
        post(urlMatching("/api/v2/tickets"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
    }
  }
  
  object CreatePerson {

    def stubSuccess(deskproPerson: DeskproPerson) = {
      stubFor(
        post(urlMatching("/api/v2/people"))
          .withRequestBody(equalTo(Json.toJson(deskproPerson).toString))
          .willReturn(
            aResponse()
              .withBody("""{
                          |  "data": {
                          |    "id": 82,
                          |    "primary_email": "dave@test.com",
                          |    "first_name": "dave",
                          |    "last_name": "testerson",
                          |    "title_prefix": "",
                          |    "name": "dave testerson",
                          |    "display_name": "dave testerson",
                          |    "is_agent": false,
                          |    "avatar": {
                          |      "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
                          |      "url_pattern": null,
                          |      "base_gravatar_url": null
                          |    },
                          |    "online": false,
                          |    "online_for_chat": false,
                          |    "last_seen": null,
                          |    "agent_data": null,
                          |    "is_user": false,
                          |    "was_agent": false,
                          |    "can_agent": false,
                          |    "can_admin": false,
                          |    "can_billing": false,
                          |    "can_reports": false,
                          |    "picture_blob": null,
                          |    "disable_picture": false,
                          |    "gravatar_url": "https:\/\/secure.gravatar.com\/avatar\/80d0801f72b3f4d774f15f7115807878?&d=mm",
                          |    "is_contact": true,
                          |    "disable_autoresponses": false,
                          |    "disable_autoresponses_log": "",
                          |    "is_confirmed": false,
                          |    "is_deleted": false,
                          |    "is_disabled": false,
                          |    "creation_system": "web.person",
                          |    "override_display_name": "",
                          |    "display_contact": "dave testerson <dave@test.com>",
                          |    "summary": "",
                          |    "language": 1,
                          |    "organization": null,
                          |    "organization_members": [],
                          |    "organization_position": "",
                          |    "organization_manager": false,
                          |    "timezone": "UTC",
                          |    "date_created": "2024-08-16T14:04:18+0000",
                          |    "date_last_login": null,
                          |    "browser": null,
                          |    "all_user_groups": [
                          |      1
                          |    ],
                          |    "user_groups": [],
                          |    "agent_groups": [],
                          |    "labels": [],
                          |    "emails": [
                          |      "dave@test.com"
                          |    ],
                          |    "phone_numbers": [],
                          |    "tickets_count": 0,
                          |    "chats_count": 0,
                          |    "fields": {},
                          |    "contextual_options": [],
                          |    "contact_data": [],
                          |    "teams": [],
                          |    "primary_team": null,
                          |    "brands": [
                          |      3
                          |    ],
                          |    "preferences": []
                          |  },
                          |  "meta": {},
                          |  "linked": {}
                          |}""".stripMargin)
              .withStatus(CREATED)
          )
      )
    }

    def stubUnauthorised() = {
      stubFor(
        post(urlMatching("/api/v2/people"))
          .willReturn(
            aResponse()
              .withStatus(UNAUTHORIZED)
          )
      )
    }

    def stubInternalServerError() = {
      stubFor(
        post(urlMatching("/api/v2/people"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
    }
    def stubDupeEmailError() = {
      stubFor(
        post(urlMatching("/api/v2/people"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody(    """{
                              |  "status": 400,
                              |  "code": "invalid_input",
                              |  "message": "Request input is invalid.",
                              |  "errors": {
                              |    "fields": {
                              |      "emails": {
                              |        "fields": {
                              |          "emails_0": {
                              |            "errors": [
                              |              {
                              |                "code": "dupe_email",
                              |                "message": "Email \"dave@test.com\" is already in use by other user."
                              |              }
                              |            ]
                              |          }
                              |        }
                              |      }
                              |    }
                              |  }
                              |}""".stripMargin)
          )
      )
    }
  }
}
