package uk.gov.hmcts.darts.arm.service;

import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.DETS;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;


@ExtendWith(OutputCaptureExtension.class)
class DetsToArmBatchPushProcessorIntTest extends IntegrationBase {

    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 26, 10, 0, 0);
    private MediaEntity savedMedia;

    @MockitoBean
    private UserIdentity userIdentity;
    @MockitoBean
    private ArmDataManagementApi armDataManagementApi;

    @Autowired
    private DetsToArmBatchPushProcessor detsToArmBatchPushProcessor;


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
    }

    @Test
    void processDetsToArm_Success_WithDetsEod() {
        // given
        ObjectStateRecordEntity objectStateRecordEntity = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(111L));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        ExternalObjectDirectoryEntity detsEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID().toString()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        detsEod.setLastModifiedDateTime(latestDateTime);
        detsEod.setTransferAttempts(1);
        detsEod.setResponseCleaned(false);
        detsEod.setOsrUuid(objectStateRecordEntity.getUuid());
        detsEod = dartsDatabase.save(detsEod);

        objectStateRecordEntity.setEodId(detsEod.getId());
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        String rawFilename = String.format("%s_%s_%s", detsEod.getId(), savedMedia.getId(), detsEod.getTransferAttempts());
        doNothing().when(armDataManagementApi).copyDetsBlobDataToArm(detsEod.getExternalLocation(), rawFilename);

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        Optional<ExternalObjectDirectoryEntity> foundArmEodOptional = dartsDatabase.getExternalObjectDirectoryRepository()
            .findMatchingExternalObjectDirectoryEntityByLocation(
                EodHelper.armDropZoneStatus(),
                EodHelper.armLocation(),
                savedMedia,
                null,
                null,
                null
            );
        assertTrue(foundArmEodOptional.isPresent());
        ExternalObjectDirectoryEntity foundArmEod = foundArmEodOptional.get();
        assertEquals(EodHelper.armDropZoneStatus(), foundArmEod.getStatus());
        assertNotNull(foundArmEod.getOsrUuid());

        ObjectStateRecordEntity objectStateRecord = dartsDatabase.getObjectStateRecordRepository()
            .findById(objectStateRecordEntity.getUuid()).orElseThrow();
        verifyObjectStateRecordSuccessfullyUpdated(foundArmEod, detsEod, objectStateRecord);

    }

    @Test
    void processDetsToArm_Success_WithFailedRawDataStatusArmEod() {
        // given
        ObjectStateRecordEntity objectStateRecordEntity = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(111L));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        ExternalObjectDirectoryEntity detsEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID().toString()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        detsEod.setLastModifiedDateTime(latestDateTime);
        detsEod.setTransferAttempts(1);
        detsEod.setOsrUuid(objectStateRecordEntity.getUuid());
        detsEod = dartsDatabase.save(detsEod);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_RAW_DATA_FAILED,
            DETS,
            UUID.randomUUID().toString()
        );
        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setOsrUuid(objectStateRecordEntity.getUuid());
        armEod = dartsDatabase.save(armEod);

        objectStateRecordEntity.setEodId(detsEod.getId());
        objectStateRecordEntity.setArmEodId(armEod.getId());
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        String rawFilename = String.format("%s_%s_%s", detsEod.getId(), savedMedia.getId(), detsEod.getTransferAttempts());
        doNothing().when(armDataManagementApi).copyDetsBlobDataToArm(detsEod.getExternalLocation(), rawFilename);

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        Optional<ExternalObjectDirectoryEntity> foundArmEodOptional = dartsDatabase.getExternalObjectDirectoryRepository()
            .findMatchingExternalObjectDirectoryEntityByLocation(
                EodHelper.armDropZoneStatus(),
                EodHelper.armLocation(),
                savedMedia,
                null,
                null,
                null
            );
        assertTrue(foundArmEodOptional.isPresent());
        ExternalObjectDirectoryEntity foundArmEod = foundArmEodOptional.get();
        assertEquals(EodHelper.armDropZoneStatus(), foundArmEod.getStatus());
        assertNotNull(foundArmEod.getOsrUuid());

        ObjectStateRecordEntity objectStateRecord = dartsDatabase.getObjectStateRecordRepository()
            .findById(objectStateRecordEntity.getUuid()).orElseThrow();
        verifyObjectStateRecordSuccessfullyUpdated(foundArmEod, detsEod, objectStateRecord);

    }

    @Test
    void processDetsToArmWithFailedManifestFileStatusArmEodSuccess() {
        // given
        ObjectStateRecordEntity objectStateRecordEntity = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(111L));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        ExternalObjectDirectoryEntity detsEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID().toString()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        detsEod.setLastModifiedDateTime(latestDateTime);
        detsEod.setTransferAttempts(1);
        detsEod.setOsrUuid(objectStateRecordEntity.getUuid());
        detsEod = dartsDatabase.save(detsEod);

        ExternalObjectDirectoryEntity armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            ARM_MANIFEST_FAILED,
            DETS,
            UUID.randomUUID().toString()
        );
        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setOsrUuid(objectStateRecordEntity.getUuid());
        armEod = dartsDatabase.save(armEod);

        objectStateRecordEntity.setEodId(detsEod.getId());
        objectStateRecordEntity.setArmEodId(armEod.getId());
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        String rawFilename = String.format("%s_%s_%s", detsEod.getId(), savedMedia.getId(), detsEod.getTransferAttempts());
        doNothing().when(armDataManagementApi).copyDetsBlobDataToArm(detsEod.getExternalLocation(), rawFilename);

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        Optional<ExternalObjectDirectoryEntity> foundArmEodOptional = dartsDatabase.getExternalObjectDirectoryRepository()
            .findMatchingExternalObjectDirectoryEntityByLocation(
                EodHelper.armDropZoneStatus(),
                EodHelper.armLocation(),
                savedMedia,
                null,
                null,
                null
            );
        assertTrue(foundArmEodOptional.isPresent());
        ExternalObjectDirectoryEntity foundArmEod = foundArmEodOptional.get();
        assertEquals(EodHelper.armDropZoneStatus(), foundArmEod.getStatus());
        assertNotNull(foundArmEod.getOsrUuid());

        ObjectStateRecordEntity objectStateRecord = dartsDatabase.getObjectStateRecordRepository()
            .findById(objectStateRecordEntity.getUuid()).orElseThrow();
        verifyObjectStateRecordSuccessfullyUpdated(foundArmEod, detsEod, objectStateRecord);

    }

    @Test
    void processDetsToArmWithNoOsrUuid(CapturedOutput output) {
        // given
        ExternalObjectDirectoryEntity detsEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID().toString()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        detsEod.setLastModifiedDateTime(latestDateTime);
        detsEod.setTransferAttempts(1);
        detsEod.setResponseCleaned(false);
        dartsDatabase.save(detsEod);

        detsToArmBatchPushProcessor.processDetsToArm(5);
        // when
        //Error is gracefully handled and logged
        assertThat(output)
            .contains("uk.gov.hmcts.darts.common.exception.DartsException: Unable to find ObjectStateRecordEntity for ARM EOD ID: 2 as OSR UUID is null");
    }

    @Test
    void processDetsToArm_ThrowsException_WhenSaveManifestFails() {
        // given
        ObjectStateRecordEntity objectStateRecordEntity = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(111L));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        ExternalObjectDirectoryEntity detsEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID().toString()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        detsEod.setLastModifiedDateTime(latestDateTime);
        detsEod.setTransferAttempts(1);
        detsEod.setResponseCleaned(false);
        detsEod.setOsrUuid(objectStateRecordEntity.getUuid());
        detsEod = dartsDatabase.save(detsEod);

        objectStateRecordEntity.setEodId(detsEod.getId());
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        String rawFilename = String.format("%s_%s_%s", detsEod.getId(), savedMedia.getId(), detsEod.getTransferAttempts());
        doNothing().when(armDataManagementApi).copyDetsBlobDataToArm(detsEod.getExternalLocation(), rawFilename);
        when(armDataManagementApi.saveBlobDataToArm(any(), any())).thenThrow(new BlobStorageException("Error", null, 500));

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        Optional<ExternalObjectDirectoryEntity> foundArmEodOptional = dartsDatabase.getExternalObjectDirectoryRepository()
            .findMatchingExternalObjectDirectoryEntityByLocation(
                EodHelper.failedArmManifestFileStatus(),
                EodHelper.armLocation(),
                savedMedia,
                null,
                null,
                null
            );
        assertTrue(foundArmEodOptional.isPresent());
        ExternalObjectDirectoryEntity foundArmEod = foundArmEodOptional.get();
        assertEquals(EodHelper.failedArmManifestFileStatus(), foundArmEod.getStatus());
        assertNotNull(foundArmEod.getOsrUuid());

        ObjectStateRecordEntity objectStateRecord = dartsDatabase.getObjectStateRecordRepository()
            .findById(objectStateRecordEntity.getUuid()).orElseThrow();
        verifyObjectStateRecordFailed(foundArmEod, detsEod, objectStateRecord);

    }

    @Test
    void processDetsToArm_ThrowsException_WhenSaveRawDataFails() {
        // given
        ObjectStateRecordEntity objectStateRecordEntity = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(111L));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        ExternalObjectDirectoryEntity detsEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID().toString()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        detsEod.setLastModifiedDateTime(latestDateTime);
        detsEod.setTransferAttempts(1);
        detsEod.setResponseCleaned(false);
        detsEod.setOsrUuid(objectStateRecordEntity.getUuid());
        detsEod = dartsDatabase.save(detsEod);

        objectStateRecordEntity.setEodId(detsEod.getId());
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        doThrow(new DartsException("")).when(armDataManagementApi).copyDetsBlobDataToArm(any(), any());

        // when
        detsToArmBatchPushProcessor.processDetsToArm(5);

        // then
        Optional<ExternalObjectDirectoryEntity> foundArmEodOptional = dartsDatabase.getExternalObjectDirectoryRepository()
            .findMatchingExternalObjectDirectoryEntityByLocation(
                EodHelper.failedArmRawDataStatus(),
                EodHelper.armLocation(),
                savedMedia,
                null,
                null,
                null
            );
        assertTrue(foundArmEodOptional.isPresent());
        ExternalObjectDirectoryEntity foundArmEod = foundArmEodOptional.get();
        assertEquals(EodHelper.failedArmRawDataStatus(), foundArmEod.getStatus());
        assertNotNull(foundArmEod.getOsrUuid());

        ObjectStateRecordEntity objectStateRecordEntityModified = dartsDatabase.getObjectStateRecordRepository()
            .findById(foundArmEod.getOsrUuid()).orElseThrow();
        assertEquals(detsEod.getId(), objectStateRecordEntityModified.getEodId());
        assertEquals(foundArmEod.getId(), objectStateRecordEntityModified.getArmEodId());

    }

    private void verifyObjectStateRecordSuccessfullyUpdated(ExternalObjectDirectoryEntity foundArmEod, ExternalObjectDirectoryEntity detsEod,
                                                            ObjectStateRecordEntity objectStateRecordEntity) {
        ObjectStateRecordEntity objectStateRecordEntityModified = dartsDatabase.getObjectStateRecordRepository()
            .findById(foundArmEod.getOsrUuid()).orElseThrow();
        assertEquals(detsEod.getId(), objectStateRecordEntityModified.getEodId());
        assertEquals(foundArmEod.getId(), objectStateRecordEntityModified.getArmEodId());
        assertTrue(objectStateRecordEntityModified.getFlagFileTransfToarml());
        assertNotNull(objectStateRecordEntityModified.getDateFileTransfToarml());
        assertEquals(detsEod.getChecksum(), objectStateRecordEntityModified.getMd5FileTransfArml());
        assertEquals(savedMedia.getFileSize(), objectStateRecordEntity.getFileSizeBytesArml());
        assertTrue(objectStateRecordEntityModified.getFlagFileMfstCreated());
        assertNotNull(objectStateRecordEntityModified.getDateFileMfstCreated());
        assertNotNull(objectStateRecordEntityModified.getIdManifestFile());
        assertTrue(objectStateRecordEntityModified.getFlagMfstTransfToArml());
        assertNotNull(objectStateRecordEntityModified.getDateMfstTransfToArml());
    }

    private void verifyObjectStateRecordFailed(ExternalObjectDirectoryEntity foundArmEod, ExternalObjectDirectoryEntity detsEod,
                                               ObjectStateRecordEntity objectStateRecordEntity) {
        ObjectStateRecordEntity objectStateRecordEntityModified = dartsDatabase.getObjectStateRecordRepository()
            .findById(foundArmEod.getOsrUuid()).orElseThrow();
        assertEquals(detsEod.getId(), objectStateRecordEntityModified.getEodId());
        assertEquals(foundArmEod.getId(), objectStateRecordEntityModified.getArmEodId());
        assertTrue(objectStateRecordEntityModified.getFlagFileTransfToarml());
        assertNotNull(objectStateRecordEntityModified.getDateFileTransfToarml());
        assertEquals(detsEod.getChecksum(), objectStateRecordEntityModified.getMd5FileTransfArml());
        assertEquals(savedMedia.getFileSize(), objectStateRecordEntity.getFileSizeBytesArml());
        assertTrue(objectStateRecordEntityModified.getFlagFileMfstCreated());
        assertNotNull(objectStateRecordEntityModified.getDateFileMfstCreated());
        assertNotNull(objectStateRecordEntityModified.getIdManifestFile());
    }

    private ObjectStateRecordEntity createObjectStateRecordEntity(Long uuid) {
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(uuid);
        return objectStateRecordEntity;
    }
}
