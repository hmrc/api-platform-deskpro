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
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproPerson, OrganisationId}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.DeskproTicket

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

    def stubBadRequest() = {
      stubFor(
        post(urlMatching("/api/v2/people"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST).withBody("""{
                                                  |  "status": 400,
                                                  |  "code": "invalid_input",
                                                  |  "message": "Request input is invalid.",
                                                  |  "errors": {
                                                  |    "fields": {
                                                  |      "firstName": {
                                                  |        "errors": [
                                                  |            {
                                                  |               "code": "other_error",
                                                  |               "message": "Some other error from deskpro"
                                                  |          }
                                                  |        ]
                                                  |      }
                                                  |    }
                                                  |  }
                                                  |}""".stripMargin)
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
              .withBody("""{
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
  object GetOrganisation {
    def stubSuccess(organisationId: OrganisationId)={
      stubFor(
        get(urlPathEqualTo(s"/api/v2/organizations/$organisationId/members"))
          .withQueryParam("include", equalTo("person,organization"))
          .willReturn(
            aResponse()
              .withBody("""{
                          |  "data": [
                          |    {
                          |      "id": 8,
                          |      "person": 63,
                          |      "organization": 1,
                          |      "is_manager": true,
                          |      "position": "",
                          |      "created_at": "2024-07-23T10:48:10+0000",
                          |      "updated_at": "2024-07-29T12:55:26+0000"
                          |    },
                          |    {
                          |      "id": 5,
                          |      "person": 6,
                          |      "organization": 1,
                          |      "is_manager": true,
                          |      "position": "Software Developer",
                          |      "created_at": "2023-09-19T11:00:15+0000",
                          |      "updated_at": "2023-09-19T11:00:15+0000"
                          |    },
                          |    {
                          |      "id": 4,
                          |      "person": 5,
                          |      "organization": 1,
                          |      "is_manager": true,
                          |      "position": "Software Developer",
                          |      "created_at": "2023-09-19T11:00:15+0000",
                          |      "updated_at": "2023-09-19T11:00:15+0000"
                          |    },
                          |    {
                          |      "id": 3,
                          |      "person": 4,
                          |      "organization": 1,
                          |      "is_manager": false,
                          |      "position": "Director of Software",
                          |      "created_at": "2023-09-19T11:00:15+0000",
                          |      "updated_at": "2023-09-19T11:00:15+0000"
                          |    },
                          |    {
                          |      "id": 2,
                          |      "person": 3,
                          |      "organization": 1,
                          |      "is_manager": false,
                          |      "position": "Product Owner",
                          |      "created_at": "2023-09-19T11:00:15+0000",
                          |      "updated_at": "2023-09-19T11:00:15+0000"
                          |    },
                          |    {
                          |      "id": 1,
                          |      "person": 2,
                          |      "organization": 1,
                          |      "is_manager": false,
                          |      "position": "",
                          |      "created_at": "2023-09-19T11:00:15+0000",
                          |      "updated_at": "2023-09-19T11:00:15+0000"
                          |    }
                          |  ],
                          |  "meta": {
                          |    "pagination": {
                          |      "total": 6,
                          |      "count": 6,
                          |      "per_page": 10,
                          |      "current_page": 1,
                          |      "total_pages": 1
                          |    }
                          |  },
                          |  "linked": {
                          |    "person": {
                          |      "2": {
                          |        "id": 2,
                          |        "primary_email": "deans@123.com",
                          |        "first_name": "Deantest",
                          |        "last_name": "",
                          |        "title_prefix": "",
                          |        "name": "Deantest",
                          |        "display_name": "Deantest",
                          |        "is_agent": false,
                          |        "avatar": {
                          |          "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
                          |          "url_pattern": null,
                          |          "base_gravatar_url": null
                          |        },
                          |        "online": false,
                          |        "online_for_chat": false,
                          |        "last_seen": null,
                          |        "agent_data": null,
                          |        "is_user": false,
                          |        "was_agent": false,
                          |        "can_agent": false,
                          |        "can_admin": false,
                          |        "can_billing": false,
                          |        "can_reports": false,
                          |        "picture_blob": null,
                          |        "disable_picture": false,
                          |        "gravatar_url": "https:\/\/secure.gravatar.com\/avatar\/406a4a012317afb4fdfe379c23bd64eb?&d=mm",
                          |        "is_contact": true,
                          |        "disable_autoresponses": false,
                          |        "disable_autoresponses_log": "",
                          |        "is_confirmed": false,
                          |        "is_deleted": false,
                          |        "is_disabled": false,
                          |        "creation_system": "web.person",
                          |        "override_display_name": "",
                          |        "display_contact": "Deantest <deans@123.com>",
                          |        "summary": "",
                          |        "language": 1,
                          |        "organization": 1,
                          |        "organization_members": [
                          |          1
                          |        ],
                          |        "organization_position": "",
                          |        "organization_manager": false,
                          |        "timezone": "UTC",
                          |        "date_created": "2022-12-12T15:47:22+0000",
                          |        "date_last_login": null,
                          |        "browser": null,
                          |        "all_user_groups": [
                          |          1
                          |        ],
                          |        "user_groups": [],
                          |        "agent_groups": [],
                          |        "labels": [],
                          |        "emails": [
                          |          "deans@123.com"
                          |        ],
                          |        "phone_numbers": [
                          |          {
                          |            "number": "+447984774537",
                          |            "label": null,
                          |            "extension": "",
                          |            "person": 2,
                          |            "type": "person"
                          |          }
                          |        ],
                          |        "tickets_count": 0,
                          |        "chats_count": 0,
                          |        "fields": {},
                          |        "contextual_options": [],
                          |        "contact_data": [],
                          |        "teams": [],
                          |        "primary_team": null,
                          |        "brands": [
                          |          1
                          |        ],
                          |        "preferences": []
                          |      },
                          |      "3": {
                          |        "id": 3,
                          |        "primary_email": null,
                          |        "first_name": "Jeff",
                          |        "last_name": "Smith",
                          |        "title_prefix": "",
                          |        "name": "Jeff Smith",
                          |        "display_name": "Jeff Smith",
                          |        "is_agent": false,
                          |        "avatar": {
                          |          "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
                          |          "url_pattern": null,
                          |          "base_gravatar_url": null
                          |        },
                          |        "online": false,
                          |        "online_for_chat": false,
                          |        "last_seen": null,
                          |        "agent_data": null,
                          |        "is_user": true,
                          |        "was_agent": false,
                          |        "can_agent": false,
                          |        "can_admin": false,
                          |        "can_billing": false,
                          |        "can_reports": false,
                          |        "picture_blob": null,
                          |        "disable_picture": false,
                          |        "gravatar_url": "&d=mm",
                          |        "is_contact": false,
                          |        "disable_autoresponses": false,
                          |        "disable_autoresponses_log": "",
                          |        "is_confirmed": false,
                          |        "is_deleted": false,
                          |        "is_disabled": false,
                          |        "creation_system": "web.person",
                          |        "override_display_name": "",
                          |        "display_contact": "Jeff Smith",
                          |        "summary": "",
                          |        "language": 1,
                          |        "organization": 1,
                          |        "organization_members": [
                          |          2
                          |        ],
                          |        "organization_position": "Product Owner",
                          |        "organization_manager": false,
                          |        "timezone": "UTC",
                          |        "date_created": "2023-01-02T10:48:34+0000",
                          |        "date_last_login": null,
                          |        "browser": null,
                          |        "all_user_groups": [
                          |          1,
                          |          2
                          |        ],
                          |        "user_groups": [],
                          |        "agent_groups": [],
                          |        "labels": [],
                          |        "emails": [],
                          |        "phone_numbers": [
                          |          {
                          |            "number": "+441234564654",
                          |            "label": "PO",
                          |            "extension": "",
                          |            "person": 3,
                          |            "type": "person"
                          |          }
                          |        ],
                          |        "tickets_count": 0,
                          |        "chats_count": 0,
                          |        "fields": {},
                          |        "contextual_options": [],
                          |        "contact_data": [],
                          |        "teams": [],
                          |        "primary_team": null,
                          |        "brands": [
                          |          1
                          |        ],
                          |        "preferences": []
                          |      },
                          |      "4": {
                          |        "id": 4,
                          |        "primary_email": null,
                          |        "first_name": "Mike",
                          |        "last_name": "Jones",
                          |        "title_prefix": "",
                          |        "name": "Mike Jones",
                          |        "display_name": "Mike Jones",
                          |        "is_agent": false,
                          |        "avatar": {
                          |          "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
                          |          "url_pattern": null,
                          |          "base_gravatar_url": null
                          |        },
                          |        "online": false,
                          |        "online_for_chat": false,
                          |        "last_seen": null,
                          |        "agent_data": null,
                          |        "is_user": true,
                          |        "was_agent": false,
                          |        "can_agent": false,
                          |        "can_admin": false,
                          |        "can_billing": false,
                          |        "can_reports": false,
                          |        "picture_blob": null,
                          |        "disable_picture": false,
                          |        "gravatar_url": "&d=mm",
                          |        "is_contact": false,
                          |        "disable_autoresponses": false,
                          |        "disable_autoresponses_log": "",
                          |        "is_confirmed": false,
                          |        "is_deleted": false,
                          |        "is_disabled": false,
                          |        "creation_system": "web.person",
                          |        "override_display_name": "",
                          |        "display_contact": "Mike Jones",
                          |        "summary": "",
                          |        "language": 1,
                          |        "organization": 1,
                          |        "organization_members": [
                          |          3
                          |        ],
                          |        "organization_position": "Director of Software",
                          |        "organization_manager": false,
                          |        "timezone": "UTC",
                          |        "date_created": "2023-01-02T10:49:59+0000",
                          |        "date_last_login": null,
                          |        "browser": null,
                          |        "all_user_groups": [
                          |          1,
                          |          2
                          |        ],
                          |        "user_groups": [],
                          |        "agent_groups": [],
                          |        "labels": [],
                          |        "emails": [],
                          |        "phone_numbers": [
                          |          {
                          |            "number": "+441234564654",
                          |            "label": "Director",
                          |            "extension": "",
                          |            "person": 4,
                          |            "type": "person"
                          |          }
                          |        ],
                          |        "tickets_count": 0,
                          |        "chats_count": 0,
                          |        "fields": {},
                          |        "contextual_options": [],
                          |        "contact_data": [],
                          |        "teams": [],
                          |        "primary_team": null,
                          |        "brands": [
                          |          1
                          |        ],
                          |        "preferences": []
                          |      },
                          |      "5": {
                          |        "id": 5,
                          |        "primary_email": "david.steel@sagasaccounting.org.none",
                          |        "first_name": "David",
                          |        "last_name": "Steel",
                          |        "title_prefix": "",
                          |        "name": "David Steel",
                          |        "display_name": "David Steel",
                          |        "is_agent": false,
                          |        "avatar": {
                          |          "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
                          |          "url_pattern": null,
                          |          "base_gravatar_url": null
                          |        },
                          |        "online": false,
                          |        "online_for_chat": false,
                          |        "last_seen": null,
                          |        "agent_data": null,
                          |        "is_user": true,
                          |        "was_agent": false,
                          |        "can_agent": false,
                          |        "can_admin": false,
                          |        "can_billing": false,
                          |        "can_reports": false,
                          |        "picture_blob": null,
                          |        "disable_picture": false,
                          |        "gravatar_url": "https:\/\/secure.gravatar.com\/avatar\/4d09eb4bc3963f13e2538014e9acd9b4?&d=mm",
                          |        "is_contact": false,
                          |        "disable_autoresponses": false,
                          |        "disable_autoresponses_log": "",
                          |        "is_confirmed": false,
                          |        "is_deleted": false,
                          |        "is_disabled": false,
                          |        "creation_system": "web.person",
                          |        "override_display_name": "",
                          |        "display_contact": "David Steel <david.steel@sagasaccounting.org.none>",
                          |        "summary": "",
                          |        "language": 1,
                          |        "organization": 1,
                          |        "organization_members": [
                          |          4
                          |        ],
                          |        "organization_position": "Software Developer",
                          |        "organization_manager": true,
                          |        "timezone": "UTC",
                          |        "date_created": "2023-01-02T10:51:41+0000",
                          |        "date_last_login": null,
                          |        "browser": null,
                          |        "all_user_groups": [
                          |          1,
                          |          2
                          |        ],
                          |        "user_groups": [],
                          |        "agent_groups": [],
                          |        "labels": [],
                          |        "emails": [
                          |          "david.steel@sagasaccounting.org.none"
                          |        ],
                          |        "phone_numbers": [
                          |          {
                          |            "number": "+441234564654",
                          |            "label": null,
                          |            "extension": "",
                          |            "person": 5,
                          |            "type": "person"
                          |          }
                          |        ],
                          |        "tickets_count": 0,
                          |        "chats_count": 0,
                          |        "fields": {},
                          |        "contextual_options": [],
                          |        "contact_data": [],
                          |        "teams": [],
                          |        "primary_team": null,
                          |        "brands": [
                          |          1
                          |        ],
                          |        "preferences": []
                          |      },
                          |      "6": {
                          |        "id": 6,
                          |        "primary_email": "tina.swift@sagaaccounting.no",
                          |        "first_name": "Tina",
                          |        "last_name": "Swift",
                          |        "title_prefix": "",
                          |        "name": "Tina Swift",
                          |        "display_name": "Tina Swift",
                          |        "is_agent": false,
                          |        "avatar": {
                          |          "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
                          |          "url_pattern": null,
                          |          "base_gravatar_url": null
                          |        },
                          |        "online": false,
                          |        "online_for_chat": false,
                          |        "last_seen": null,
                          |        "agent_data": null,
                          |        "is_user": true,
                          |        "was_agent": false,
                          |        "can_agent": false,
                          |        "can_admin": false,
                          |        "can_billing": false,
                          |        "can_reports": false,
                          |        "picture_blob": null,
                          |        "disable_picture": false,
                          |        "gravatar_url": "https:\/\/secure.gravatar.com\/avatar\/8db610e3261ed70bef9df0cefbe887dc?&d=mm",
                          |        "is_contact": false,
                          |        "disable_autoresponses": false,
                          |        "disable_autoresponses_log": "",
                          |        "is_confirmed": false,
                          |        "is_deleted": false,
                          |        "is_disabled": false,
                          |        "creation_system": "web.person",
                          |        "override_display_name": "",
                          |        "display_contact": "Tina Swift <tina.swift@sagaaccounting.no>",
                          |        "summary": "",
                          |        "language": 1,
                          |        "organization": 1,
                          |        "organization_members": [
                          |          5,
                          |          6
                          |        ],
                          |        "organization_position": "",
                          |        "organization_manager": true,
                          |        "timezone": "UTC",
                          |        "date_created": "2023-01-02T10:54:03+0000",
                          |        "date_last_login": null,
                          |        "browser": null,
                          |        "all_user_groups": [
                          |          1,
                          |          2
                          |        ],
                          |        "user_groups": [],
                          |        "agent_groups": [],
                          |        "labels": [],
                          |        "emails": [
                          |          "tina.swift@sagaaccounting.no"
                          |        ],
                          |        "phone_numbers": [],
                          |        "tickets_count": 0,
                          |        "chats_count": 0,
                          |        "fields": {},
                          |        "contextual_options": [],
                          |        "contact_data": [],
                          |        "teams": [],
                          |        "primary_team": null,
                          |        "brands": [
                          |          1
                          |        ],
                          |        "preferences": []
                          |      },
                          |      "63": {
                          |        "id": 63,
                          |        "primary_email": "bob@example.com",
                          |        "first_name": "Andy",
                          |        "last_name": "Spaven",
                          |        "title_prefix": "",
                          |        "name": "Andy Spaven",
                          |        "display_name": "Andy Spaven",
                          |        "is_agent": false,
                          |        "avatar": {
                          |          "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
                          |          "url_pattern": null,
                          |          "base_gravatar_url": null
                          |        },
                          |        "online": false,
                          |        "online_for_chat": false,
                          |        "last_seen": null,
                          |        "agent_data": null,
                          |        "is_user": false,
                          |        "was_agent": false,
                          |        "can_agent": false,
                          |        "can_admin": false,
                          |        "can_billing": false,
                          |        "can_reports": false,
                          |        "picture_blob": null,
                          |        "disable_picture": false,
                          |        "gravatar_url": "https:\/\/secure.gravatar.com\/avatar\/4b9bb80620f03eb3719e0a061c14283d?&d=mm",
                          |        "is_contact": true,
                          |        "disable_autoresponses": false,
                          |        "disable_autoresponses_log": "",
                          |        "is_confirmed": false,
                          |        "is_deleted": false,
                          |        "is_disabled": false,
                          |        "creation_system": "web.person",
                          |        "override_display_name": "",
                          |        "display_contact": "Andy Spaven <bob@example.com>",
                          |        "summary": "",
                          |        "language": 1,
                          |        "organization": null,
                          |        "organization_members": [
                          |          8,
                          |          9
                          |        ],
                          |        "organization_position": "",
                          |        "organization_manager": true,
                          |        "timezone": "UTC",
                          |        "date_created": "2024-05-03T10:28:49+0000",
                          |        "date_last_login": null,
                          |        "browser": null,
                          |        "all_user_groups": [
                          |          1
                          |        ],
                          |        "user_groups": [],
                          |        "agent_groups": [],
                          |        "labels": [],
                          |        "emails": [
                          |          "bob@example.com"
                          |        ],
                          |        "phone_numbers": [],
                          |        "tickets_count": 0,
                          |        "chats_count": 0,
                          |        "fields": {},
                          |        "contextual_options": [],
                          |        "contact_data": [],
                          |        "teams": [],
                          |        "primary_team": null,
                          |        "brands": [
                          |          3
                          |        ],
                          |        "preferences": []
                          |      }
                          |    },
                          |    "organization": {
                          |      "1": {
                          |        "id": 1,
                          |        "name": "Saga Accounting",
                          |        "summary": "",
                          |        "importance": 0,
                          |        "fields": {
                          |          "1": {
                          |            "aliases": [],
                          |            "value": 1
                          |          },
                          |          "2": {
                          |            "aliases": [],
                          |            "value": "Mike"
                          |          },
                          |          "7": {
                          |            "aliases": [
                          |              "7",
                          |              "Apps",
                          |              "field7"
                          |            ],
                          |            "value": "https:\/\/admin.qa.tax.service.gov.uk\/api-gatekeeper\/applications?param1=1"
                          |          }
                          |        },
                          |        "user_groups": [],
                          |        "labels": [
                          |          "DRM",
                          |          "LG"
                          |        ],
                          |        "contact_data": [
                          |          {
                          |            "id": 1,
                          |            "contact_type": "website",
                          |            "comment": "",
                          |            "url": "www.sagac.org.uk"
                          |          }
                          |        ],
                          |        "emails": null,
                          |        "email_domains": [],
                          |        "date_created": "2023-01-02T10:47:53+0000",
                          |        "parent": null,
                          |        "chats_count": 0,
                          |        "tickets_count": 3,
                          |        "phone_numbers": []
                          |      }
                          |    }
                          |  }
                          |}""".stripMargin)
              .withStatus(OK)
          )
      )
    }
  }
}
