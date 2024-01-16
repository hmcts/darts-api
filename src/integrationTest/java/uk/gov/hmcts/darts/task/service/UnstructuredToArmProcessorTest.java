package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.MediaTestData;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_MANIFEST_FILE_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@Transactional
class UnstructuredToArmProcessorTest extends IntegrationBase {

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 10);
    private UnstructuredToArmProcessor unstructuredToArmProcessor;
    @MockBean
    private ArmDataManagementApi armDataManagementApi;

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Autowired
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Autowired
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Autowired
    private DataManagementApi dataManagementApi;
    @MockBean
    private UserIdentity userIdentity;
    @Autowired
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Autowired
    private FileOperationService fileOperationService;
    @Autowired
    private ArchiveRecordService archiveRecordService;

    @Autowired
    private AuthorisationStub authorisationStub;


    @BeforeEach
    void setupData() {
        unstructuredToArmProcessor = new UnstructuredToArmProcessorImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            dataManagementApi,
            armDataManagementApi,
            userIdentity,
            armDataManagementConfiguration,
            fileOperationService,
            archiveRecordService
        );

    }

    @Test
    void movePendingMediaDataFromUnstructuredToArmStorage() {

        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        ExternalObjectDirectoryEntity unstructuredEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.UNSTRUCTURED),
            UUID.randomUUID()
        );
        dartsDatabase.save(unstructuredEod);

        unstructuredToArmProcessor.processUnstructuredToArm();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
    }

    @Test
    void movePendingAnnotationDataFromUnstructuredToArmStorage() {
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub()
            .createAndSaveAnnotationEntityWith(testUser, testAnnotation);

        hearing.addAnnotation(annotation);
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        final String checksum = "xi/XkzD2HuqTUzDafW8Cgw==";

        AnnotationDocumentEntity annotationDocument = dartsDatabase.getAnnotationStub()
            .createAndSaveAnnotationDocumentEntityWith(annotation, fileName, fileType, fileSize,
                                                       testUser, uploadedDateTime, checksum
            );

        ExternalObjectDirectoryEntity unstructuredEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            annotationDocument,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.UNSTRUCTURED),
            UUID.randomUUID()
        );
        dartsDatabase.save(unstructuredEod);

        unstructuredToArmProcessor.processUnstructuredToArm();

        List<ExternalObjectDirectoryEntity> foundList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByStatusAndType(
                dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
                dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM)
            );

        assertEquals(1, foundList.size());
        ExternalObjectDirectoryEntity objectDirectory = foundList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), objectDirectory.getStatus().getId());
    }

    @Test
    void movePendingTranscriptionDataFromUnstructuredToArmStorage() {

        authorisationStub.givenTestSchema();
        TranscriptionEntity transcriptionEntity = authorisationStub.getTranscriptionEntity();

        final String fileName = "Test Document.docx";
        final String fileType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        final int fileSize = 11_937;
        final UserAccountEntity uploadedBy = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        final String checksum = "xi/XkzD2HuqTUzDafW8Cgw==";

        TranscriptionDocumentEntity transcriptionDocumentEntity = TranscriptionStub.createTranscriptionDocumentEntity(
            transcriptionEntity, fileName, fileType, fileSize, uploadedBy, checksum);

        when(userIdentity.getUserAccount()).thenReturn(uploadedBy);

        ExternalObjectDirectoryEntity unstructuredEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            transcriptionDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.UNSTRUCTURED),
            UUID.randomUUID()
        );
        dartsDatabase.getExternalObjectDirectoryRepository().saveAndFlush(unstructuredEod);

        unstructuredToArmProcessor.processUnstructuredToArm();

        List<ExternalObjectDirectoryEntity> foundList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByStatusAndType(
                dartsDatabase.getObjectRecordStatusEntity(ARM_DROP_ZONE),
                dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM)
            );

        assertEquals(1, foundList.size());
        ExternalObjectDirectoryEntity objectDirectory = foundList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), objectDirectory.getStatus().getId());
    }

    @Test
    void movePreviousRawDataFailedAttemptFromUnstructuredToArmStorage() {
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        ExternalObjectDirectoryEntity unstructuredEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.UNSTRUCTURED),
            UUID.randomUUID()
        );
        dartsDatabase.save(unstructuredEod);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(FAILURE_ARM_RAW_DATA_FAILED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        final UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        unstructuredToArmProcessor.processUnstructuredToArm();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());

    }

    @Test
    void movePreviousManifestFailedAttemptFromUnstructuredToArmStorage() {
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        ExternalObjectDirectoryEntity unstructuredEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.UNSTRUCTURED),
            UUID.randomUUID()
        );
        dartsDatabase.save(unstructuredEod);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(FAILURE_ARM_MANIFEST_FILE_FAILED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(2);
        dartsDatabase.save(armEod);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        unstructuredToArmProcessor.processUnstructuredToArm();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(ARM_DROP_ZONE.getId(), foundMedia.getStatus().getId());
        assertEquals(2, foundMedia.getTransferAttempts());
    }

    @Test
    void skipMovePreviousManifestFailedAttemptFromUnstructuredToArmStorageWhenMaxTransferAttemptReached() {
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        ExternalObjectDirectoryEntity unstructuredEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.UNSTRUCTURED),
            UUID.randomUUID()
        );
        dartsDatabase.save(unstructuredEod);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(FAILURE_ARM_MANIFEST_FILE_FAILED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );

        armEod.setTransferAttempts(4);
        dartsDatabase.save(armEod);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        unstructuredToArmProcessor.processUnstructuredToArm();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(savedMedia, dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(FAILURE_ARM_MANIFEST_FILE_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(4, foundMedia.getTransferAttempts());
    }

    @Test
    void updateTransferAttemptIfUnableToFindUnstructuredRecordFromFailedArmEod() {
        HearingEntity hearing = dartsDatabase.createHearing(
            "NEWCASTLE",
            "Int Test Courtroom 2",
            "2",
            HEARING_DATE
        );

        MediaEntity savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            UUID.randomUUID()
        );
        armEod.setStatus(dartsDatabase.getObjectRecordStatusRepository().getReferenceById(FAILURE_ARM_RAW_DATA_FAILED.getId()));
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        unstructuredToArmProcessor.processUnstructuredToArm();

        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository().findByMediaAndExternalLocationType(
            savedMedia,
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM)
        );

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.get(0);
        assertEquals(FAILURE_ARM_RAW_DATA_FAILED.getId(), foundMedia.getStatus().getId());
        assertEquals(2, foundMedia.getTransferAttempts());
    }
}
