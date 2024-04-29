package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.client.model.UpdateMetadataResponse;
import uk.gov.hmcts.darts.arm.component.ArmRetentionEventDateCalculator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
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
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.MediaTestData;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;

@Slf4j
@Transactional
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
    private ArmDataManagementApi armDataManagementApi;
    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private AuthorisationStub authorisationStub;

    private ArmRetentionEventDateProcessor armRetentionEventDateProcessor;


    @BeforeEach
    void setupData() {
        armRetentionEventDateProcessor = new ArmRetentionEventDateProcessorImpl(externalObjectDirectoryRepository,
                                                                                armRetentionEventDateCalculator);
    }

    @Test
    void calculateEventDates_WithMediaSuccessfulUpdate() {

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
                1
            ));
        savedMedia.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.save(armEod);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        UpdateMetadataResponse updateMetadataResponse = UpdateMetadataResponse.builder().itemId(UUID.randomUUID())
            .cabinetId(101)
            .objectId(UUID.fromString("4bfe4fc7-4e2f-4086-8a0e-146cc4556260"))
            .objectType(1)
            .fileName("UpdateMetadata-20241801-122819.json")
            .isError(false)
            .responseStatus(0)
            .responseStatusMessages(null)
            .build();
        when(armDataManagementApi.updateMetadata(any(), any())).thenReturn(updateMetadataResponse);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", armEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(armEod.isUpdateRetention());
        assertEquals(0, armEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

    }


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

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                START_TIME,
                END_TIME,
                1
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

        UpdateMetadataResponse updateMetadataResponse = UpdateMetadataResponse.builder().itemId(UUID.randomUUID())
            .cabinetId(101)
            .objectId(UUID.fromString("4bfe4fc7-4e2f-4086-8a0e-146cc4556260"))
            .objectType(1)
            .fileName("UpdateMetadata-20241801-122819.json")
            .isError(false)
            .responseStatus(0)
            .responseStatusMessages(null)
            .build();
        when(armDataManagementApi.updateMetadata(any(), any())).thenReturn(updateMetadataResponse);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", armEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(armEod.isUpdateRetention());
        assertEquals(0, armEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

    }

    @Test
    void calculateEventDates_WithCorrectlySetRetention() {

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
                1
            ));
        savedMedia.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        dartsDatabase.save(savedMedia);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );

        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.save(armEod);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        UpdateMetadataResponse updateMetadataResponse = UpdateMetadataResponse.builder().itemId(UUID.randomUUID())
            .cabinetId(101)
            .objectId(UUID.fromString("4bfe4fc7-4e2f-4086-8a0e-146cc4556260"))
            .objectType(1)
            .fileName("UpdateMetadata-20241801-122819.json")
            .isError(false)
            .responseStatus(0)
            .responseStatusMessages(null)
            .build();
        when(armDataManagementApi.updateMetadata(any(), any())).thenReturn(updateMetadataResponse);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", armEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(armEod.isUpdateRetention());
        assertEquals(0, armEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

    }

    @Test
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
        TranscriptionDocumentEntity transcriptionDocumentEntity = TranscriptionStub.createTranscriptionDocumentEntity(
            transcriptionEntity, fileName, fileType, fileSize, testUser, checksum);
        transcriptionDocumentEntity.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        dartsDatabase.getTranscriptionDocumentRepository().save(transcriptionDocumentEntity);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            transcriptionDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.getExternalObjectDirectoryRepository().save(armEod);

        UpdateMetadataResponse updateMetadataResponse = UpdateMetadataResponse.builder().itemId(UUID.randomUUID())
            .cabinetId(101)
            .objectId(UUID.fromString("4bfe4fc7-4e2f-4086-8a0e-146cc4556260"))
            .objectType(1)
            .fileName("UpdateMetadata-20241801-122819.json")
            .isError(false)
            .responseStatus(0)
            .responseStatusMessages(null)
            .build();
        when(armDataManagementApi.updateMetadata(any(), any())).thenReturn(updateMetadataResponse);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", armEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(armEod.isUpdateRetention());
        assertEquals(0, armEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

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
        AnnotationDocumentEntity annotationDocument = dartsDatabase.getAnnotationStub()
            .createAndSaveAnnotationDocumentEntityWith(annotation, fileName, fileType, fileSize,
                                                       testUser, uploadedDateTime, checksum
            );
        annotationDocument.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        dartsDatabase.save(annotationDocument);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            annotationDocument,
            dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
            dartsDatabase.getExternalLocationTypeEntity(ARM),
            UUID.randomUUID()
        );
        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.save(armEod);

        UpdateMetadataResponse updateMetadataResponse = UpdateMetadataResponse.builder().itemId(UUID.randomUUID())
            .cabinetId(101)
            .objectId(UUID.fromString("4bfe4fc7-4e2f-4086-8a0e-146cc4556260"))
            .objectType(1)
            .fileName("UpdateMetadata-20241801-122819.json")
            .isError(false)
            .responseStatus(0)
            .responseStatusMessages(null)
            .build();
        when(armDataManagementApi.updateMetadata(any(), any())).thenReturn(updateMetadataResponse);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", armEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(armEod.isUpdateRetention());
        assertEquals(0, armEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

    }

    @Test
    void calculateEventDates_WithCaseDocumentSuccessfulUpdate() {

        // given
        when(armDataManagementConfiguration.getEventDateAdjustmentYears()).thenReturn(EVENT_DATE_ADJUSTMENT_YEARS);

        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "Case1");
        UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        CaseDocumentEntity caseDocument = dartsDatabase.getCaseDocumentStub().createAndSaveCaseDocumentEntity(courtCaseEntity, uploadedBy);
        caseDocument.setFileName("test_case_document.docx");
        caseDocument.setRetainUntilTs(DOCUMENT_RETENTION_DATE_TIME);
        dartsDatabase.save(caseDocument);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            caseDocument,
            ARM_DROP_ZONE,
            ARM,
            UUID.randomUUID()
        );
        armEod.setEventDateTs(END_TIME);
        armEod.setUpdateRetention(true);
        dartsDatabase.getExternalObjectDirectoryRepository().save(armEod);

        UpdateMetadataResponse updateMetadataResponse = UpdateMetadataResponse.builder().itemId(UUID.randomUUID())
            .cabinetId(101)
            .objectId(UUID.fromString("4bfe4fc7-4e2f-4086-8a0e-146cc4556260"))
            .objectType(1)
            .fileName("UpdateMetadata-20241801-122819.json")
            .isError(false)
            .responseStatus(0)
            .responseStatusMessages(null)
            .build();
        when(armDataManagementApi.updateMetadata(any(), any())).thenReturn(updateMetadataResponse);

        // when
        armRetentionEventDateProcessor.calculateEventDates();

        // then
        dartsDatabase.getExternalObjectDirectoryRepository().findById(armEod.getId()).orElseThrow();
        log.info("EOD event date time {}", armEod.getEventDateTs().truncatedTo(MILLIS));
        log.info("Retention date time {}", RETENTION_DATE_TIME.truncatedTo(MILLIS));
        assertFalse(armEod.isUpdateRetention());
        assertEquals(0, armEod.getEventDateTs().truncatedTo(MILLIS).compareTo(RETENTION_DATE_TIME.truncatedTo(MILLIS)));

    }
}
