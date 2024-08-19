package uk.gov.hmrc.apiplatformdeskpro.connector

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status._

import uk.gov.hmrc.apiplatformdeskpro.utils.AsyncHmrcSpec
import uk.gov.hmrc.apiplatformdeskpro.utils.WireMockSupport
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import uk.gov.hmrc.apiplatformdeskpro.utils.ConfigBuilder
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import play.api.Mode
import java.time.LocalDate
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class DeveloperConnectorISpec extends AsyncHmrcSpec
    with WireMockSupport
    with GuiceOneServerPerSuite
    with ConfigBuilder 
    with FixedClock{

  
    override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(stubConfig(wireMockPort))
      .in(Mode.Test)
      .build()

      trait Setup {
            
        val objInTest: DeveloperConnector = app.injector.instanceOf[DeveloperConnector]


        def stubGetDevelopersSuccess() = {
            stubFor(
                get(urlMatching("/developers"))
                .withQueryParam("status",equalTo("VERIFIED"))
                .withQueryParam("limit",equalTo("100"))
                .withQueryParam("createdAfter",equalTo(LocalDate.now()))
                .willReturn(
                    aResponse()
                     .withBody("""[{"email":"bob.hope@business.com","userId":"6f0c4afa-1e10-44a1-8538-f5d436e7b615", "firstName":"Bob", "lastName":"Hope"},{"email":"emu.hull@business.com","userId":"a86857b3-732a-41c7-8d37-d29d5f416404", "firstName":"Emu", "lastName":"Hull"}]""")
           
                    .withStatus(OK)
                )
            )
        }

        def stubCreateTicketUnauthorised() = {
            stubFor(
                post(urlMatching("/api/v2/tickets"))
                .willReturn(
                    aResponse()
                    .withStatus(UNAUTHORIZED)
                )
            )
        }

        def stubCreateTicketInternalServerError() = {
            stubFor(
                post(urlMatching("/api/v2/tickets"))
                .willReturn(
                    aResponse()
                    .withStatus(INTERNAL_SERVER_ERROR)
                )
            )
        }
    }

     "DeveloperConnector" should {
        
     } 
}
