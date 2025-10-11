package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.client.version.fivetwo.ArmApiBaseClient;
import uk.gov.hmcts.darts.arm.client.version.fivetwo.ArmAuthClient;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
    "darts.storage.arm-api.enable-arm-v5-2-upgrade=true"
})
class ArmApiServiceVersionFiveTwoIntTest extends IntegrationBase {

    private static final String EXTERNAL_RECORD_ID = "7683ee65-c7a7-7343-be80-018b8ac13602";
    private static final String EXTERNAL_FILE_ID = "075987ea-b34d-49c7-b8db-439bfbe2496c";
    private static final String CABINET_ID = "100";
    private static final String ARM_ERROR_BODY = """
        { "itemId": "00000000-0000-0000-0000-000000000000", "cabinetId": 0, ...}
        """;
    private static final String BINARY_CONTENT = "some binary content";

    @Value("${darts.storage.arm-api.version5-2.api.download-data-path}")
    private String downloadPath;

    @Value("${darts.storage.arm-api.version5-2.api.update-metadata-path}")
    private String uploadPath;

    @Autowired
    private ArmApiService armApiService;

    @MockitoBean
    private ArmAuthClient armAuthClient;
    @MockitoBean
    private ArmApiBaseClient armApiBaseClient;

    @BeforeEach
    void setup() {
        ArmTokenRequest armTokenRequest = ArmTokenRequest.builder()
            .username("some-username")
            .password("some-password")
            .build();
        ArmTokenResponse armTokenResponse = getArmTokenResponse();
        String bearerToken = String.format("Bearer %s", armTokenResponse.getAccessToken());
        when(armAuthClient.getToken(armTokenRequest))
            .thenReturn(armTokenResponse);
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        when(armApiBaseClient.availableEntitlementProfiles(bearerToken, emptyRpoRequest))
            .thenReturn(getAvailableEntitlementProfile());
        when(armApiBaseClient.selectEntitlementProfile(bearerToken, "some-profile-id", emptyRpoRequest))
            .thenReturn(armTokenResponse);
    }

    @Test
    void updateMetadata_WithNanoSeconds() {
        // Given
        var eventTimestamp = OffsetDateTime.parse("2024-01-31T11:29:56.101701Z").plusYears(7);

        var bearerAuth = "Bearer some-token";
        var reasonConf = "reason";
        var scoreConfId = RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;
        var updateMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(EXTERNAL_RECORD_ID)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(formatDateTime(eventTimestamp))
                          .retConfScore(scoreConfId.getId())
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

        when(armApiBaseClient.updateMetadata(bearerAuth, updateMetadataRequest)).thenReturn(updateMetadataResponse);

        // when
        var responseToTest = armApiService.updateMetadata(EXTERNAL_RECORD_ID, eventTimestamp, scoreConfId, reasonConf);

        // then
        verify(armAuthClient).getToken(any());

        assertEquals(updateMetadataResponse, responseToTest);
    }

    @Test
    void updateMetadata_WithZeroTimes() {

        // Given
        var eventTimestamp = OffsetDateTime.parse("2024-01-31T00:00:00.00Z").plusYears(7);

        var bearerAuth = "Bearer some-token";
        var reasonConf = "reason";
        var scoreConfId = RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;
        var updateMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(EXTERNAL_RECORD_ID)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(formatDateTime(eventTimestamp))
                          .retConfScore(scoreConfId.getId())
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

        when(armApiBaseClient.updateMetadata(bearerAuth, updateMetadataRequest)).thenReturn(updateMetadataResponse);

        // When
        var responseToTest = armApiService.updateMetadata(EXTERNAL_RECORD_ID, eventTimestamp, scoreConfId, reasonConf);

        // Then
        verify(armAuthClient).getToken(any());

        assertEquals(updateMetadataResponse, responseToTest);
    }

    @Test
    void downloadArmData_ShouldDownloadData() throws FileNotDownloadedException, IOException {
        // Given
        byte[] binaryData = BINARY_CONTENT.getBytes();

        when(armApiBaseClient.downloadArmData(any(), any(), any(), any())).thenReturn(
            feign.Response.builder()
                .status(200)
                .reason("OK")
                .headers(java.util.Collections.emptyMap()) // Fix: use empty map instead of null
                .body(binaryData)
                .request(feign.Request.create(
                    feign.Request.HttpMethod.GET,
                    "some-url", java.util.Collections.emptyMap(), null, null, null))
                .build());

        // When
        try (DownloadResponseMetaData downloadResponseMetaData = armApiService.downloadArmData(EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID)) {

            // Then
            verify(armAuthClient).getToken(any());

            assertThat(downloadResponseMetaData.getResource().getInputStream().readAllBytes()).isEqualTo(binaryData);
        }
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

    private String formatDateTime(OffsetDateTime offsetDateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        return offsetDateTime.format(dateTimeFormatter);
    }
}
