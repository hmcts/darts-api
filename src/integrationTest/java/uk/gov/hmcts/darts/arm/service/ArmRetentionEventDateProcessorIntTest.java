package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.client.ArmApiClient;
import uk.gov.hmcts.darts.arm.client.ArmTokenClient;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenRequest;
import uk.gov.hmcts.darts.arm.client.model.ArmTokenResponse;
import uk.gov.hmcts.darts.arm.client.model.AvailableEntitlementProfile;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataRequest;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.component.ArmRetentionEventDateCalculator;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.enums.GrantType;
import uk.gov.hmcts.darts.arm.service.impl.ArmRetentionEventDateProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.test.common.data.MediaTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Slf4j
class ArmRetentionEventDateProcessorIntTest extends IntegrationBase {

    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 6, 10, 10, 0, 0);

    private static final OffsetDateTime DOCUMENT_RETENTION_DATE_TIME =
        OffsetDateTime.of(2023, 6, 10, 10, 50, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime RETENTION_DATE_TIME =
        OffsetDateTime.of(1923, 6, 10, 10, 50, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime START_TIME =
        OffsetDateTime.of(2023, 6, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime END_TIME =
        OffsetDateTime.of(2023, 6, 10, 10, 45, 0, 0, ZoneOffset.UTC);
    public static final int EVENT_DATE_ADJUSTMENT_YEARS = 100;

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Autowired
    private ArmRetentionEventDateCalculator armRetentionEventDateCalculator;

    @MockBean
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private AuthorisationStub authorisationStub;

    @MockBean
    private ArmTokenClient armTokenClient;

    @Autowired
    private ArmApiConfigurationProperties armApiConfigurationProperties;

    @MockBean
    private ArmApiClient armApiClient;


    private ArmRetentionEventDateProcessor armRetentionEventDateProcessor;

    private static final String BEARER_TOKEN = "bearer";

    @BeforeEach
    void setupData() {
        armRetentionEventDateProcessor = new ArmRetentionEventDateProcessorImpl(externalObjectDirectoryRepository,
                                                                                armRetentionEventDateCalculator);

        String bearerToken = "bearer";
        ArmTokenRequest tokenRequest = new ArmTokenRequest(
            armApiConfigurationProperties.getArmUsername(), armApiConfigurationProperties.getArmPassword(), GrantType.PASSWORD.getValue());
        ArmTokenResponse tokenResponse = ArmTokenResponse.builder().accessToken(bearerToken).build();
        String armProfileId = "profileId";

        when(armTokenClient.getToken(tokenRequest)).thenReturn(tokenResponse);

        AvailableEntitlementProfile.Profiles profiles
            = AvailableEntitlementProfile.Profiles.builder().profileId(armProfileId).profileName(armApiConfigurationProperties.getArmServiceProfile()).build();
        AvailableEntitlementProfile profile = Mockito.mock(AvailableEntitlementProfile.class);
        when(profile.getProfiles()).thenReturn(List.of(profiles));

        when(armTokenClient.availableEntitlementProfiles("Bearer " + bearerToken)).thenReturn(profile);
        when(armTokenClient.selectEntitlementProfile("Bearer " + bearerToken, armProfileId)).thenReturn(tokenResponse);

    }

    @Disabled("Impacted by V1_367__adding_not_null_constraints_part_4.sql")
    @Test
    void calculateEventDates_WithMediaSuccessfulUpdate() {
        final String confidenceReason = "reason";
        final Integer confidenceScore = 232;
        final String externalRecordId = "recordId";

        // given
        when(armDataManagementConfiguration.getEventDateAdjustmentYears()).thenReturn(EVENT_DATE_ADJUSTMENT_YEARS);

        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                START_TIME,
                END_TIME,
                1,
                "mp2",
                confidenceScore,
                confidenceReason
            ));
        savedMedia.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );

        armEod.setExternalRecordId(externalRecordId);
        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.save(armEod);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        UpdateMetadataResponse response = UpdateMetadataResponse.builder().responseStatus(200).isError(false).build();
        when(armApiClient.updateMetadata(any(), any())).thenReturn(response);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        var persistedEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", persistedEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(persistedEod.isUpdateRetention());
        assertEquals(0, persistedEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

        UpdateMetadataRequest expectedMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(savedMedia.getRetainUntilTs().minusYears(EVENT_DATE_ADJUSTMENT_YEARS))
                          .retConfReason(confidenceReason)
                          .retConfScore(confidenceScore)
                          .build())
            .useGuidsForFields(false)
            .build();
        verify(armApiClient, times(1)).updateMetadata("Bearer " + BEARER_TOKEN, expectedMetadataRequest);

    }

    @Disabled("Impacted by V1_367__adding_not_null_constraints_part_4.sql")
    @Test
    void calculateEventDates_NoEodsToProcess() {

        // given
        when(armDataManagementConfiguration.getEventDateAdjustmentYears()).thenReturn(EVENT_DATE_ADJUSTMENT_YEARS);

        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        String confReason = "reason";
        Integer confScore = 100;
        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                START_TIME,
                END_TIME,
                1,
                "mp2",
                confScore,
                confReason
            ));
        savedMedia.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setEventDateTs(RETENTION_DATE_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.save(armEod);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        var persistedEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", persistedEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(persistedEod.isUpdateRetention());
        assertEquals(0, persistedEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

    }


    @Test
    @Disabled("Impacted by V1_364_*.sql")
    void calculateEventDates_WithTranscriptionSuccessfulUpdate() {
        // given
        when(armDataManagementConfiguration.getEventDateAdjustmentYears()).thenReturn(EVENT_DATE_ADJUSTMENT_YEARS);

        authorisationStub.givenTestSchema();
        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        final String checksum = "123";
        final String confidenceReason = "reason";
        final Integer confidenceScore = 232;
        final String externalRecordId = "recordId";

        TranscriptionDocumentEntity transcriptionDocumentEntity = TranscriptionStub.createTranscriptionDocumentEntity(
            transcriptionEntity, fileName, fileType, fileSize, testUser, checksum, confidenceScore, confidenceReason);
        transcriptionDocumentEntity.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        dartsDatabase.getTranscriptionDocumentRepository().save(transcriptionDocumentEntity);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            transcriptionDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setExternalRecordId(externalRecordId);
        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.getExternalObjectDirectoryRepository().save(armEod);


        UpdateMetadataResponse response = UpdateMetadataResponse.builder().responseStatus(200).isError(false).build();
        when(armApiClient.updateMetadata(any(), any())).thenReturn(response);


        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        var persistedEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", armEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(persistedEod.isUpdateRetention());
        assertEquals(0, persistedEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

        UpdateMetadataRequest expectedMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(transcriptionDocumentEntity.getRetainUntilTs().minusYears(EVENT_DATE_ADJUSTMENT_YEARS))
                          .retConfReason(confidenceReason)
                          .retConfScore(confidenceScore)
                          .build())
            .useGuidsForFields(false)
            .build();
        verify(armApiClient, times(1)).updateMetadata("Bearer " + BEARER_TOKEN, expectedMetadataRequest);

    }

    @Test
    void calculateEventDates_WithAnnotationSuccessfulUpdate() {

        // given
        when(armDataManagementConfiguration.getEventDateAdjustmentYears()).thenReturn(EVENT_DATE_ADJUSTMENT_YEARS);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, testAnnotation);

        when(userIdentity.getUserAccount()).thenReturn(testUser);
        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        final String checksum = "123";
        final String confidenceReason = "reason";
        final Integer confidenceScore = 232;
        final String externalRecordId = "recordId";

        AnnotationDocumentEntity annotationDocument = dartsDatabase.getAnnotationStub()
            .createAnnotationDocumentEntity(annotation, fileName, fileType, fileSize,
                                            testUser, uploadedDateTime, checksum, confidenceScore, confidenceReason
            );
        annotationDocument.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        dartsDatabase.save(annotationDocument);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            annotationDocument,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );

        armEod.setExternalRecordId(externalRecordId);
        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.save(armEod);

        UpdateMetadataResponse response = UpdateMetadataResponse.builder().responseStatus(200).isError(false).build();
        when(armApiClient.updateMetadata(any(), any())).thenReturn(response);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        var persistedEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", persistedEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(persistedEod.isUpdateRetention());
        assertEquals(0, persistedEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));


        UpdateMetadataRequest expectedMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(annotationDocument.getRetainUntilTs().minusYears(EVENT_DATE_ADJUSTMENT_YEARS))
                          .retConfReason(confidenceReason)
                          .retConfScore(confidenceScore)
                          .build())
            .useGuidsForFields(false)
            .build();
        verify(armApiClient, times(1)).updateMetadata("Bearer " + BEARER_TOKEN, expectedMetadataRequest);
    }

    @Test
    void calculateEventDates_WithCaseDocumentSuccessfulUpdate() {
        // given
        when(armDataManagementConfiguration.getEventDateAdjustmentYears()).thenReturn(EVENT_DATE_ADJUSTMENT_YEARS);

        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        String confidenceReason = "reason";
        Integer confidenceScore = 232;

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, testUser);
        caseDocument.setFileName("test_case_document.docx");
        caseDocument.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        caseDocument.setRetConfReason(confidenceReason);
        caseDocument.setRetConfScore(confidenceScore);

        dartsDatabase.save(caseDocument);

        String externalRecordId = "recordId";
        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            caseDocument,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        armEod.setExternalRecordId(externalRecordId);
        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.getExternalObjectDirectoryRepository().save(armEod);

        UpdateMetadataResponse response = UpdateMetadataResponse.builder().responseStatus(200).isError(false).build();
        when(armApiClient.updateMetadata(any(), any())).thenReturn(response);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        var persistedEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", persistedEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(persistedEod.isUpdateRetention());
        assertEquals(0, persistedEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

        UpdateMetadataRequest expectedMetadataRequest = UpdateMetadataRequest.builder()
            .itemId(externalRecordId)
            .manifest(UpdateMetadataRequest.Manifest.builder()
                          .eventDate(caseDocument.getRetainUntilTs().minusYears(EVENT_DATE_ADJUSTMENT_YEARS))
                          .retConfReason(confidenceReason)
                          .retConfScore(confidenceScore)
                          .build())
            .useGuidsForFields(false)
            .build();

        verify(armApiClient, times(1)).updateMetadata("Bearer " + BEARER_TOKEN, expectedMetadataRequest);
    }

    @Test
    void calculateEventDates_NoConfidenceScore() {
        // given
        when(armDataManagementConfiguration.getEventDateAdjustmentYears()).thenReturn(EVENT_DATE_ADJUSTMENT_YEARS);

        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        String confidenceReason = "reason";

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, testUser);
        caseDocument.setFileName("test_case_document.docx");
        caseDocument.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        caseDocument.setRetConfReason(confidenceReason);
        caseDocument.setRetConfScore(null);

        dartsDatabase.save(caseDocument);

        String externalRecordId = "recordId";
        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            caseDocument,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        armEod.setExternalRecordId(externalRecordId);
        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.getExternalObjectDirectoryRepository().save(armEod);

        UpdateMetadataResponse response = UpdateMetadataResponse.builder().responseStatus(200).isError(false).build();
        when(armApiClient.updateMetadata(any(), any())).thenReturn(response);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        var persistedEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", persistedEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertTrue(persistedEod.isUpdateRetention());

        verify(armApiClient, times(0)).updateMetadata(notNull(), notNull());
    }

    @Test
    void calculateEventDates_ConfidenceScoreOfZero() {
        // given
        when(armDataManagementConfiguration.getEventDateAdjustmentYears()).thenReturn(EVENT_DATE_ADJUSTMENT_YEARS);

        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        String confidenceReason = "reason";

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, testUser);
        caseDocument.setFileName("test_case_document.docx");
        caseDocument.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        caseDocument.setRetConfReason(confidenceReason);
        caseDocument.setRetConfScore(0);

        dartsDatabase.save(caseDocument);

        String externalRecordId = "recordId";
        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            caseDocument,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        armEod.setExternalRecordId(externalRecordId);
        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.getExternalObjectDirectoryRepository().save(armEod);

        UpdateMetadataResponse response = UpdateMetadataResponse.builder().responseStatus(200).isError(false).build();
        when(armApiClient.updateMetadata(any(), any())).thenReturn(response);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        var persistedEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", persistedEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertTrue(persistedEod.isUpdateRetention());

        verify(armApiClient, times(0)).updateMetadata(notNull(), notNull());
    }

    @Disabled("Impacted by V1_367__adding_not_null_constraints_part_4.sql")
    @Test
    void calculateEventDates_NoArmRecord_NoRetentionDateSet() {

        // given
        when(armDataManagementConfiguration.getEventDateAdjustmentYears()).thenReturn(EVENT_DATE_ADJUSTMENT_YEARS);

        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        String confReason = "reason";
        Integer confScore = 100;
        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                START_TIME,
                END_TIME,
                1,
                "mp2",
                confScore,
                confReason
            ));
        savedMedia.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_INGESTION,
            ARM,
            UUID.randomUUID()
        );

        armEod.setUpdateRetention(true);
        dartsDatabase.save(armEod);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        var persistedEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        assertNull(persistedEod.getEventDateTs());
        assertTrue(persistedEod.isUpdateRetention());
        verify(armApiClient, times(0)).updateMetadata(any(), any());
    }
}