package uk.gov.hmcts.darts.arm.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItem;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItems;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.metadata.UploadNewFileRecordMetadata;
import uk.gov.hmcts.darts.arm.model.record.operation.MediaCreateArchiveRecordOperation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.test.common.FileStore;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.UPLOAD_NEW_FILE;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.DETS;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.NEW;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;

class DataStoreToArmHelperIntTest extends IntegrationBase {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 26, 10, 0, 0);
    private MediaEntity savedMedia;

    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private DataStoreToArmHelper dataStoreToArmHelper;

    @Autowired
    private EodHelper eodHelper;
    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    private ExternalObjectDirectoryEntity externalObjectDirectory;
    private ObjectStateRecordEntity objectStateRecordEntity;

    @TempDir
    private File tempDirectory;

    @BeforeEach
    void setupData() {

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        HearingEntity hearing = dartsDatabase.createHearing(
            "Bristol",
            "Int Test Courtroom 1",
            "1",
            HEARING_DATE
        );

        savedMedia = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        savedMedia.setFileSize(1000L);
        savedMedia = dartsDatabase.save(savedMedia);
        objectStateRecordEntity = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(111L));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        externalObjectDirectory = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        externalObjectDirectory.setLastModifiedDateTime(latestDateTime);
        externalObjectDirectory.setTransferAttempts(1);
        externalObjectDirectory.setResponseCleaned(false);
        externalObjectDirectory.setOsrUuid(objectStateRecordEntity.getUuid());
        externalObjectDirectory = dartsDatabase.save(externalObjectDirectory);
    }

    @Test
    void ignoreArmDropZoneStatus() {
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> eod.setManifestFile("existingManifestFile"));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_DROP_ZONE, ARM);

        List<ExternalObjectDirectoryEntity> eodEntitiesToSendToArm = dataStoreToArmHelper.getEodEntitiesToSendToArm(EodHelper.unstructuredLocation(),
                                                                                                                    EodHelper.armLocation(), 5);
        assertEquals(1, eodEntitiesToSendToArm.size());

    }

    @Test
    void getCorrectEodEntities() {
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> eod.setManifestFile("existingManifestFile"));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_MANIFEST_FAILED, ARM);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(3), NEW, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(4), STORED, UNSTRUCTURED);
        ExternalObjectDirectoryEntity failedTooManyTimesEod = externalObjectDirectoryStub.createAndSaveEod(medias.get(4), ARM_MANIFEST_FAILED, ARM);
        failedTooManyTimesEod.setTransferAttempts(4);
        dartsDatabase.save(failedTooManyTimesEod);

        List<ExternalObjectDirectoryEntity> eodEntitiesToSendToArm = dataStoreToArmHelper.getEodEntitiesToSendToArm(EodHelper.unstructuredLocation(),
                                                                                                                    EodHelper.armLocation(), 5);
        assertEquals(3, eodEntitiesToSendToArm.size());

    }

    @Test
    void getEodEntitiesToSendToArm() {
        ExternalLocationTypeEntity sourceLocation = externalObjectDirectoryStub.getLocation(DETS);
        ExternalLocationTypeEntity armLocation = externalObjectDirectoryStub.getLocation(ARM);

        List<ExternalObjectDirectoryEntity> result = dataStoreToArmHelper.getEodEntitiesToSendToArm(sourceLocation, armLocation, 5);

        assertNotNull(result);
    }

    @Test
    void getExternalObjectDirectoryEntity() {
        ExternalLocationTypeEntity eodSourceLocation = externalObjectDirectoryStub.getLocation(DETS);
        ObjectRecordStatusEntity status = dartsDatabase.getObjectRecordStatusRepository().findById(STORED.getId()).orElseThrow();
        ExternalObjectDirectoryEntity armExternalObjectDirectory = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        armExternalObjectDirectory.setOsrUuid(objectStateRecordEntity.getUuid());
        dartsDatabase.save(armExternalObjectDirectory);

        Optional<ExternalObjectDirectoryEntity> result = dataStoreToArmHelper.getExternalObjectDirectoryEntity(externalObjectDirectory, eodSourceLocation,
                                                                                                               status);
        assertTrue(result.isPresent());
    }

    @Test
    void updateExternalObjectDirectoryStatusToFailed() {
        ObjectRecordStatusEntity status = dartsDatabase.getObjectRecordStatusRepository().findById(FAILURE_CHECKSUM_FAILED.getId()).orElseThrow();
        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        dataStoreToArmHelper.updateExternalObjectDirectoryStatusToFailed(externalObjectDirectory, status, userAccount);

        assertEquals(status, externalObjectDirectory.getStatus());
    }

    @Test
    void incrementTransferAttempts() {

        dataStoreToArmHelper.incrementTransferAttempts(externalObjectDirectory);

        assertEquals(2, externalObjectDirectory.getTransferAttempts());
    }

    @Test
    void updateExternalObjectDirectoryStatus() {
        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ObjectRecordStatusEntity status = dartsDatabase.getObjectRecordStatusRepository().findById(STORED.getId()).orElseThrow();

        dataStoreToArmHelper.updateExternalObjectDirectoryStatus(externalObjectDirectory, status, userAccount);

        assertEquals(status, externalObjectDirectory.getStatus());
    }

    @Test
    void createArmExternalObjectDirectoryEntity() {
        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ObjectRecordStatusEntity status = dartsDatabase.getObjectRecordStatusRepository().findById(ARM_DROP_ZONE.getId()).orElseThrow();

        ExternalObjectDirectoryEntity result = dataStoreToArmHelper.createArmExternalObjectDirectoryEntity(
            externalObjectDirectory, status, userAccount);

        assertNotNull(result);
        assertEquals(externalObjectDirectory.getExternalLocation(), result.getExternalLocation());
        assertEquals(externalObjectDirectory.getOsrUuid(), result.getOsrUuid());
        assertEquals(status, result.getStatus());
        assertEquals(userAccount, result.getCreatedBy());
    }

    @Test
    void generateRawFilename() {

        String result = dataStoreToArmHelper.generateRawFilename(externalObjectDirectory);

        assertNotNull(result);
    }

    @Test
    void updateExternalObjectDirectoryFailedTransferAttempts() {
        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        dataStoreToArmHelper.updateExternalObjectDirectoryFailedTransferAttempts(externalObjectDirectory, userAccount);

        assertEquals(2, externalObjectDirectory.getTransferAttempts());
    }

    @Test
    void createEmptyArchiveRecordsFile() {
        String manifestFilePrefix = "DETS";

        File result = dataStoreToArmHelper.createEmptyArchiveRecordsFile(manifestFilePrefix);

        assertNotNull(result);
    }

    @Test
    void updateArmEodToArmIngestionStatus() {
        ArmBatchItem batchItem = new ArmBatchItem();
        ArmBatchItems batchItems = new ArmBatchItems();
        File archiveRecordsFile = new File("testfile");
        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        ExternalLocationTypeEntity eodSourceLocation = dartsDatabase.getExternalLocationTypeRepository().findById(DETS.getId()).orElseThrow();

        dataStoreToArmHelper.updateArmEodToArmIngestionStatus(externalObjectDirectory, batchItem, batchItems,
                                                              archiveRecordsFile, userAccount, eodSourceLocation);

        assertEquals(ARM_INGESTION.getId(), externalObjectDirectory.getStatus().getId());
    }

    @Test
    void createArmEodWithArmIngestionStatus() {
        ArmBatchItem batchItem = new ArmBatchItem();
        ArmBatchItems batchItems = new ArmBatchItems();
        File archiveRecordsFile = new File("testfile");
        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        ExternalObjectDirectoryEntity result = dataStoreToArmHelper.createArmEodWithArmIngestionStatus(externalObjectDirectory,
                                                                                                       batchItem, batchItems,
                                                                                                       archiveRecordsFile, userAccount);
        assertNotNull(result);
    }

    @Test
    void shouldPushRawDataToArm() {
        ArmBatchItem batchItem = new ArmBatchItem();
        ObjectRecordStatusEntity status = externalObjectDirectoryStub.getStatus(ARM_INGESTION);
        batchItem.setPreviousStatus(status);

        boolean result = dataStoreToArmHelper.shouldPushRawDataToArm(batchItem);

        assertTrue(result);
    }

    @Test
    void shouldAddEntryToManifestFile() {
        ArmBatchItem batchItem = new ArmBatchItem();
        ObjectRecordStatusEntity status = dartsDatabase.getObjectRecordStatusRepository().findById(ARM_MANIFEST_FAILED.getId()).orElseThrow();

        batchItem.setPreviousStatus(status);

        boolean result = dataStoreToArmHelper.shouldAddEntryToManifestFile(batchItem);

        assertTrue(result);
    }

    @Test
    void writeManifestFile() throws IOException {
        ArmBatchItems batchItems = new ArmBatchItems();
        ArmBatchItem batchItem = new ArmBatchItem();
        batchItem.setArmEod(externalObjectDirectory);
        ArchiveRecord archiveRecord = createMediaArchiveRecord(String.valueOf(externalObjectDirectory.getId()));
        batchItem.setArchiveRecord(archiveRecord);
        batchItem.setRawFilePushSuccessful(true);
        batchItems.add(batchItem);
        String fileLocation = tempDirectory.getAbsolutePath();
        File archiveFile = FileStore.getFileStore().create(Path.of(fileLocation), Path.of("archive-records.a360"));

        dataStoreToArmHelper.writeManifestFile(batchItems, archiveFile);

        assertTrue(archiveFile.exists());
    }

    @Test
    void recoverByUpdatingEodToFailedArmStatus() {
        ArmBatchItem batchItem = new ArmBatchItem();
        batchItem.setArmEod(externalObjectDirectory);
        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        dataStoreToArmHelper.recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount);

        assertEquals(ARM_MANIFEST_FAILED.getId(), externalObjectDirectory.getStatus().getId());
    }

    @Test
    void getFileSize() {
        int externalObjectDirectoryId = externalObjectDirectory.getId();

        Long result = dataStoreToArmHelper.getFileSize(externalObjectDirectoryId);

        assertNotNull(result);
    }

    private ObjectStateRecordEntity createObjectStateRecordEntity(Long uuid) {
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(uuid);
        return objectStateRecordEntity;
    }

    private MediaArchiveRecord createMediaArchiveRecord(String relationId) {
        return MediaArchiveRecord.builder()
            .mediaCreateArchiveRecord(createMediaArchiveRecordOperation(relationId))
            .uploadNewFileRecord(createMediaUploadNewFileRecord(relationId))
            .build();
    }

    private MediaCreateArchiveRecordOperation createMediaArchiveRecordOperation(String relationId) {
        return MediaCreateArchiveRecordOperation.builder()
            .relationId(relationId)
            .recordMetadata(createMediaArchiveRecordMetadata())
            .build();
    }

    private RecordMetadata createMediaArchiveRecordMetadata() {
        OffsetDateTime recordTime = OffsetDateTime.of(2024, 1, 23, 10, 0, 0, 0, ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        return RecordMetadata.builder()
            .recordClass("DARTS")
            .publisher("DARTS")
            .region("GBR")
            .recordDate(recordTime.format(formatter))
            .eventDate("2024-01-23T11:40:00Z")
            .title("Filename")
            .clientId("1234")
            .contributor("Swansea & Courtroom 1")
            .bf001("Media")
            .bf002("Case_1|Case_2|Case_3")
            .bf003("mp2")
            .bf004("2024-01-23T00:00:00Z")
            .bf005("xi/XkzD2HuqTUzDafW8Cgw==")
            .bf011("2024-01-23T11:39:30Z")
            .bf012(1)
            .bf013(1)
            .bf014(3)
            .bf015(4)
            .bf017("2024-01-23T11:40:00Z")
            .bf018("2024-01-23T13:40:00Z")
            .bf019("Swansea")
            .bf020("Courtroom 1")
            .build();
    }

    private UploadNewFileRecord createMediaUploadNewFileRecord(String relationId) {
        UploadNewFileRecord uploadNewFileRecord = new UploadNewFileRecord();
        uploadNewFileRecord.setOperation(UPLOAD_NEW_FILE);
        uploadNewFileRecord.setRelationId(relationId);
        uploadNewFileRecord.setFileMetadata(createMediaUploadNewFileRecordMetadata());
        return uploadNewFileRecord;
    }

    private UploadNewFileRecordMetadata createMediaUploadNewFileRecordMetadata() {
        UploadNewFileRecordMetadata uploadNewFileRecordMetadata = new UploadNewFileRecordMetadata();
        uploadNewFileRecordMetadata.setPublisher("DARTS");
        uploadNewFileRecordMetadata.setDzFilename("123_456_1");
        uploadNewFileRecordMetadata.setFileTag("mp2");
        return uploadNewFileRecordMetadata;
    }

}
