package uk.gov.hmcts.darts.arm.service;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.client.ArmApiClient;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.CloseResource")
class ArmApiServiceIntTest extends IntegrationBase {

    private static final String EXTERNAL_RECORD_ID = "7683ee65-c7a7-7343-be80-018b8ac13602";
    private static final String EXTERNAL_FILE_ID = "075987ea-b34d-49c7-b8db-439bfbe2496c";
    private static final String CABINET_ID = "100";

    ArmTokenRequest armTokenRequest;

    @Autowired
    private ArmApiService armApiService;

    @MockBean
    private ArmTokenClient armTokenClient;
    @MockBean
    private ArmApiClient armApiClient;

    @BeforeEach
    void setup() {
        armTokenRequest = new ArmTokenRequest("some-username", "some-password", "password");
        when(armTokenClient.getToken(armTokenRequest))
            .thenReturn(ArmTokenResponse.builder()
                            .accessToken("some-token")
                            .tokenType("Bearer")
                            .expiresIn("3600")
                            .build());
    }

    @Test
    void updateMetadata() {
        // Given
        var eventTimestamp = OffsetDateTime.parse("2024-01-31T11:29:56.101701Z").plusYears(7);

        var bearerAuth = "Bearer some-token";
        var updateMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(EXTERNAL_RECORD_ID)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(eventTimestamp)
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
        when(armApiClient.updateMetadata(
            bearerAuth,
            updateMetadataRequest
        )).thenReturn(updateMetadataResponse);

        // When
        var responseToTest = armApiService.updateMetadata(EXTERNAL_RECORD_ID, eventTimestamp);

        // Then
        verify(armTokenClient).getToken(armTokenRequest);
        verify(armApiClient).updateMetadata(bearerAuth, updateMetadataRequest);
        assertEquals(updateMetadataResponse, responseToTest);
    }

    @Test
    @SneakyThrows
    void downloadArmData() {

        // Given
        byte[] serverResponse = "some binary content".getBytes();
        when(armApiClient.downloadArmData(any(), any(), any(), any())).thenReturn(serverResponse);

        // When
        InputStream inputStreamResult = armApiService.downloadArmData(EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID);

        // Then
        verify(armTokenClient).getToken(armTokenRequest);
        verify(armApiClient).downloadArmData("Bearer some-token", CABINET_ID, EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID);
        assertThat(inputStreamResult.readAllBytes()).isEqualTo(serverResponse);
    }

}
