package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmApiClient;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.rpo.EmptyRpoRequest;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArmClientService;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmApiServiceImplTest {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

    @Mock
    private ArmApiConfigurationProperties armApiConfigurationProperties;
    @Mock
    private ArmTokenClient armTokenClient;
    @Mock
    private ArmApiClient armApiClient;

    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;


    private ArmApiServiceImpl armApiService;

    @BeforeEach
    void setUp() {
        ArmClientService armClientService = new ArmClientServiceImpl(armTokenClient, armApiClient, null);
        armApiService = new ArmApiServiceImpl(armApiConfigurationProperties, armDataManagementConfiguration, armClientService);
    }

    @Test
    void updateMetadata_Success() {
        String bearerToken = "this is the bearer token";
        String username = "username";
        String password = "pass";
        String armProfile = "profile";
        String armProfileId = "profileId";
        String externalRecordId = "myexternalrecord";
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        var refConfScore = RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;
        String refConfReason = "reason";

        when(armApiConfigurationProperties.getArmUsername()).thenReturn(username);
        when(armApiConfigurationProperties.getArmPassword()).thenReturn(password);
        when(armApiConfigurationProperties.getArmServiceProfile()).thenReturn(armProfile);
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);

        ArmTokenRequest tokenRequest = ArmTokenRequest.builder().username(username).password(password).build();
        ArmTokenResponse response = ArmTokenResponse.builder().accessToken(bearerToken).build();

        when(armTokenClient.getToken(tokenRequest)).thenReturn(response);

        AvailableEntitlementProfile.Profiles profiles = AvailableEntitlementProfile.Profiles.builder().profileId(armProfileId).profileName(armProfile).build();
        AvailableEntitlementProfile profile = Mockito.mock(AvailableEntitlementProfile.class);
        when(profile.getProfiles()).thenReturn(List.of(profiles));
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        when(armTokenClient.availableEntitlementProfiles("Bearer " + bearerToken, emptyRpoRequest)).thenReturn(profile);
        when(armTokenClient.selectEntitlementProfile("Bearer " + bearerToken, armProfileId, emptyRpoRequest)).thenReturn(response);

        UpdateMetadataRequest expectedMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(formatDateTime(offsetDateTime))
                          .retConfReason(refConfReason)
                          .retConfScore(refConfScore.getId())
                          .build())
            .useGuidsForFields(false)
            .build();

        armApiService.updateMetadata(externalRecordId, offsetDateTime, refConfScore, refConfReason);

        verify(armApiClient, times(1)).updateMetadata(eq("Bearer " + bearerToken), eq(expectedMetadataRequest));
    }

    @Test
    void updateMetadata_WithNullRetentionConfidenceScoreAndReason() {
        // given
        String bearerToken = "this is the bearer token";
        String username = "username";
        String password = "pass";
        String armProfile = "profile";
        String armProfileId = "profileId";
        String externalRecordId = "myexternalrecord";
        OffsetDateTime offsetDateTime = OffsetDateTime.now();

        when(armApiConfigurationProperties.getArmUsername()).thenReturn(username);
        when(armApiConfigurationProperties.getArmPassword()).thenReturn(password);
        when(armApiConfigurationProperties.getArmServiceProfile()).thenReturn(armProfile);
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);

        ArmTokenRequest tokenRequest = ArmTokenRequest.builder().username(username).password(password).build();
        ArmTokenResponse response = ArmTokenResponse.builder().accessToken(bearerToken).build();

        when(armTokenClient.getToken(tokenRequest)).thenReturn(response);

        AvailableEntitlementProfile.Profiles profiles = AvailableEntitlementProfile.Profiles.builder().profileId(armProfileId).profileName(armProfile).build();
        AvailableEntitlementProfile profile = Mockito.mock(AvailableEntitlementProfile.class);
        when(profile.getProfiles()).thenReturn(List.of(profiles));
        EmptyRpoRequest emptyRpoRequest = EmptyRpoRequest.builder().build();
        when(armTokenClient.availableEntitlementProfiles("Bearer " + bearerToken, emptyRpoRequest)).thenReturn(profile);
        when(armTokenClient.selectEntitlementProfile("Bearer " + bearerToken, armProfileId, emptyRpoRequest)).thenReturn(response);

        UpdateMetadataRequest expectedMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(formatDateTime(offsetDateTime))
                          .retConfReason(null)
                          .retConfScore(null)
                          .build())
            .useGuidsForFields(false)
            .build();

        // when
        armApiService.updateMetadata(externalRecordId, offsetDateTime, null, null);

        // then
        verify(armApiClient, times(1)).updateMetadata(eq("Bearer " + bearerToken), eq(expectedMetadataRequest));
    }

    @Test
    void formatDateTime_ShouldReturnFormattedDateTime_WhenOffsetDateTimeIsNotNull() {
        // given
        OffsetDateTime offsetDateTime = OffsetDateTime.parse("2023-01-01T12:00:00Z");
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);

        String expectedFormattedDateTime = "2023-01-01T12:00:00.000Z";

        // when
        String formattedDateTime = armApiService.formatDateTime(offsetDateTime);

        // then
        assertEquals(expectedFormattedDateTime, formattedDateTime);
    }

    @Test
    void formatDateTime_ShouldReturnNull_WhenOffsetDateTimeIsNull() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);

        // when
        String formattedDateTime = armApiService.formatDateTime(null);

        // then
        assertNull(formattedDateTime);
    }

    private String formatDateTime(OffsetDateTime offsetDateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        return offsetDateTime.format(dateTimeFormatter);
    }
}