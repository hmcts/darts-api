package uk.gov.hmcts.darts.arm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import feign.FeignException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.enums.GrantType;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.CloseResource")
class ArmApiServiceIntTest extends IntegrationBase {

    private static final String EXTERNAL_RECORD_ID = "7683ee65-c7a7-7343-be80-018b8ac13602";
    private static final String EXTERNAL_FILE_ID = "075987ea-b34d-49c7-b8db-439bfbe2496c";
    private static final String CABINET_ID = "100";
    private static final String ARM_ERROR_BODY = """
        { "itemId": "00000000-0000-0000-0000-000000000000", "cabinetId": 0, ...}
        """;

    ArmTokenRequest armTokenRequest;

    @Autowired
    private ArmApiService armApiService;

    @MockBean
    private ArmTokenClient armTokenClient;

    @Value("${darts.storage.arm-api.download-data-path}")
    private String downloadPath;

    @Value("${darts.storage.arm-api.update-metadata-path}")
    private String uploadPath;

    @Value("${darts.storage.arm-api.url}")
    private String baseArmPath;

    @Autowired
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        armTokenRequest = new ArmTokenRequest("some-username", "some-password", GrantType.PASSWORD.getValue());
        ArmTokenResponse armTokenResponse = getArmTokenResponse();
        String bearerToken = String.format("Bearer %s", armTokenResponse.getAccessToken());
        when(armTokenClient.getToken(armTokenRequest))
            .thenReturn(armTokenResponse);
        when(armTokenClient.availableEntitlementProfiles(bearerToken))
            .thenReturn(getAvailableEntitlementProfile());
        when(armTokenClient.selectEntitlementProfile(bearerToken, "some-profile-id"))
            .thenReturn(armTokenResponse);
    }

    @Test
    void updateMetadata() throws Exception {

        // Given
        var eventTimestamp = OffsetDateTime.parse("2024-01-31T11:29:56.101701Z").plusYears(7);

        var bearerAuth = "Bearer some-token";
        var reasonConf = "reason";
        var scoreConfId = 23;
        var updateMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(EXTERNAL_RECORD_ID)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(eventTimestamp)
                          .retConfScore(scoreConfId)
                          .retConfReason(reasonConf)
                          .build())
            .useGuidsForFields(false)
            .build();
        var updateMetadataResponse = UpdateMetadataResponse.builder()
            .itemId(UUID.fromString(EXTERNAL_RECORD_ID))
            .cabinetId(101)
            .objectId(UUID.fromString("4bfe4fc7-4e2f-4086-8a0e-146cc4556260"))
            .objectType(1)
            .fileName("UpdateMetadata-20241801-122819.json")
            .isError(false)
            .responseStatus(0)
            .responseStatusMessages(null)
            .build();

        String dummyResponse = objectMapper.writeValueAsString(updateMetadataResponse);
        String dummyRequest = objectMapper.writeValueAsString(updateMetadataRequest);

        stubFor(
            WireMock.post(urlPathMatching(uploadPath)).withRequestBody(equalToJson(dummyRequest))
                .willReturn(
                    aResponse().withHeader("Content-Type", "application/json").withBody(dummyResponse)
                        .withStatus(200)));

        // When
        var responseToTest = armApiService.updateMetadata(EXTERNAL_RECORD_ID, eventTimestamp, scoreConfId, reasonConf);

        // Then
        verify(armTokenClient).getToken(armTokenRequest);

        WireMock.verify(postRequestedFor(urlPathMatching(uploadPath))
            .withHeader("Authorization", new RegexPattern(bearerAuth))
                            .withRequestBody(equalToJson(dummyRequest)));

        assertEquals(updateMetadataResponse, responseToTest);
    }

    @Test
    void updateMetadataFailureResultsInAnExceptionBeingThrown() throws Exception {

        // Given
        var eventTimestamp = OffsetDateTime.parse("2024-01-31T11:29:56.101701Z").plusYears(7);
        var reasonConf = "reason";
        var scoreConfId = 23;
        var updateMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(EXTERNAL_RECORD_ID)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(eventTimestamp)
                          .retConfScore(scoreConfId)
                          .retConfReason(reasonConf)
                          .build())
            .useGuidsForFields(false)
            .build();

        String dummyRequest = objectMapper.writeValueAsString(updateMetadataRequest);

        stubFor(
            WireMock.post(urlPathMatching(uploadPath)).withRequestBody(equalToJson(dummyRequest))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(ARM_ERROR_BODY)
                        .withStatus(400)));

        // When/Then
        assertThrows(FeignException.class, () -> armApiService.updateMetadata(EXTERNAL_RECORD_ID, eventTimestamp, scoreConfId, reasonConf));
    }

    @Test
    @SneakyThrows
    void downloadArmData() {
        // Given
        byte[] binaryData = "some binary content".getBytes();

        stubFor(
            WireMock.get(urlPathMatching(getDownloadPath(downloadPath, CABINET_ID, EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID)))
                .willReturn(
                    aResponse().withBody(binaryData)
                    .withStatus(200)));

        // When
        DownloadResponseMetaData downloadResponseMetaData = armApiService.downloadArmData(EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID);

        // Then
        verify(armTokenClient).getToken(armTokenRequest);

        WireMock.verify(getRequestedFor(urlPathMatching(getDownloadPath(downloadPath, CABINET_ID, EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID)))
                    .withHeader("Authorization", new RegexPattern("Bearer some-token")));

        assertThat(downloadResponseMetaData.getInputStream().readAllBytes()).isEqualTo(binaryData);
    }

    @Test
    @SneakyThrows
    void downloadFailureExceptionFromFeign() {
        // Given
        stubFor(
            WireMock.get(urlPathMatching(getDownloadPath(downloadPath, CABINET_ID, EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID)))
                .willReturn(
                    aResponse().withStatus(400)));
        // When
        FileNotDownloadedException exception
            = assertThrows(FileNotDownloadedException.class, () -> armApiService.downloadArmData(EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID));

        // Then
        verify(armTokenClient).getToken(armTokenRequest);
        assertTrue(exception.getMessage().contains(CABINET_ID));
        assertTrue(exception.getMessage().contains(EXTERNAL_RECORD_ID));
        assertTrue(exception.getMessage().contains(EXTERNAL_FILE_ID));
    }

    private AvailableEntitlementProfile getAvailableEntitlementProfile() {
        List<AvailableEntitlementProfile.Profiles> profiles = List.of(AvailableEntitlementProfile.Profiles.builder()
                        .profileName("some-profile-name")
                        .profileId("some-profile-id")
                        .build());

        return AvailableEntitlementProfile.builder()
            .profiles(profiles)
            .isError(false)
            .build();
    }

    private ArmTokenResponse getArmTokenResponse() {
        return ArmTokenResponse.builder()
            .accessToken("some-token")
            .tokenType("Bearer")
            .expiresIn("3600")
            .build();
    }

    private String getDownloadPath(String downloadPath, String cabinetId, String recordId, String fileId) {
        return downloadPath.replace("{cabinet_id}", cabinetId).replace("{record_id}", recordId).replace("{file_id}", fileId);
    }
}