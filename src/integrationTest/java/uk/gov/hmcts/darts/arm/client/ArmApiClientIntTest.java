package uk.gov.hmcts.darts.arm.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@TestPropertySource(properties = {
    "darts.storage.arm-api.url=http://localhost:${wiremock.server.port}"
})
class ArmApiClientIntTest extends IntegrationBaseWithWiremock {

    private static final String EXTERNAL_RECORD_ID = "7683ee65-c7a7-7343-be80-018b8ac13602";
    private static final String EXTERNAL_FILE_ID = "075987ea-b34d-49c7-b8db-439bfbe2496c";
    private static final String CABINET_ID = "100";
    private static final String UPDATE_METADATA_PATH = "/api/v3/UpdateMetadata";
    private static final String DOWNLOAD_ARM_DATA_PATH = "/api/v1/downloadBlob/\\S+/\\S+/\\S+";

    @Autowired
    private ArmApiClient armApiClient;

    @Test
    void updateMetadata_ShouldSucceed_WhenServerReturns200Success() {
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
                          .eventDate(formatDateTime(eventTimestamp))
                          .build())
            .useGuidsForFields(false)
            .build();

        // When
        UpdateMetadataResponse updateMetadataResponse = armApiClient.updateMetadata(bearerAuth, updateMetadataRequest);

        // Then
        verify(postRequestedFor(urlEqualTo(UPDATE_METADATA_PATH))
                   .withHeader(AUTHORIZATION, equalTo(bearerAuth))
                   .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                   .withRequestBody(
                       matchingJsonPath("$.UseGuidsForFields", equalTo("false"))
                           .and(matchingJsonPath("$.manifest.event_date", equalTo(formatDateTime(eventTimestamp))))
                           .and(matchingJsonPath("$.itemId", equalTo(externalRecordId)))
                   ));

        assertEquals(UUID.fromString(externalRecordId), updateMetadataResponse.getItemId());
    }

    @Test
    void updateMetadata_ShouldSucceed_WithZeroTimes() {
        // Given
        var bearerAuth = "Bearer some-token";
        var externalRecordId = "7683ee65-c7a7-7343-be80-018b8ac13602";
        var eventTimestamp = OffsetDateTime.parse("2024-01-31T00:00:00.00000Z").plusYears(7);

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
                          .eventDate(formatDateTime(eventTimestamp))
                          .build())
            .useGuidsForFields(false)
            .build();

        // When
        UpdateMetadataResponse updateMetadataResponse = armApiClient.updateMetadata(bearerAuth, updateMetadataRequest);

        // Then
        verify(postRequestedFor(urlEqualTo(UPDATE_METADATA_PATH))
                   .withHeader(AUTHORIZATION, equalTo(bearerAuth))
                   .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                   .withRequestBody(
                       matchingJsonPath("$.UseGuidsForFields", equalTo("false"))
                           .and(matchingJsonPath("$.manifest.event_date", equalTo(formatDateTime(eventTimestamp))))
                           .and(matchingJsonPath("$.itemId", equalTo(externalRecordId)))
                   ));

        assertEquals(UUID.fromString(externalRecordId), updateMetadataResponse.getItemId());
    }

    @Test
    @SneakyThrows
    void downloadArmData_ShouldSucceed_WhenServerReturns200Success() {
        // Given
        stubFor(
            WireMock.get(urlPathMatching(DOWNLOAD_ARM_DATA_PATH))
                .willReturn(
                    aResponse()
                        .withHeader("Content-type", MediaType.APPLICATION_OCTET_STREAM_VALUE)
                        .withBodyFile("testAudio.mp3")
                        .withStatus(200)));

        // When
        try (feign.Response response = armApiClient.downloadArmData("Bearer token", CABINET_ID, EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID)) {

            //Then
            try (InputStream expectedInputStream = Files.newInputStream(Paths.get("src/integrationTest/resources/wiremock/__files/testAudio.mp3"))) {
                assertTrue(IOUtils.contentEquals(response.body().asInputStream(), expectedInputStream));
            }
        }
    }

    private String formatDateTime(OffsetDateTime offsetDateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        return offsetDateTime.format(dateTimeFormatter);
    }
}