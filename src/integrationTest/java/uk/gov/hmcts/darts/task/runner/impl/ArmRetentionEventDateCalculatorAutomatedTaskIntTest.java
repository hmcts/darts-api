package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.client.ArmApiClient;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;

class ArmRetentionEventDateCalculatorAutomatedTaskIntTest extends PostgresIntegrationBase {
    private static final String BEARER_TOKEN = "bearer";

    private static final OffsetDateTime DOCUMENT_RETENTION_DATE_TIME =
        OffsetDateTime.of(2023, 6, 10, 10, 50, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime RETENTION_DATE_TIME =
        OffsetDateTime.of(1923, 6, 10, 10, 50, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime START_TIME =
        OffsetDateTime.of(2023, 6, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime END_TIME =
        OffsetDateTime.of(2023, 6, 10, 10, 45, 0, 0, ZoneOffset.UTC);
    private static final int EVENT_DATE_ADJUSTMENT_YEARS = 100;

    @Autowired
    private AuthorisationStub authorisationStub;
    @Autowired
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    @MockitoBean
    private ArmApiClient armApiClient;
    @MockitoBean
    private ArmTokenClient armTokenClient;

    //@Autowired
    //private ArmRetentionEventDateCalculator armRetentionEventDateCalculator;

    @Autowired
    private ArmRetentionEventDateCalculatorAutomatedTask armRetentionEventDateCalculatorAutomatedTask;

    @Test
    void runTask_ShouldUpdateRetentionEventDate() {
        // given
        final String confidenceReason = "reason";
        final RetentionConfidenceScoreEnum confidenceScore = CASE_PERFECTLY_CLOSED;
        final String externalRecordId = "recordId";

        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimalHearing();

        MediaEntity savedMedia = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                START_TIME,
                END_TIME,
                1,
                "mp2",
                confidenceScore,
                confidenceReason
            ));
        savedMedia.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        dartsPersistence.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().media(savedMedia)
            .status(dartsDatabase.getExternalObjectDirectoryStub().getStatus(STORED))
            .externalLocationType(dartsDatabase.getExternalObjectDirectoryStub().getLocation(ARM))
            .externalLocation(UUID.randomUUID().toString()).build().getEntity();

        armEod.setExternalRecordId(externalRecordId);
        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsPersistence.save(armEod);

        UpdateMetadataResponse response = UpdateMetadataResponse.builder().responseStatus(200).isError(false).build();
        ArmTokenRequest armTokenRequest = createTokenRequest();
        when(armApiClient.updateMetadata(any(), any())).thenReturn(response);

        ArmTokenResponse armTokenResponse = createArmTokenResponse();
        when(armTokenClient.getToken(armTokenRequest)).thenReturn(armTokenResponse);

        AvailableEntitlementProfile availableEntitlementProfile = createAvailableEntitlementProfile();
        when(armTokenClient.availableEntitlementProfiles(any(), any())).thenReturn(availableEntitlementProfile);

        when(armTokenClient.selectEntitlementProfile(any(), any(), any())).thenReturn(armTokenResponse);

        armRetentionEventDateCalculatorAutomatedTask.preRunTask();

        // when
        armRetentionEventDateCalculatorAutomatedTask.runTask();

        // then
        var persistedEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        assertFalse(persistedEod.isUpdateRetention());
        assertEquals(0, persistedEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

        UpdateMetadataRequest expectedMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(formatDateTime(savedMedia.getRetainUntilTs().minusYears(EVENT_DATE_ADJUSTMENT_YEARS)))
                          .retConfReason(confidenceReason)
                          .retConfScore(confidenceScore.getId())
                          .build())
            .useGuidsForFields(false)
            .build();
        verify(armApiClient).updateMetadata("Bearer " + BEARER_TOKEN, expectedMetadataRequest);

    }

    @Test
    void runTask_ShouldNotUpdateRetentionEventDate_WhenInterruptedExceptionIsThrown() {
        // given
        final String confidenceReason = "reason";
        final RetentionConfidenceScoreEnum confidenceScore = CASE_PERFECTLY_CLOSED;
        final String externalRecordId = "recordId";

        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimalHearing();

        MediaEntity savedMedia = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                START_TIME,
                END_TIME,
                1,
                "mp2",
                confidenceScore,
                confidenceReason
            ));
        savedMedia.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        dartsPersistence.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().media(savedMedia)
            .status(dartsDatabase.getExternalObjectDirectoryStub().getStatus(STORED))
            .externalLocationType(dartsDatabase.getExternalObjectDirectoryStub().getLocation(ARM))
            .externalLocation(UUID.randomUUID().toString()).build().getEntity();

        armEod.setExternalRecordId(externalRecordId);
        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsPersistence.save(armEod);

        ArmTokenResponse armTokenResponse = createArmTokenResponse();
        when(armTokenClient.getToken(any())).thenReturn(armTokenResponse);
        // Simulate InterruptedException
        doAnswer(invocation -> {
            throw new InterruptedException("Simulated interruption");
        }).when(armTokenClient).getToken(any());

        armRetentionEventDateCalculatorAutomatedTask.preRunTask();

        // when
        armRetentionEventDateCalculatorAutomatedTask.runTask();

        // then
        var persistedEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        assertTrue(persistedEod.isUpdateRetention());
        // Verify that the updateMetadata method was not called
        verifyNoInteractions(armApiClient);
    }

    private AvailableEntitlementProfile createAvailableEntitlementProfile() {
        List<AvailableEntitlementProfile.Profiles> profiles = List.of(AvailableEntitlementProfile.Profiles.builder()
                                                                          .profileName("some-profile-name")
                                                                          .profileId("some-profile-id")
                                                                          .build());

        return AvailableEntitlementProfile.builder()
            .profiles(profiles)
            .isError(false)
            .build();
    }

    private String formatDateTime(OffsetDateTime offsetDateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        return offsetDateTime.format(dateTimeFormatter);
    }

    private static ArmTokenRequest createTokenRequest() {
        return ArmTokenRequest.builder()
            .username("some-username")
            .password("some-password")
            .build();
    }

    private static ArmTokenResponse createArmTokenResponse() {
        return ArmTokenResponse.builder()
            .accessToken(BEARER_TOKEN)
            .tokenType("Bearer")
            .expiresIn("3600")
            .build();
    }
}
