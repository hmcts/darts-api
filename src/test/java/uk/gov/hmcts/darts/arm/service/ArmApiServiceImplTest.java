package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmApiClient;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.enums.GrantType;
import uk.gov.hmcts.darts.arm.service.impl.ArmApiServiceImpl;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmApiServiceImplTest {
    @Mock
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    @Mock
    private ArmTokenClient armTokenClient;

    @Mock
    private ArmApiClient armApiClient;

    @InjectMocks
    private ArmApiServiceImpl armApiService;

    @Test
    void testArmUpdateCall() {
        String bearerToken = "this is the bearer token";
        String username = "username";
        String password = "pass";
        String armProfile = "profile";
        String armProfileId = "profileId";
        String externalRecordId = "myexternalrecord";
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        Integer refConfScope = 2;
        String  refConfReason = "reason";

        when(armApiConfigurationProperties.getArmUsername()).thenReturn(username);
        when(armApiConfigurationProperties.getArmPassword()).thenReturn(password);
        when(armApiConfigurationProperties.getArmServiceProfile()).thenReturn(armProfile);

        ArmTokenRequest tokenRequest = new ArmTokenRequest(username, password, GrantType.PASSWORD.getValue());
        ArmTokenResponse response =  ArmTokenResponse.builder().accessToken(bearerToken).build();

        when(armTokenClient.getToken(tokenRequest)).thenReturn(response);

        AvailableEntitlementProfile.Profiles profiles = AvailableEntitlementProfile.Profiles.builder().profileId(armProfileId).profileName(armProfile).build();
        AvailableEntitlementProfile profile = Mockito.mock(AvailableEntitlementProfile.class);
        when(profile.getProfiles()).thenReturn(List.of(profiles));

        when(armTokenClient.availableEntitlementProfiles("Bearer " + bearerToken)).thenReturn(profile);
        when(armTokenClient.selectEntitlementProfile("Bearer " + bearerToken, armProfileId)).thenReturn(response);

        UpdateMetadataRequest expectedMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(offsetDateTime)
                          .retConfReason(refConfReason)
                          .retConfScore(refConfScope)
                          .build())
            .useGuidsForFields(false)
            .build();

        armApiService.updateMetadata(externalRecordId, offsetDateTime, refConfScope, refConfReason);

        Mockito.verify(armApiClient, times(1)).updateMetadata(eq("Bearer " + bearerToken), eq(expectedMetadataRequest));
    }
}