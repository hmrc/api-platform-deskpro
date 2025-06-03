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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{CreateDeskproTicket, DeskproInactivePerson}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproPerson, DeskproPersonUpdate, OrganisationId}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

trait DeskproStub {

  object CreateTicket {

    def stubSuccess(deskproTicket: CreateDeskproTicket) = {
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
              .withBody("""{"status":400,"code":"invalid_input","message":"Request input is invalid.","errors":{"errors":[{"code":"dupe_ticket","message":"Duplicate ticket."}]}}""")
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

  object UpdatePerson {

    def stubSuccess(personId: Int, deskproPerson: DeskproPersonUpdate) = {
      stubFor(
        put(urlMatching(s"/api/v2/people/${personId}"))
          .withRequestBody(equalTo(Json.toJson(deskproPerson).toString))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )
    }

    def stubBadRequest(personId: Int) = {
      stubFor(
        put(urlMatching(s"/api/v2/people/${personId}"))
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

    def stubInternalServerError(personId: Int) = {
      stubFor(
        put(urlMatching(s"/api/v2/people/${personId}"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
    }
  }

  object MarkPersonInactive {

    def stubSuccess(personId: Int, deskproInactivePerson: DeskproInactivePerson) = {
      stubFor(
        put(urlMatching(s"/api/v2/people/$personId"))
          .withRequestBody(equalTo(Json.toJson(deskproInactivePerson).toString))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )
    }

    def stubBadRequest(personId: Int) = {
      stubFor(
        put(urlMatching(s"/api/v2/people/$personId"))
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

    def stubInternalServerError(personId: Int) = {
      stubFor(
        put(urlMatching(s"/api/v2/people/${personId}"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
      )
    }
  }

  object GetPeopleByOrganisationId {

    def stubSuccess(organisationId: OrganisationId, pageWanted: Int = 1) = {
      stubFor(
        get(urlPathEqualTo(s"/api/v2/people"))
          .withQueryParam("organization", equalTo(organisationId.value))
          .withQueryParam("count", equalTo("200"))
          .withQueryParam("page", equalTo(pageWanted.toString))
          .willReturn(
            aResponse()
              .withBody("""{
                          |  "data": [
                          |    {
                          |      "id": 3,
                          |      "primary_email": null,
                          |      "first_name": "Jeff",
                          |      "last_name": "Smith",
                          |      "title_prefix": "",
                          |      "name": "Jeff Smith",
                          |      "display_name": "Jeff Smith",
                          |      "is_agent": false,
                          |      "avatar": {
                          |        "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
                          |        "url_pattern": null,
                          |        "base_gravatar_url": null
                          |      },
                          |      "online": false,
                          |      "online_for_chat": false,
                          |      "last_seen": null,
                          |      "agent_data": null,
                          |      "is_user": true,
                          |      "was_agent": false,
                          |      "can_agent": false,
                          |      "can_admin": false,
                          |      "can_billing": false,
                          |      "can_reports": false,
                          |      "picture_blob": null,
                          |      "disable_picture": false,
                          |      "gravatar_url": "&d=mm",
                          |      "is_contact": false,
                          |      "disable_autoresponses": false,
                          |      "disable_autoresponses_log": "",
                          |      "is_confirmed": false,
                          |      "is_deleted": false,
                          |      "is_disabled": false,
                          |      "creation_system": "web.person",
                          |      "override_display_name": "",
                          |      "display_contact": "Jeff Smith",
                          |      "summary": "",
                          |      "language": 1,
                          |      "organization": 1,
                          |      "organization_members": [
                          |        2
                          |      ],
                          |      "organization_position": "Product Owner",
                          |      "organization_manager": false,
                          |      "timezone": "UTC",
                          |      "date_created": "2023-01-02T10:48:34+0000",
                          |      "date_last_login": null,
                          |      "browser": null,
                          |      "all_user_groups": [
                          |        1,
                          |        2
                          |      ],
                          |      "user_groups": [],
                          |      "agent_groups": [],
                          |      "labels": [],
                          |      "emails": [],
                          |      "phone_numbers": [
                          |        {
                          |          "number": "+441234564654",
                          |          "label": "PO",
                          |          "extension": "",
                          |          "person": 3,
                          |          "type": "person"
                          |        }
                          |      ],
                          |      "tickets_count": 0,
                          |      "chats_count": 0,
                          |      "fields": {},
                          |      "contextual_options": [],
                          |      "contact_data": [],
                          |      "teams": [],
                          |      "primary_team": null,
                          |      "brands": [
                          |        1
                          |      ],
                          |      "preferences": []
                          |    },
                          |    {
                          |      "id": 63,
                          |      "primary_email": "bob@example.com",
                          |      "first_name": "Bob",
                          |      "last_name": "Emu",
                          |      "title_prefix": "",
                          |      "name": "Bob Emu",
                          |      "display_name": "Bob Emu",
                          |      "is_agent": false,
                          |      "avatar": {
                          |        "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
                          |        "url_pattern": null,
                          |        "base_gravatar_url": null
                          |      },
                          |      "online": false,
                          |      "online_for_chat": false,
                          |      "last_seen": null,
                          |      "agent_data": null,
                          |      "is_user": false,
                          |      "was_agent": false,
                          |      "can_agent": false,
                          |      "can_admin": false,
                          |      "can_billing": false,
                          |      "can_reports": false,
                          |      "picture_blob": null,
                          |      "disable_picture": false,
                          |      "gravatar_url": "https:\/\/secure.gravatar.com\/avatar\/4b9bb80620f03eb3719e0a061c14283d?&d=mm",
                          |      "is_contact": true,
                          |      "disable_autoresponses": false,
                          |      "disable_autoresponses_log": "",
                          |      "is_confirmed": false,
                          |      "is_deleted": false,
                          |      "is_disabled": false,
                          |      "creation_system": "web.person",
                          |      "override_display_name": "",
                          |      "display_contact": "Andy Spaven <bob@example.com>",
                          |      "summary": "",
                          |      "language": 1,
                          |      "organization": null,
                          |      "organization_members": [
                          |        8,
                          |        9
                          |      ],
                          |      "organization_position": "",
                          |      "organization_manager": true,
                          |      "timezone": "UTC",
                          |      "date_created": "2024-05-03T10:28:49+0000",
                          |      "date_last_login": null,
                          |      "browser": null,
                          |      "all_user_groups": [
                          |        1
                          |      ],
                          |      "user_groups": [],
                          |      "agent_groups": [],
                          |      "labels": [],
                          |      "emails": [
                          |        "bob@example.com"
                          |      ],
                          |      "phone_numbers": [],
                          |      "tickets_count": 0,
                          |      "chats_count": 0,
                          |      "fields": {},
                          |      "contextual_options": [],
                          |      "contact_data": [],
                          |      "teams": [],
                          |      "primary_team": null,
                          |      "brands": [
                          |        3
                          |      ],
                          |      "preferences": []
                          |    }
                          |  ],
                          |  "meta": {
                          |    "pagination": {
                          |      "total": 7,
                          |      "count": 7,
                          |      "per_page": 200,
                          |      "current_page": 1,
                          |      "total_pages": 1
                          |    }
                          |  },
                          |  "linked": {}
                          |}""".stripMargin)
              .withStatus(OK)
          )
      )
    }

    def stubSuccessNoPerson(organisationId: OrganisationId) = {
      stubFor(
        get(urlPathEqualTo(s"/api/v2/people"))
          .withQueryParam("organization", equalTo(organisationId.value))
          .withQueryParam("count", equalTo("200"))
          .withQueryParam("page", equalTo("1"))
          .willReturn(
            aResponse()
              .withBody("""{
                          |  "data": [
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
                          |
                          |  }
                          |}""".stripMargin)
              .withStatus(OK)
          )
      )
    }

    def stubFailure(organisationId: OrganisationId) = {
      stubFor(
        get(urlPathEqualTo(s"/api/v2/organizations/$organisationId/members"))
          .withQueryParam("include", equalTo("person"))
          .willReturn(
            aResponse()
              .withBody("{}")
              .withStatus(NOT_FOUND)
          )
      )
    }
  }

  object GetOrganisationById {

    def stubSuccess(organisationId: OrganisationId) = {
      stubFor(
        get(urlPathEqualTo(s"/api/v2/organizations/$organisationId"))
          .willReturn(
            aResponse()
              .withBody("""{
                          |  "data": {
                          |    "id": 1,
                          |    "name": "Example Accounting",
                          |    "summary": "",
                          |    "importance": 0,
                          |    "fields": {
                          |      "1": {
                          |        "aliases": [],
                          |        "value": 1
                          |      },
                          |      "2": {
                          |        "aliases": [],
                          |        "value": "Mike"
                          |      },
                          |      "7": {
                          |        "aliases": [
                          |          "7",
                          |          "Apps",
                          |          "field7"
                          |        ],
                          |        "value": "https:\\/\\/admin.qa.tax.service.gov.uk\\/api-gatekeeper\\/applications?param1=1"
                          |      }
                          |    },
                          |    "user_groups": [],
                          |    "labels": [
                          |      "DRM",
                          |      "LG"
                          |    ],
                          |    "contact_data": [
                          |      {
                          |        "id": 1,
                          |        "contact_type": "website",
                          |        "comment": "",
                          |        "url": "www.sagac.org.uk"
                          |      }
                          |    ],
                          |    "emails": null,
                          |    "email_domains": [],
                          |    "date_created": "2023-01-02T10:47:53+0000",
                          |    "parent": null,
                          |    "chats_count": 0,
                          |    "tickets_count": 3,
                          |    "phone_numbers": []
                          |  },
                          |  "meta": {},
                          |  "linked": {}
                          |}
                          |""".stripMargin)
              .withStatus(OK)
          )
      )
    }

    def stubFailure(organisationId: OrganisationId) = {
      stubFor(
        get(urlPathEqualTo(s"/api/v2/organizations/$organisationId"))
          .willReturn(
            aResponse()
              .withBody("{}")
              .withStatus(NOT_FOUND)
          )
      )
    }
  }

  object GetOrganisationsByEmail {

    def stubSuccess(email: LaxEmailAddress) = {
      stubFor(
        post(urlPathEqualTo(s"/api/v2/people"))
          .withRequestBody(equalToJson(
            s"""
               |{
               |  "primary_email":"${email.text}",
               |  "include": "organization_member,organization"
               |}
               |""".stripMargin
          ))
          .willReturn(
            aResponse()
              .withBody("""
                          |{
                          |  "data": [
                          |    {
                          |      "id": 63,
                          |      "primary_email": "bob@example.com",
                          |      "first_name": "Andy",
                          |      "last_name": "Spaven",
                          |      "title_prefix": "",
                          |      "name": "Andy Spaven",
                          |      "display_name": "Andy Spaven",
                          |      "is_agent": false,
                          |      "avatar": {
                          |        "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
                          |        "url_pattern": null,
                          |        "base_gravatar_url": null
                          |      },
                          |      "online": false,
                          |      "online_for_chat": false,
                          |      "last_seen": null,
                          |      "agent_data": null,
                          |      "is_user": false,
                          |      "was_agent": false,
                          |      "can_agent": false,
                          |      "can_admin": false,
                          |      "can_billing": false,
                          |      "can_reports": false,
                          |      "picture_blob": null,
                          |      "disable_picture": false,
                          |      "gravatar_url": "https:\/\/secure.gravatar.com\/avatar\/4b9bb80620f03eb3719e0a061c14283d?&d=mm",
                          |      "is_contact": true,
                          |      "disable_autoresponses": false,
                          |      "disable_autoresponses_log": "",
                          |      "is_confirmed": false,
                          |      "is_deleted": false,
                          |      "is_disabled": false,
                          |      "creation_system": "web.person",
                          |      "override_display_name": "",
                          |      "display_contact": "Andy Spaven <bob@example.com>",
                          |      "summary": "",
                          |      "language": 1,
                          |      "organization": null,
                          |      "organization_members": [
                          |        8,
                          |        9
                          |      ],
                          |      "organization_position": "",
                          |      "organization_manager": true,
                          |      "timezone": "UTC",
                          |      "date_created": "2024-05-03T10:28:49+0000",
                          |      "date_last_login": null,
                          |      "browser": null,
                          |      "all_user_groups": [
                          |        1
                          |      ],
                          |      "user_groups": [],
                          |      "agent_groups": [],
                          |      "labels": [],
                          |      "emails": [
                          |        "bob@example.com"
                          |      ],
                          |      "phone_numbers": [],
                          |      "tickets_count": 0,
                          |      "chats_count": 0,
                          |      "fields": {},
                          |      "contextual_options": [],
                          |      "contact_data": [],
                          |      "teams": [],
                          |      "primary_team": null,
                          |      "brands": [
                          |        3
                          |      ],
                          |      "preferences": []
                          |    }
                          |  ],
                          |  "meta": {
                          |    "pagination": {
                          |      "total": 1,
                          |      "count": 1,
                          |      "per_page": 10,
                          |      "current_page": 1,
                          |      "total_pages": 1
                          |    }
                          |  },
                          |  "linked": {
                          |    "organization_member": {
                          |      "8": {
                          |        "id": 8,
                          |        "person": 63,
                          |        "organization": 1,
                          |        "is_manager": true,
                          |        "position": "",
                          |        "created_at": "2024-07-23T10:48:10+0000",
                          |        "updated_at": "2024-07-29T12:55:26+0000"
                          |      },
                          |      "9": {
                          |        "id": 9,
                          |        "person": 63,
                          |        "organization": 3,
                          |        "is_manager": false,
                          |        "position": "",
                          |        "created_at": "2024-07-23T10:49:14+0000",
                          |        "updated_at": "2024-07-23T10:49:14+0000"
                          |      }
                          |    },
                          |    "organization": {
                          |      "1": {
                          |        "id": 1,
                          |        "name": "Saga Accounting dkfjgh",
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
                          |        "email_domains": [
                          |          "sagaaccounting.com"
                          |        ],
                          |        "date_created": "2023-01-02T10:47:53+0000",
                          |        "parent": null,
                          |        "chats_count": 0,
                          |        "tickets_count": 3,
                          |        "phone_numbers": []
                          |      },
                          |      "3": {
                          |        "id": 3,
                          |        "name": "Deans Demo Org",
                          |        "summary": "",
                          |        "importance": 0,
                          |        "fields": {
                          |          "1": {
                          |            "aliases": [],
                          |            "value": null
                          |          }
                          |        },
                          |        "user_groups": [],
                          |        "labels": [],
                          |        "contact_data": [],
                          |        "emails": null,
                          |        "email_domains": [],
                          |        "date_created": "2024-07-23T10:49:01+0000",
                          |        "parent": null,
                          |        "chats_count": 0,
                          |        "tickets_count": 0,
                          |        "phone_numbers": []
                          |      }
                          |    }
                          |  }
                          |}
                          |""".stripMargin)
              .withStatus(OK)
          )
      )
    }

    def stubFailure(email: LaxEmailAddress) = {
      stubFor(
        post(urlPathEqualTo(s"/api/v2/people"))
          .withRequestBody(equalToJson(
            s"""
               |{
               |  "primary_email":"${email.text}",
               |  "include": "organization_member,organization"
               |}
               |""".stripMargin
          ))
          .willReturn(
            aResponse()
              .withBody("{}")
              .withStatus(NOT_FOUND)
          )
      )
    }
  }

  object GetPersonForEmail {

    def stubSuccess(email: LaxEmailAddress) = {
      stubFor(
        post(urlPathEqualTo(s"/api/v2/people"))
          .withRequestBody(equalToJson(
            s"""
               |{
               |  "primary_email":"${email.text}"
               |}
               |""".stripMargin
          ))
          .willReturn(
            aResponse()
              .withBody("""
                          |{
                          |  "data": [
                          |    {
                          |      "id": 63,
                          |      "primary_email": "bob@example.com",
                          |      "first_name": "Andy",
                          |      "last_name": "Spaven",
                          |      "title_prefix": "",
                          |      "name": "Andy Spaven",
                          |      "display_name": "Andy Spaven",
                          |      "is_agent": false,
                          |      "avatar": {
                          |        "default_url_pattern": "https:\/\/apiplatformsupporttest.deskpro.com\/file.php\/avatar\/{{IMG_SIZE}}\/default.jpg?size-fit=1",
                          |        "url_pattern": null,
                          |        "base_gravatar_url": null
                          |      },
                          |      "online": false,
                          |      "online_for_chat": false,
                          |      "last_seen": null,
                          |      "agent_data": null,
                          |      "is_user": false,
                          |      "was_agent": false,
                          |      "can_agent": false,
                          |      "can_admin": false,
                          |      "can_billing": false,
                          |      "can_reports": false,
                          |      "picture_blob": null,
                          |      "disable_picture": false,
                          |      "gravatar_url": "https:\/\/secure.gravatar.com\/avatar\/4b9bb80620f03eb3719e0a061c14283d?&d=mm",
                          |      "is_contact": true,
                          |      "disable_autoresponses": false,
                          |      "disable_autoresponses_log": "",
                          |      "is_confirmed": false,
                          |      "is_deleted": false,
                          |      "is_disabled": false,
                          |      "creation_system": "web.person",
                          |      "override_display_name": "",
                          |      "display_contact": "Andy Spaven <bob@example.com>",
                          |      "summary": "",
                          |      "language": 1,
                          |      "organization": null,
                          |      "organization_members": [
                          |        8,
                          |        9
                          |      ],
                          |      "organization_position": "",
                          |      "organization_manager": true,
                          |      "timezone": "UTC",
                          |      "date_created": "2024-05-03T10:28:49+0000",
                          |      "date_last_login": null,
                          |      "browser": null,
                          |      "all_user_groups": [
                          |        1
                          |      ],
                          |      "user_groups": [],
                          |      "agent_groups": [],
                          |      "labels": [],
                          |      "emails": [
                          |        "bob@example.com"
                          |      ],
                          |      "phone_numbers": [],
                          |      "tickets_count": 0,
                          |      "chats_count": 0,
                          |      "fields": {},
                          |      "contextual_options": [],
                          |      "contact_data": [],
                          |      "teams": [],
                          |      "primary_team": null,
                          |      "brands": [
                          |        3
                          |      ],
                          |      "preferences": []
                          |    }
                          |  ],
                          |  "meta": {
                          |    "pagination": {
                          |      "total": 1,
                          |      "count": 1,
                          |      "per_page": 10,
                          |      "current_page": 1,
                          |      "total_pages": 1
                          |    }
                          |  },
                          |  "linked": {}
                          |}
                          |""".stripMargin)
              .withStatus(OK)
          )
      )
    }

    def stubFailure(email: LaxEmailAddress) = {
      stubFor(
        post(urlPathEqualTo(s"/api/v2/people"))
          .withRequestBody(equalToJson(
            s"""
               |{
               |  "primary_email":"${email.text}"
               |}
               |""".stripMargin
          ))
          .willReturn(
            aResponse()
              .withBody("{}")
              .withStatus(NOT_FOUND)
          )
      )
    }
  }

  object GetTicketsForPersonId {

    def stubSuccess(personId: Int, maybeStatus: Option[String]) = {

      val response = """{
                       |  "data": [ 
                       |    {
                       |      "id": 3432,
                       |      "ref": "SDST-2025XON927",
                       |      "auth": "WTKK9AWM9ZY6SJK",
                       |      "parent": null,
                       |      "language": null,
                       |      "brand": 3,
                       |      "department": 37,
                       |      "category": null,
                       |      "priority": null,
                       |      "workflow": null,
                       |      "product": null,
                       |      "person": 61,
                       |      "person_email": "bob@example.com",
                       |      "agent": 61,
                       |      "agent_team": 8,
                       |      "organization": 13,
                       |      "linked_chat": null,
                       |      "sent_to_address": [],
                       |      "email_account": null,
                       |      "email_account_address": "",
                       |      "creation_system": "web.api",
                       |      "creation_system_option": "",
                       |      "ticket_hash": "be1a60543910a4a33911df10cd5a5ccd5c49d3d6",
                       |      "status": "awaiting_user",
                       |      "ticket_status": "awaiting_user",
                       |      "is_hold": false,
                       |      "labels": null,
                       |      "urgency": 1,
                       |      "feedback_rating": null,
                       |      "date_feedback_rating": null,
                       |      "date_created": "2025-05-01T08:02:02+0000",
                       |      "date_resolved": null,
                       |      "date_archived": null,
                       |      "date_first_agent_assign": "2025-05-19T11:51:18+0000",
                       |      "date_first_agent_reply": "2025-05-01T08:02:02+0000",
                       |      "date_last_agent_reply": "2025-05-20T07:24:41+0000",
                       |      "date_last_user_reply": "2025-05-20T07:24:41+0000",
                       |      "date_agent_waiting": "2025-05-20T07:24:40+0000",
                       |      "date_user_waiting": null,
                       |      "date_status": "2025-05-20T07:24:41+0000",
                       |      "total_user_waiting": 1639358,
                       |      "total_to_first_reply": 0,
                       |      "total_user_waiting_wh": 421187,
                       |      "total_to_first_reply_wh": 0,
                       |      "locked_by_agent": null,
                       |      "date_locked": null,
                       |      "date_on_hold": null,
                       |      "has_attachments": true,
                       |      "subject": "HMRC Developer Hub: Support Enquiry",
                       |      "original_subject": "HMRC Developer Hub: Support Enquiry",
                       |      "properties": null,
                       |      "problems": [],
                       |      "count_agent_replies": 3,
                       |      "count_user_replies": 0,
                       |      "worst_sla_status": null,
                       |      "waiting_times": [
                       |        {
                       |          "start": 1746086523,
                       |          "end": 1747655693,
                       |          "length": 1569170,
                       |          "ticket_status": "awaiting_agent"
                       |        },
                       |        {
                       |          "start": 1747655694,
                       |          "end": 1747656009,
                       |          "length": 315,
                       |          "ticket_status": "pending"
                       |        },
                       |        {
                       |          "start": 1747656009,
                       |          "end": 1747725880,
                       |          "length": 69871,
                       |          "ticket_status": "pending"
                       |        }
                       |      ],
                       |      "ticket_slas": [
                       |        3404
                       |      ],
                       |      "fields": {
                       |        "7": {
                       |          "aliases": [],
                       |          "value": "Finding the API needed to build my software"
                       |        },
                       |        "8": {
                       |          "aliases": [],
                       |          "value": "Bob's burgers"
                       |        }
                       |      },
                       |      "contextual_fields": {},
                       |      "children": [],
                       |      "siblings": [],
                       |      "cc": [],
                       |      "star": null,
                       |      "ticket_layout": null,
                       |      "ticket_excerpt": null,
                       |      "ticket_agent_errors": null,
                       |      "ticket_user_errors": null,
                       |      "ticket_permissions": null,
                       |      "access_code": "FCAWTKK9AWM9ZY6SJK",
                       |      "followers": ""
                       |    },
                       |    {
                       |      "id": 443,
                       |      "ref": "SDST-2024EKL881",
                       |      "auth": "HQT9DKH3Q6PCJJA",
                       |      "parent": null,
                       |      "language": null,
                       |      "brand": 3,
                       |      "department": 37,
                       |      "category": null,
                       |      "priority": null,
                       |      "workflow": null,
                       |      "product": null,
                       |      "person": 61,
                       |      "person_email": "bob@example.com",
                       |      "agent": null,
                       |      "agent_team": null,
                       |      "organization": 14,
                       |      "linked_chat": null,
                       |      "sent_to_address": [],
                       |      "email_account": null,
                       |      "email_account_address": "",
                       |      "creation_system": "web.api",
                       |      "creation_system_option": "",
                       |      "ticket_hash": "aa7cad2d714a721edd7519cc0edbfea0291cdf8f",
                       |      "status": "awaiting_agent",
                       |      "ticket_status": "awaiting_agent",
                       |      "is_hold": false,
                       |      "labels": null,
                       |      "urgency": 1,
                       |      "feedback_rating": null,
                       |      "date_feedback_rating": null,
                       |      "date_created": "2024-04-19T12:32:26+0000",
                       |      "date_resolved": null,
                       |      "date_archived": null,
                       |      "date_first_agent_assign": null,
                       |      "date_first_agent_reply": null,
                       |      "date_last_agent_reply": null,
                       |      "date_last_user_reply": "2024-04-19T12:32:26+0000",
                       |      "date_agent_waiting": null,
                       |      "date_user_waiting": null,
                       |      "date_status": "2024-04-19T12:32:27+0000",
                       |      "total_user_waiting": 0,
                       |      "total_to_first_reply": 0,
                       |      "total_user_waiting_wh": 0,
                       |      "total_to_first_reply_wh": 0,
                       |      "locked_by_agent": null,
                       |      "date_locked": null,
                       |      "date_on_hold": null,
                       |      "has_attachments": false,
                       |      "subject": "HMRC Developer Hub: Support Enquiry",
                       |      "original_subject": "HMRC Developer Hub: Support Enquiry",
                       |      "properties": null,
                       |      "problems": [],
                       |      "count_agent_replies": 1,
                       |      "count_user_replies": 0,
                       |      "worst_sla_status": null,
                       |      "waiting_times": [],
                       |      "ticket_slas": [
                       |        415
                       |      ],
                       |      "fields": {
                       |        "6": {
                       |          "aliases": [],
                       |          "value": "Individuals Child Benefit"
                       |        },
                       |        "7": {
                       |          "aliases": [],
                       |          "value": "api"
                       |        }
                       |      },
                       |      "contextual_fields": {},
                       |      "children": [],
                       |      "siblings": [],
                       |      "cc": [],
                       |      "star": null,
                       |      "ticket_layout": null,
                       |      "ticket_excerpt": null,
                       |      "ticket_agent_errors": null,
                       |      "ticket_user_errors": null,
                       |      "ticket_permissions": null,
                       |      "access_code": "RBHQT9DKH3Q6PCJJA",
                       |      "followers": ""
                       |    }
                       |  ],
                       |  "meta": {
                       |    "fql": "ticket.person = 61 AND ticket.status != \"hidden\"",
                       |    "pagination": {
                       |      "total": 2,
                       |      "count": 2,
                       |      "per_page": 2,
                       |      "current_page": 1,
                       |      "total_pages": 1
                       |    }
                       |  },
                       |  "linked": {}
                       |}""".stripMargin

      maybeStatus match {
        case Some(status) => {
          stubFor(
            get(urlPathEqualTo("/api/v2/tickets"))
              .withQueryParam("person", equalTo(personId.toString()))
              .withQueryParam("status", equalTo(status))
              .willReturn(
                aResponse()
                  .withStatus(OK)
                  .withBody(response)
              )
          )
        }
        case _            => {
          stubFor(
            get(urlPathEqualTo("/api/v2/tickets"))
              .withQueryParam("person", equalTo(personId.toString()))
              .willReturn(
                aResponse()
                  .withStatus(OK)
                  .withBody(response)
              )
          )
        }
      }
    }
  }

  object FetchTicket {

    def stubSuccess(ticketId: Int) = {
      stubFor(
        get(urlPathEqualTo(s"/api/v2/tickets/$ticketId"))
          .willReturn(
            aResponse()
              .withBody("""{
                          |  "data": { 
                          |    "id": 3432,
                          |    "ref": "SDST-2025XON927",
                          |    "auth": "WTKK9AWM9ZY6SJK",
                          |    "parent": null,
                          |    "language": null,
                          |    "brand": 3,
                          |    "department": 37,
                          |    "category": null,
                          |    "priority": null,
                          |    "workflow": null,
                          |    "product": null,
                          |    "person": 61,
                          |    "person_email": "bob@example.com",
                          |    "agent": 61,
                          |    "agent_team": 8,
                          |    "organization": 13,
                          |    "linked_chat": null,
                          |    "sent_to_address": [],
                          |    "email_account": null,
                          |    "email_account_address": "",
                          |    "creation_system": "web.api",
                          |    "creation_system_option": "",
                          |    "ticket_hash": "be1a60543910a4a33911df10cd5a5ccd5c49d3d6",
                          |    "status": "awaiting_user",
                          |    "ticket_status": "awaiting_user",
                          |    "is_hold": false,
                          |    "labels": null,
                          |    "urgency": 1,
                          |    "feedback_rating": null,
                          |    "date_feedback_rating": null,
                          |    "date_created": "2025-05-01T08:02:02+0000",
                          |    "date_resolved": null,
                          |    "date_archived": null,
                          |    "date_first_agent_assign": "2025-05-19T11:51:18+0000",
                          |    "date_first_agent_reply": "2025-05-01T08:02:02+0000",
                          |    "date_last_agent_reply": "2025-05-20T07:24:41+0000",
                          |    "date_last_user_reply": "2025-05-20T07:24:41+0000",
                          |    "date_agent_waiting": "2025-05-20T07:24:40+0000",
                          |    "date_user_waiting": null,
                          |    "date_status": "2025-05-20T07:24:41+0000",
                          |    "total_user_waiting": 1639358,
                          |    "total_to_first_reply": 0,
                          |    "total_user_waiting_wh": 421187,
                          |    "total_to_first_reply_wh": 0,
                          |    "locked_by_agent": null,
                          |    "date_locked": null,
                          |    "date_on_hold": null,
                          |    "has_attachments": true,
                          |    "subject": "HMRC Developer Hub: Support Enquiry",
                          |    "original_subject": "HMRC Developer Hub: Support Enquiry",
                          |    "properties": null,
                          |    "problems": [],
                          |    "count_agent_replies": 3,
                          |    "count_user_replies": 0,
                          |    "worst_sla_status": null,
                          |    "waiting_times": [
                          |      {
                          |        "start": 1746086523,
                          |        "end": 1747655693,
                          |        "length": 1569170,
                          |        "ticket_status": "awaiting_agent"
                          |      },
                          |      {
                          |        "start": 1747655694,
                          |        "end": 1747656009,
                          |        "length": 315,
                          |        "ticket_status": "pending"
                          |      },
                          |      {
                          |        "start": 1747656009,
                          |        "end": 1747725880,
                          |        "length": 69871,
                          |        "ticket_status": "pending"
                          |      }
                          |    ],
                          |    "ticket_slas": [
                          |      3404
                          |    ],
                          |    "fields": {
                          |      "7": {
                          |        "aliases": [],
                          |        "value": "Finding the API needed to build my software"
                          |      },
                          |      "8": {
                          |        "aliases": [],
                          |        "value": "Bob's burgers"
                          |      }
                          |    },
                          |    "contextual_fields": {},
                          |    "children": [],
                          |    "siblings": [],
                          |    "cc": [],
                          |    "star": null,
                          |    "ticket_layout": null,
                          |    "ticket_excerpt": null,
                          |    "ticket_agent_errors": null,
                          |    "ticket_user_errors": null,
                          |    "ticket_permissions": null,
                          |    "access_code": "FCAWTKK9AWM9ZY6SJK",
                          |    "followers": ""
                          |  },
                          |  "meta": {},
                          |  "linked": {}
                          |}""".stripMargin)
              .withStatus(OK)
          )
      )
    }

    def stubFailure(ticketId: Int) = {
      stubFor(
        get(urlPathEqualTo(s"/api/v2/tickets/$ticketId"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )
    }
  }

  object GetTicketMessages {

    def stubSuccess(ticketId: Int) = {
      stubFor(
        get(urlPathEqualTo(s"/api/v2/tickets/$ticketId/messages"))
          .willReturn(
            aResponse()
              .withBody("""{
                          |  "data": [ 
                          |    {
                          |      "id": 3467,
                          |      "ticket": 3432,
                          |      "person": 33,
                          |      "email_source": null,
                          |      "attributes": [
                          |        {
                          |          "name": "agent_type",
                          |          "value": "agent",
                          |          "date_created": "2025-05-01T08:02:02+0000",
                          |          "type": "value"
                          |        },
                          |        {
                          |          "name": "email_recipients",
                          |          "value": "[{\"type\":\"to\",\"address\":\"pete.kirby@digital.hmrc.gov.uk\"}]",
                          |          "date_created": "2025-05-01T08:02:02+0000",
                          |          "type": "value"
                          |        }
                          |      ],
                          |      "attachments": [],
                          |      "date_created": "2025-05-01T08:02:02+0000",
                          |      "is_agent_note": 0,
                          |      "creation_system": "web.api",
                          |      "ip_address": "",
                          |      "visitor_id": null,
                          |      "hostname": "",
                          |      "email": "",
                          |      "message_hash": "7fe219f88a4ed6bd2b7b8f6c6bc1712e6d0d5a70",
                          |      "primary_translation": null,
                          |      "message": "Hi. What API do I need to get next weeks lottery numbers?",
                          |      "message_full": "",
                          |      "message_raw": null,
                          |      "message_preview_text": "Hi. What API do I need to get next weeks lottery numbers?",
                          |      "show_full_hint": false,
                          |      "lang_code": null
                          |    },
                          |    {
                          |      "id": 3698,
                          |      "ticket": 3432,
                          |      "person": 61,
                          |      "email_source": null,
                          |      "attributes": [
                          |        {
                          |          "name": "agent_type",
                          |          "value": "agent",
                          |          "date_created": "2025-05-19T11:54:53+0000",
                          |          "type": "value"
                          |        },
                          |        {
                          |          "name": "email_recipients",
                          |          "value": "[{\"type\":\"to\",\"address\":\"pete.kirby@digital.hmrc.gov.uk\"}]",
                          |          "date_created": "2025-05-19T11:54:53+0000",
                          |          "type": "value"
                          |        }
                          |      ],
                          |      "attachments": [],
                          |      "date_created": "2025-05-19T11:54:53+0000",
                          |      "is_agent_note": 0,
                          |      "creation_system": "web",
                          |      "ip_address": "",
                          |      "visitor_id": null,
                          |      "hostname": "",
                          |      "email": "",
                          |      "message_hash": "72d080ba0552fdf024c843a20fff3831f8f76387",
                          |      "primary_translation": null,
                          |      "message": "<p>Reply message from agent. What else gets filled in?</p>",
                          |      "message_full": "",
                          |      "message_raw": null,
                          |      "message_preview_text": "Reply message from agent. What else gets filled in?",
                          |      "show_full_hint": false,
                          |      "lang_code": null
                          |    }
                          |  ],
                          |  "meta": {
                          |    "pagination": {
                          |      "total": 4,
                          |      "count": 4,
                          |      "per_page": 10,
                          |      "current_page": 1,
                          |      "total_pages": 1
                          |    }
                          |  },
                          |  "linked": {}
                          |}""".stripMargin)
              .withStatus(OK)
          )
      )
    }
  }
}
