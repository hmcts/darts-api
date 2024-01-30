package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.darts.arm.client.ArmApiClient;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArmApiServiceIntTest extends IntegrationBase {

    @Autowired
    private ArmApiService armApiService;

    @MockBean
    private ArmTokenClient armTokenClient;
    @MockBean
    private ArmApiClient armApiClient;

    @Test
    void updateMetadata() {
        // Given
        var externalRecordId = "7683ee65-c7a7-7343-be80-018b8ac13602";
        var eventTimestamp = OffsetDateTime.now().plusYears(7);

        var armTokenRequest = new ArmTokenRequest("some-username", "some-password", "password");
        when(armTokenClient.getToken(armTokenRequest))
            .thenReturn(ArmTokenResponse.builder()
                            .accessToken("some-token")
                            .tokenType("Bearer")
                            .expiresIn("3600")
                            .build());

        var bearerAuth = "Bearer some-token";
        var updateMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(eventTimestamp)
                          .build())
            .useGuidsForFields(false)
            .build();
        var updateMetadataResponse = UpdateMetadataResponse.builder()
            .itemId(UUID.fromString(externalRecordId))
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
        )).thenReturn(ResponseEntity.ok(updateMetadataResponse));

        // When
        var responseToTest = armApiService.updateMetadata(externalRecordId, eventTimestamp);

        // Then
        verify(armTokenClient).getToken(armTokenRequest);
        verify(armApiClient).updateMetadata(bearerAuth, updateMetadataRequest);
        assertEquals(updateMetadataResponse, responseToTest);
    }

}
