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

package uk.gov.hmrc.apiplatformdeskpro.service

import java.net.URL
import java.time.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import uk.gov.hmrc.apiplatformdeskpro.config.AppConfig
import uk.gov.hmrc.apiplatformdeskpro.connector.DeskproConnector
import uk.gov.hmrc.apiplatformdeskpro.domain.models._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.connector._
import uk.gov.hmrc.apiplatformdeskpro.domain.models.controller.CreateTicketResponseRequest
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.UploadStatus.{Failed, UploadedSuccessfully}
import uk.gov.hmrc.apiplatformdeskpro.domain.models.mongo.{BlobDetails, DeskproMessageFileAttachment, UploadedFile}
import uk.gov.hmrc.apiplatformdeskpro.repository.{DeskproMessageFileAttachmentRepository, UploadedFileRepository}
import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class TicketServiceSpec extends AsyncHmrcSpec with FixedClock {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockDeskproConnector          = mock[DeskproConnector]
    val mockPersonService             = mock[PersonService]
    val mockAppConfig                 = mock[AppConfig]
    val mockMessageFileAttachmentRepo = mock[DeskproMessageFileAttachmentRepository]
    val mockUploadedFileRepo          = mock[UploadedFileRepository]

    val fullName        = "Bob Holness"
    val email           = LaxEmailAddress("bob@example.com")
    val subject         = "Subject of the ticket"
    val message         = "This is where the message for the ticket goes"
    val fileReference   = "fileRef"
    val apiName         = "apiName"
    val applicationId   = ApplicationId.random.toString()
    val organisation    = "organisation"
    val supportReason   = "supportReason"
    val reasonKey       = "reason-key"
    val teamMemberEmail = "frank@example.com"
    val ref             = "ref"
    val brand           = 1

    val personId           = 34
    val personEmail        = LaxEmailAddress("bob@example.com")
    val status             = Some("resolved")
    val ticketId: Int      = 123
    val messageId: Int     = 789
    val deskproTicket1     = DeskproTicketResponse(ticketId, "ref1", personId, "bob@example.com", "awaiting_user", instant, instant, Some(instant), "subject 1")
    val deskproTicket2     = DeskproTicketResponse(456, "ref2", personId, "bob@example.com", "awaiting_agent", instant, instant, None, "subject 2")
    val deskproMessage1    = DeskproMessageResponse(787, ticketId, personId, instant, 0, "message 1", List.empty)
    val deskproMessage2    = DeskproMessageResponse(788, ticketId, personId, instant.minus(Duration.ofDays(2)), 0, "message 2", List.empty)
    val deskproMessage3    = DeskproMessageResponse(789, ticketId, personId, instant.minus(Duration.ofDays(5)), 0, "message 3", List.empty)
    val attachmentId       = 1
    val deskproAttachment1 = DeskproAttachmentResponse(attachmentId, DeskproBlobResponse(123, "auth", "https://example.com", "file.name"))

    val messageResponse = DeskproCreateMessageResponse(messageId, ticketId, personId, instant, 0, "message", List.empty)
    val messageWrapper  = DeskproCreateMessageWrapperResponse(messageResponse)

    val blobId       = 67890
    val blobAuth     = "FHFK558GGDGFD45465GHJGJ"
    val blobDetails  = BlobDetails(blobId, blobAuth)
    val uploadStatus = UploadedSuccessfully("fileName", "text/plain", new URL("https://example.com/file1"), 1000, BlobDetails(blobId, blobAuth))
    val uploadedFile = UploadedFile(fileReference, uploadStatus, instant)

    val underTest = new TicketService(mockDeskproConnector, mockPersonService, mockMessageFileAttachmentRepo, mockUploadedFileRepo, mockAppConfig, clock)
  }

  "submitTicket" should {
    "successfully create a new deskpro ticket with all custom fields" in new Setup {

      val createTicketRequest = CreateTicketRequest(
        fullName,
        email.text,
        subject,
        message,
        Some(apiName),
        Some(applicationId),
        Some(organisation),
        Some(supportReason),
        Some(reasonKey),
        Some(teamMemberEmail)
      )

      val fields                = Map("2" -> apiName, "3" -> applicationId, "4" -> organisation, "5" -> supportReason, "6" -> teamMemberEmail, "7" -> reasonKey)
      val expectedPerson        = DeskproPerson(fullName, email.text)
      val expectedDeskproTicket = CreateDeskproTicket(expectedPerson, subject, DeskproTicketMessage(message, expectedPerson), brand, fields)

      when(mockDeskproConnector.createTicket(*)(*)).thenReturn(Future.successful(Right(DeskproTicketCreated(ref))))

      when(mockAppConfig.deskproBrand).thenReturn(brand)
      when(mockAppConfig.deskproApiName).thenReturn("2")
      when(mockAppConfig.deskproApplicationId).thenReturn("3")
      when(mockAppConfig.deskproOrganisation).thenReturn("4")
      when(mockAppConfig.deskproSupportReason).thenReturn("5")
      when(mockAppConfig.deskproReasonKey).thenReturn("7")
      when(mockAppConfig.deskproTeamMemberEmail).thenReturn("6")

      val result = await(underTest.submitTicket(createTicketRequest))

      result shouldBe Right(DeskproTicketCreated(ref))
      verify(mockDeskproConnector).createTicket(eqTo(expectedDeskproTicket))(*)
    }

    "successfully create a new deskpro ticket with no custom fields" in new Setup {

      val createTicketRequest = CreateTicketRequest(
        fullName,
        email.text,
        subject,
        message,
        None,
        None,
        None,
        None,
        None,
        None
      )

      val fields: Map[String, String] = Map.empty
      val expectedPerson              = DeskproPerson(fullName, email.text)
      val expectedDeskproTicket       = CreateDeskproTicket(expectedPerson, subject, DeskproTicketMessage(message, expectedPerson), brand, fields)

      when(mockDeskproConnector.createTicket(*)(*)).thenReturn(Future.successful(Right(DeskproTicketCreated(ref))))

      when(mockAppConfig.deskproBrand).thenReturn(brand)

      val result = await(underTest.submitTicket(createTicketRequest))

      result shouldBe Right(DeskproTicketCreated(ref))
      verify(mockDeskproConnector).createTicket(eqTo(expectedDeskproTicket))(*)
    }
  }

  "getTicketsForPerson" should {
    "return a list of DeskproTickets" in new Setup {
      when(mockPersonService.getPersonIdForEmail(eqTo(personEmail))(*)).thenReturn(Future.successful(personId))
      when(mockDeskproConnector.getTicketsForPersonId(eqTo(personId), eqTo(status), *, *, *)(*)).thenReturn(Future.successful(DeskproTicketsWrapperResponse(List(
        deskproTicket1,
        deskproTicket2
      ))))

      val result = await(underTest.getTicketsForPerson(personEmail, status))

      val expectedResponse = List(
        DeskproTicket(123, "ref1", personId, LaxEmailAddress("bob@example.com"), "awaiting_user", instant, instant, Some(instant), "subject 1", List.empty),
        DeskproTicket(456, "ref2", personId, LaxEmailAddress("bob@example.com"), "awaiting_agent", instant, instant, None, "subject 2", List.empty)
      )
      result shouldBe expectedResponse
    }
  }

  "batchFetchTicket" should {
    "return a DeskproTicket with messages" in new Setup {
      val batchResponse = BatchResponse(
        BatchTicketResponse(
          BatchTicketWrapperResponse(BatchHeadersResponse(200), Some(deskproTicket1)),
          BatchMessagesWrapperResponse(BatchHeadersResponse(200), Some(List(deskproMessage1, deskproMessage2, deskproMessage3))),
          BatchAttachmentsWrapperResponse(BatchHeadersResponse(200), Some(List.empty))
        )
      )
      when(mockDeskproConnector.batchFetchTicket(*, *, *, *)(*)).thenReturn(Future.successful(batchResponse))

      val result = await(underTest.batchFetchTicket(ticketId))

      val expectedResponse =
        DeskproTicket(
          123,
          "ref1",
          personId,
          LaxEmailAddress("bob@example.com"),
          "awaiting_user",
          instant,
          instant,
          Some(instant),
          "subject 1",
          List(
            DeskproMessage(787, ticketId, personId, instant, false, "message 1", List.empty),
            DeskproMessage(788, ticketId, personId, instant.minus(Duration.ofDays(2)), false, "message 2", List.empty),
            DeskproMessage(789, ticketId, personId, instant.minus(Duration.ofDays(5)), false, "message 3", List.empty)
          )
        )

      result shouldBe Some(expectedResponse)
    }
    "return a DeskproTicket with attached messages" in new Setup {
      val batchResponse = BatchResponse(
        BatchTicketResponse(
          BatchTicketWrapperResponse(BatchHeadersResponse(200), Some(deskproTicket1)),
          BatchMessagesWrapperResponse(BatchHeadersResponse(200), Some(List(deskproMessage1.copy(attachments = List(attachmentId)), deskproMessage2, deskproMessage3))),
          BatchAttachmentsWrapperResponse(BatchHeadersResponse(200), Some(List(deskproAttachment1)))
        )
      )
      when(mockDeskproConnector.batchFetchTicket(*, *, *, *)(*)).thenReturn(Future.successful(batchResponse))

      val result = await(underTest.batchFetchTicket(ticketId))

      val expectedResponse =
        DeskproTicket(
          123,
          "ref1",
          personId,
          LaxEmailAddress("bob@example.com"),
          "awaiting_user",
          instant,
          instant,
          Some(instant),
          "subject 1",
          List(
            DeskproMessage(787, ticketId, personId, instant, false, "message 1", List(DeskproAttachment(attachmentId, "file.name", "https://example.com"))),
            DeskproMessage(788, ticketId, personId, instant.minus(Duration.ofDays(2)), false, "message 2", List.empty),
            DeskproMessage(789, ticketId, personId, instant.minus(Duration.ofDays(5)), false, "message 3", List.empty)
          )
        )

      result shouldBe Some(expectedResponse)
    }

    "return a DeskproTicket with no messages" in new Setup {
      val batchResponse = BatchResponse(
        BatchTicketResponse(
          BatchTicketWrapperResponse(BatchHeadersResponse(200), Some(deskproTicket1)),
          BatchMessagesWrapperResponse(BatchHeadersResponse(200), Some(List.empty)),
          BatchAttachmentsWrapperResponse(BatchHeadersResponse(200), Some(List.empty))
        )
      )
      when(mockDeskproConnector.batchFetchTicket(*, *, *, *)(*)).thenReturn(Future.successful(batchResponse))

      val result = await(underTest.batchFetchTicket(ticketId))

      val expectedResponse =
        DeskproTicket(
          123,
          "ref1",
          personId,
          LaxEmailAddress("bob@example.com"),
          "awaiting_user",
          instant,
          instant,
          Some(instant),
          "subject 1",
          List.empty
        )

      result shouldBe Some(expectedResponse)
    }

    "return a DeskproTicket with messages not found" in new Setup {
      val batchResponse = BatchResponse(
        BatchTicketResponse(
          BatchTicketWrapperResponse(BatchHeadersResponse(200), Some(deskproTicket1)),
          BatchMessagesWrapperResponse(BatchHeadersResponse(404), None),
          BatchAttachmentsWrapperResponse(BatchHeadersResponse(200), None)
        )
      )
      when(mockDeskproConnector.batchFetchTicket(*, *, *, *)(*)).thenReturn(Future.successful(batchResponse))

      val result = await(underTest.batchFetchTicket(ticketId))

      val expectedResponse =
        DeskproTicket(
          123,
          "ref1",
          personId,
          LaxEmailAddress("bob@example.com"),
          "awaiting_user",
          instant,
          instant,
          Some(instant),
          "subject 1",
          List.empty
        )

      result shouldBe Some(expectedResponse)
    }

    "return a None if not found" in new Setup {
      val batchResponse = BatchResponse(
        BatchTicketResponse(
          BatchTicketWrapperResponse(BatchHeadersResponse(404), None),
          BatchMessagesWrapperResponse(BatchHeadersResponse(404), None),
          BatchAttachmentsWrapperResponse(BatchHeadersResponse(200), None)
        )
      )
      when(mockDeskproConnector.batchFetchTicket(*, *, *, *)(*)).thenReturn(Future.successful(batchResponse))

      val result = await(underTest.batchFetchTicket(ticketId))

      result shouldBe None
    }
  }

  "createMessage" should {

    "return DeskproTicketResponseSuccess when response created" in new Setup {
      when(mockDeskproConnector.createMessage(*, *, *)(*)).thenReturn(Future.successful(messageWrapper))
      when(mockDeskproConnector.updateTicketStatus(*, *)(*)).thenReturn(Future.successful(DeskproTicketUpdateSuccess))

      val result = await(underTest.createMessage(ticketId, CreateTicketResponseRequest(email, message, TicketStatus.AwaitingAgent)))

      result shouldBe messageResponse

      verify(mockDeskproConnector).createMessage(eqTo(ticketId), eqTo(email.text), eqTo(message))(*)
      verify(mockDeskproConnector, never).createMessageWithAttachment(*, *, *, *, *)(*)
      verify(mockDeskproConnector).updateTicketStatus(eqTo(ticketId), eqTo(TicketStatus.AwaitingAgent))(*)
      verify(mockMessageFileAttachmentRepo, never).create(*)
    }

    "return DeskproTicketResponseSuccess and save response when fileReference is present and the file is already in Deskpro as a blob" in new Setup {
      when(mockUploadedFileRepo.fetchByFileReference(*)).thenReturn(Future.successful(Some(uploadedFile)))
      when(mockDeskproConnector.createMessageWithAttachment(*, *, *, *, *)(*)).thenReturn(Future.successful(messageWrapper))
      when(mockDeskproConnector.updateTicketStatus(*, *)(*)).thenReturn(Future.successful(DeskproTicketUpdateSuccess))
      val response = DeskproMessageFileAttachment(ticketId, messageId, fileReference, instant)
      when(mockMessageFileAttachmentRepo.create(*)).thenReturn(Future.successful(response))

      val result = await(underTest.createMessage(ticketId, CreateTicketResponseRequest(email, message, TicketStatus.AwaitingAgent, Some(fileReference))))

      result shouldBe messageResponse

      verify(mockDeskproConnector).createMessageWithAttachment(eqTo(ticketId), eqTo(email.text), eqTo(message), eqTo(blobId), eqTo(blobAuth))(*)
      verify(mockDeskproConnector, never).createMessage(*, *, *)(*)
      verify(mockDeskproConnector).updateTicketStatus(eqTo(ticketId), eqTo(TicketStatus.AwaitingAgent))(*)
      verify(mockMessageFileAttachmentRepo).create(eqTo(response))
    }

    "return DeskproTicketResponseSuccess and save response when fileReference is present and the file has failed to upload" in new Setup {
      val failedUploadStatus = Failed("message", "reason")
      val failedUploadedFile = UploadedFile(fileReference, failedUploadStatus, instant)

      when(mockUploadedFileRepo.fetchByFileReference(*)).thenReturn(Future.successful(Some(failedUploadedFile)))
      when(mockDeskproConnector.createMessage(*, *, *)(*)).thenReturn(Future.successful(messageWrapper))
      when(mockDeskproConnector.updateTicketStatus(*, *)(*)).thenReturn(Future.successful(DeskproTicketUpdateSuccess))
      val response = DeskproMessageFileAttachment(ticketId, messageId, fileReference, instant)
      when(mockMessageFileAttachmentRepo.create(*)).thenReturn(Future.successful(response))

      val result = await(underTest.createMessage(ticketId, CreateTicketResponseRequest(email, message, TicketStatus.AwaitingAgent, Some(fileReference))))

      result shouldBe messageResponse

      verify(mockDeskproConnector).createMessage(eqTo(ticketId), eqTo(email.text), eqTo(message))(*)
      verify(mockDeskproConnector, never).createMessageWithAttachment(*, *, *, *, *)(*)
      verify(mockDeskproConnector).updateTicketStatus(eqTo(ticketId), eqTo(TicketStatus.AwaitingAgent))(*)
      verify(mockMessageFileAttachmentRepo).create(eqTo(response))
    }

    "return DeskproTicketResponseSuccess and save response when fileReference is present and the file has not been uploaded" in new Setup {
      when(mockUploadedFileRepo.fetchByFileReference(*)).thenReturn(Future.successful(None))
      when(mockDeskproConnector.createMessage(*, *, *)(*)).thenReturn(Future.successful(messageWrapper))
      when(mockDeskproConnector.updateTicketStatus(*, *)(*)).thenReturn(Future.successful(DeskproTicketUpdateSuccess))
      val response = DeskproMessageFileAttachment(ticketId, messageId, fileReference, instant)
      when(mockMessageFileAttachmentRepo.create(*)).thenReturn(Future.successful(response))

      val result = await(underTest.createMessage(ticketId, CreateTicketResponseRequest(email, message, TicketStatus.AwaitingAgent, Some(fileReference))))

      result shouldBe messageResponse

      verify(mockDeskproConnector).createMessage(eqTo(ticketId), eqTo(email.text), eqTo(message))(*)
      verify(mockDeskproConnector, never).createMessageWithAttachment(*, *, *, *, *)(*)
      verify(mockDeskproConnector).updateTicketStatus(eqTo(ticketId), eqTo(TicketStatus.AwaitingAgent))(*)
      verify(mockMessageFileAttachmentRepo).create(eqTo(response))
    }

    "return DeskproTicketResponseSuccess when response created, even if ticket status update failed" in new Setup {
      when(mockDeskproConnector.createMessage(*, *, *)(*)).thenReturn(Future.successful(messageWrapper))
      when(mockDeskproConnector.updateTicketStatus(*, *)(*)).thenReturn(Future.successful(DeskproTicketUpdateFailure))

      val result = await(underTest.createMessage(ticketId, CreateTicketResponseRequest(email, message, TicketStatus.AwaitingAgent)))

      result shouldBe messageResponse

      verify(mockDeskproConnector).createMessage(eqTo(ticketId), eqTo(email.text), eqTo(message))(*)
      verify(mockDeskproConnector, never).createMessageWithAttachment(*, *, *, *, *)(*)
    }

    "return DeskproTicketResponseFailure if create failed" in new Setup {
      when(mockDeskproConnector.createMessage(*, *, *)(*)).thenReturn(Future.failed(UpstreamErrorResponse("Not found", 404)))
      when(mockDeskproConnector.updateTicketStatus(*, *)(*)).thenReturn(Future.successful(DeskproTicketUpdateSuccess))

      intercept[UpstreamErrorResponse] {
        await(underTest.createMessage(ticketId, CreateTicketResponseRequest(email, message, TicketStatus.AwaitingAgent)))
      }
    }
  }

  "deleteTicket" should {
    "return a success result" in new Setup {
      when(mockDeskproConnector.deleteTicket(*)(*)).thenReturn(Future.successful(DeskproTicketUpdateSuccess))

      val result = await(underTest.deleteTicket(ticketId))

      result shouldBe DeskproTicketUpdateSuccess
    }
  }

  "updateMessageAddAttachmentIfRequired" should {
    "return a success result when adding a new attachment to the message" in new Setup {
      val fileAttachment      = DeskproMessageFileAttachment(ticketId, messageId, fileReference, instant)
      when(mockMessageFileAttachmentRepo.fetchByFileReference(*)).thenReturn(Future.successful(Some(fileAttachment)))
      val existingAttachments = List(DeskproAttachmentResponse(12, DeskproBlobResponse(12345, "FGFK6657HHJHJ7987", "https://example.com/file01", "example.txt")))
      val attachmantsWrapper  = DeskproAttachmentsWrapperResponse(existingAttachments)
      when(mockDeskproConnector.getMessageAttachments(*, *)(*)).thenReturn(Future.successful(attachmantsWrapper))
      when(mockDeskproConnector.updateMessageAttachments(*, *, *, *, *)(*)).thenReturn(Future.successful(DeskproTicketMessageSuccess))

      val result = await(underTest.updateMessageAddAttachmentIfRequired(fileReference, blobDetails))

      result shouldBe DeskproTicketMessageSuccess
      verify(mockMessageFileAttachmentRepo).fetchByFileReference(eqTo(fileReference))
      verify(mockDeskproConnector).getMessageAttachments(eqTo(ticketId), eqTo(messageId))(*)
      verify(mockDeskproConnector).updateMessageAttachments(eqTo(ticketId), eqTo(messageId), eqTo(existingAttachments), eqTo(blobId), eqTo(blobAuth))(*)
    }

    "return a success result when message already has attachment" in new Setup {
      val fileAttachment     = DeskproMessageFileAttachment(ticketId, messageId, fileReference, instant)
      when(mockMessageFileAttachmentRepo.fetchByFileReference(*)).thenReturn(Future.successful(Some(fileAttachment)))
      val attachmantsWrapper =
        DeskproAttachmentsWrapperResponse(List(DeskproAttachmentResponse(12, DeskproBlobResponse(blobId, blobAuth, "https://example.com/file01", "example.txt"))))
      when(mockDeskproConnector.getMessageAttachments(*, *)(*)).thenReturn(Future.successful(attachmantsWrapper))

      val result = await(underTest.updateMessageAddAttachmentIfRequired(fileReference, blobDetails))

      result shouldBe DeskproTicketMessageSuccess
      verify(mockMessageFileAttachmentRepo).fetchByFileReference(eqTo(fileReference))
      verify(mockDeskproConnector).getMessageAttachments(eqTo(ticketId), eqTo(messageId))(*)
      verify(mockDeskproConnector, never).updateMessageAttachments(*, *, *, *, *)(*)
    }

    "return a success result when no file attachment record found (therefore nothing to do)" in new Setup {
      when(mockMessageFileAttachmentRepo.fetchByFileReference(*)).thenReturn(Future.successful(None))

      val result = await(underTest.updateMessageAddAttachmentIfRequired(fileReference, blobDetails))

      result shouldBe DeskproTicketMessageSuccess
      verify(mockMessageFileAttachmentRepo).fetchByFileReference(eqTo(fileReference))
      verify(mockDeskproConnector, never).getMessageAttachments(*, *)(*)
      verify(mockDeskproConnector, never).updateMessageAttachments(*, *, *, *, *)(*)
    }
  }
}
