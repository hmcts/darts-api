package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.File;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
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
    @MockitoSpyBean
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @TempDir
    private File tempDirectory;

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

        String fileLocation = tempDirectory.getAbsolutePath();
        lenient().when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

    }

    @Test
    void processDetsToArmWithDetsEodReturnsSuccess() {
        // given
        ObjectStateRecordEntity objectStateRecordEntity = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(111L));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        ExternalObjectDirectoryEntity detsEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);

        detsEod.setLastModifiedDateTime(latestDateTime);
        detsEod.setTransferAttempts(1);
        detsEod.setResponseCleaned(false);
        detsEod.setOsrUuid(objectStateRecordEntity.getUuid());
        detsEod = dartsDatabase.save(detsEod);

        objectStateRecordEntity.setEodId(String.valueOf(detsEod.getId()));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        String rawFilename = String.format("%s_%s_%s", detsEod.getId(), savedMedia.getId(), detsEod.getTransferAttempts());
        doNothing().when(armDataManagementApi).copyDetsBlobDataToArm(detsEod.getExternalLocation().toString(), rawFilename);

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
    void processDetsToArmWithFailedRawDataStatusArmEodSuccess() {
        // given
        ObjectStateRecordEntity objectStateRecordEntity = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(111L));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        ExternalObjectDirectoryEntity detsEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID()
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
            UUID.randomUUID()
        );
        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setOsrUuid(objectStateRecordEntity.getUuid());
        armEod = dartsDatabase.save(armEod);

        objectStateRecordEntity.setEodId(String.valueOf(detsEod.getId()));
        objectStateRecordEntity.setArmEodId(String.valueOf(armEod.getId()));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        String rawFilename = String.format("%s_%s_%s", detsEod.getId(), savedMedia.getId(), detsEod.getTransferAttempts());
        doNothing().when(armDataManagementApi).copyDetsBlobDataToArm(detsEod.getExternalLocation().toString(), rawFilename);

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
            UUID.randomUUID()
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
            UUID.randomUUID()
        );
        armEod.setLastModifiedDateTime(latestDateTime);
        armEod.setTransferAttempts(1);
        armEod.setOsrUuid(objectStateRecordEntity.getUuid());
        armEod = dartsDatabase.save(armEod);

        objectStateRecordEntity.setEodId(String.valueOf(detsEod.getId()));
        objectStateRecordEntity.setArmEodId(String.valueOf(armEod.getId()));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

        String rawFilename = String.format("%s_%s_%s", detsEod.getId(), savedMedia.getId(), detsEod.getTransferAttempts());
        doNothing().when(armDataManagementApi).copyDetsBlobDataToArm(detsEod.getExternalLocation().toString(), rawFilename);

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

    private void verifyObjectStateRecordSuccessfullyUpdated(ExternalObjectDirectoryEntity foundArmEod, ExternalObjectDirectoryEntity detsEod,
                                                            ObjectStateRecordEntity objectStateRecordEntity) {
        ObjectStateRecordEntity objectStateRecordEntityModified = dartsDatabase.getObjectStateRecordRepository()
            .findById(foundArmEod.getOsrUuid()).orElseThrow();
        assertEquals(detsEod.getId(), Integer.parseInt(objectStateRecordEntityModified.getEodId()));
        assertEquals(foundArmEod.getId(), Integer.parseInt(objectStateRecordEntityModified.getArmEodId()));
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

    @Test
    void processDetsToArmWithNoOsrUuid(CapturedOutput output) {
        // given
        ExternalObjectDirectoryEntity detsEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            DETS,
            UUID.randomUUID()
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

    private ObjectStateRecordEntity createObjectStateRecordEntity(Long uuid) {
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(uuid);
        return objectStateRecordEntity;
    }
}
