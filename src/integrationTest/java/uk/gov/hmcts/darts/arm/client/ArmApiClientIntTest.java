package uk.gov.hmcts.darts.arm.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@TestPropertySource(properties = {
    "darts.storage.arm-api.url=http://localhost:8080"
})
class ArmApiClientIntTest extends IntegrationBase {

    @Autowired
    private ArmApiClient armApiClient;
    @Autowired
    private WireMockServer wireMockServer;

    private static final String UPDATE_METADATA_PATH = "/api/v3/UpdateMetadata";

    @Test
    void updateMetadataShouldSucceedIfServerReturns200Success() {
        // Given
        var bearerAuth = "Bearer some-token";
        var externalRecordId = "7683ee65-c7a7-7343-be80-018b8ac13602";
        var eventTimestamp = OffsetDateTime.parse("2024-01-31T11:29:56.101701Z").plusYears(7);

        stubFor(
            WireMock.post(urlEqualTo(UPDATE_METADATA_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", "application/json")
                        .withBody("""
                                      {
                                          "itemId": "7683ee65-c7a7-7343-be80-018b8ac13602",
                                          "cabinetId": 101,
                                          "objectId": "4bfe4fc7-4e2f-4086-8a0e-146cc4556260",
                                          "objectType": 1,
                                          "fileName": "UpdateMetadata-20241801-122819.json",
                                          "isError": false,
                                          "responseStatus": 0,
                                          "responseStatusMessages": null
                                      }
                                      """
                        )
                        .withStatus(200)));

        var updateMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(eventTimestamp)
                          .build())
            .useGuidsForFields(false)
            .build();

        // When
        ResponseEntity<UpdateMetadataResponse> updateMetadataResponse = armApiClient.updateMetadata(bearerAuth, updateMetadataRequest);

        // Then
        wireMockServer.verify(postRequestedFor(urlEqualTo(UPDATE_METADATA_PATH))
                                  .withHeader(AUTHORIZATION, equalTo(bearerAuth))
                                  .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                                  .withRequestBody(
                                      matchingJsonPath("$.UseGuidsForFields", equalTo("false"))
                                          .and(matchingJsonPath("$.manifest.event_date", equalTo(eventTimestamp.toString())))
                                          .and(matchingJsonPath("$.itemId", equalTo(externalRecordId)))
                                  ));

        assertEquals(OK, updateMetadataResponse.getStatusCode());
        var body = updateMetadataResponse.getBody();
        assertEquals(UUID.fromString(externalRecordId), body.getItemId());
        assertFalse(body.isError());
    }

}
