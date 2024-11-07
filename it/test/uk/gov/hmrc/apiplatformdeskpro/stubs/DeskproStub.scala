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
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector.{DeskproInactivePerson, DeskproTicket}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.{DeskproPerson, DeskproPersonUpdate, OrganisationId}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

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
}
