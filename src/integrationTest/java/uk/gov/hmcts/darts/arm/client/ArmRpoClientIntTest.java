package uk.gov.hmcts.darts.arm.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.util.AssertionErrors.assertEquals;

@TestPropertySource(properties = {
    "darts.storage.arm-api.url=http://localhost:${wiremock.server.port}"
})
class ArmRpoClientIntTest extends IntegrationBaseWithWiremock {

    private static final String GET_RECORD_MANAGEMENT_MATTER_PATH = "/api/v1/getRecordManagementMatter";

    @Autowired
    private ArmRpoClient armRpoClient;

    //@Disabled("This test is failing other wiremock tests")
    @Test
    void getRecordManagementMatterShouldSucceedIfServerReturns200Success() {
        // given
        var bearerAuth = "Bearer some-token";

        stubFor(
            WireMock.post(urlEqualTo(GET_RECORD_MANAGEMENT_MATTER_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""
                                      {
                                           "recordManagementMatter": {
                                               "matterCategory": 3,
                                               "matterID": "cb70c7fa-8972-4400-af1d-ff5dd76d2104",
                                               "name": "Records Management",
                                               "isQuickSearch": false,
                                               "isUsedForRM": true,
                                               "description": "Records Management",
                                               "createdDate": "2022-12-14T08:50:55.75+00:00",
                                               "type": 1,
                                               "status": 0,
                                               "userID": null,
                                               "backgroundJobID": null,
                                               "isClosed": false
                                           },
                                           "status": 200,
                                           "demoMode": false,
                                           "isError": false,
                                           "responseStatus": 0,
                                           "responseStatusMessages": null,
                                           "exception": null,
                                           "message": null
                                       }
                                      """
                        )
                        .withStatus(200)));
        // when
        var getRecordManagementMatterResponse = armRpoClient.getRecordManagementMatter(bearerAuth);

        // then
        verify(postRequestedFor(urlEqualTo(GET_RECORD_MANAGEMENT_MATTER_PATH))
                   .withHeader(AUTHORIZATION, equalTo(bearerAuth))
                   .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
        );

        assertEquals("Failed to get matter id", "cb70c7fa-8972-4400-af1d-ff5dd76d2104",
                     getRecordManagementMatterResponse.getRecordManagementMatter().getMatterId());
    }

}
