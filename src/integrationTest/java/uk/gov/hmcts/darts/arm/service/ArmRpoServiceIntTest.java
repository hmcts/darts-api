package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.io.File;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_REPLAY;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getArmRpoExecutionDetailTestData;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;

@Slf4j
class ArmRpoServiceIntTest extends PostgresIntegrationBase {

    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 26, 10, 0, 0);
    private static final String PRODUCTION_ID = "ProductionId";
    private static final int BATCH_SIZE = 10;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private ArmRpoService armRpoService;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private MediaEntity media1;
    private MediaEntity media2;
    private MediaEntity media3;
    private MediaEntity media4;
    private MediaEntity media5;
    private MediaEntity media6;

    @BeforeEach
    void setUp() {
        UserAccountEntity userAccountEntity = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        HearingEntity hearing = dartsDatabase.createHearing(
            "Bristol",
            "Int Test Courtroom 1",
            "1",
            HEARING_DATE
        );

        media1 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        media1 = dartsDatabase.save(media1);

        media2 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                2
            ));
        media2 = dartsDatabase.save(media2);

        media3 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                3
            ));
        media3 = dartsDatabase.save(media3);

        media4 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                4
            ));
        media4 = dartsDatabase.save(media4);

        media5 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-27T13:00:00Z"),
                OffsetDateTime.parse("2023-09-27T13:45:00Z"),
                1
            ));
        media5 = dartsDatabase.save(media5);

        media6 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-27T13:00:00Z"),
                OffsetDateTime.parse("2023-09-27T13:45:00Z"),
                2
            ));
        media6 = dartsDatabase.save(media6);

        armRpoExecutionDetailEntity = dartsPersistence.save(getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity());
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.completedRpoStatus());
        armRpoExecutionDetailEntity.setArmRpoState(ArmRpoHelper.saveBackgroundSearchRpoState());
        armRpoExecutionDetailEntity.setMatterId("MatterId");
        armRpoExecutionDetailEntity.setSearchId("SearchId");
        armRpoExecutionDetailEntity.setStorageAccountId("StorageAccountId");
        armRpoExecutionDetailEntity.setProductionId(PRODUCTION_ID);
        armRpoExecutionDetailEntity = dartsPersistence.save(armRpoExecutionDetailEntity);

    }

    @Test
    void reconcileArmRpoCsvData_Success() {
        // given
        var externalObjectDirectoryEntity1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity1.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(26));
        dartsDatabase.save(externalObjectDirectoryEntity1);

        var externalObjectDirectoryEntity2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media2,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity2.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(6));
        dartsDatabase.save(externalObjectDirectoryEntity2);

        var externalObjectDirectoryEntity3 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media3,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity3.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(26));
        dartsDatabase.save(externalObjectDirectoryEntity3);

        var externalObjectDirectoryEntity4 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media4,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity4.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(28));
        dartsDatabase.save(externalObjectDirectoryEntity4);

        var externalObjectDirectoryEntity5 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media5,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID().toString()
        );
        dartsDatabase.save(externalObjectDirectoryEntity5);
        dartsDatabase.getExternalObjectDirectoryRepository().delete(externalObjectDirectoryEntity5);

        var externalObjectDirectoryEntity6 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media6,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity6.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(28));
        dartsDatabase.save(externalObjectDirectoryEntity6);

        log.info("eod 1: {}, eod 2: {}", externalObjectDirectoryEntity1.getId(), externalObjectDirectoryEntity2.getId());

        armRpoExecutionDetailEntity.setCreatedDateTime(OffsetDateTime.now());

        File file = TestUtils.getFile("tests/arm/rpo/armRpoCsvData.csv");

        // when
        armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, Collections.singletonList(file), 2);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList1 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));
        assertEquals(1, foundMediaList1.size());
        ExternalObjectDirectoryEntity foundMedia1 = foundMediaList1.getFirst();
        assertEquals(STORED.getId(), foundMedia1.getStatus().getId());

        List<ExternalObjectDirectoryEntity> foundMediaList2 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media2, dartsDatabase.getExternalLocationTypeEntity(ARM));
        assertEquals(1, foundMediaList2.size());
        ExternalObjectDirectoryEntity foundMedia2 = foundMediaList2.getFirst();
        assertEquals(ARM_RPO_PENDING.getId(), foundMedia2.getStatus().getId());

        List<ExternalObjectDirectoryEntity> foundMediaList3 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media3, dartsDatabase.getExternalLocationTypeEntity(ARM));
        assertEquals(1, foundMediaList3.size());
        ExternalObjectDirectoryEntity foundMedia3 = foundMediaList1.getFirst();
        assertEquals(STORED.getId(), foundMedia3.getStatus().getId());

        List<ExternalObjectDirectoryEntity> foundMediaList4 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));
        assertEquals(1, foundMediaList4.size());
        ExternalObjectDirectoryEntity foundMedia4 = foundMediaList4.getFirst();
        assertEquals(STORED.getId(), foundMedia4.getStatus().getId());

        List<ExternalObjectDirectoryEntity> foundMediaList5 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media5, dartsDatabase.getExternalLocationTypeEntity(ARM));
        assertEquals(0, foundMediaList5.size());

        List<ExternalObjectDirectoryEntity> foundMediaList6 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media6, dartsDatabase.getExternalLocationTypeEntity(ARM));
        assertEquals(1, foundMediaList6.size());
        ExternalObjectDirectoryEntity foundMedia6 = foundMediaList6.getFirst();
        assertEquals(ARM_REPLAY.getId(), foundMedia6.getStatus().getId());
        assertNotNull(foundMedia6.getInputUploadProcessedTs());

    }

    @Test
    void reconcileArmRpoCsvData_DoesNothing_WhenIngestionDateOutsideOfRange() {
        // given
        var externalObjectDirectoryEntity1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity1.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(1));
        dartsDatabase.save(externalObjectDirectoryEntity1);

        var externalObjectDirectoryEntity2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media2,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity2.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(100));
        dartsDatabase.save(externalObjectDirectoryEntity2);

        armRpoExecutionDetailEntity.setCreatedDateTime(OffsetDateTime.now());

        File file = TestUtils.getFile("tests/arm/rpo/armRpoCsvData.csv");

        // when
        armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, Collections.singletonList(file), BATCH_SIZE);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList1 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList1.size());
        ExternalObjectDirectoryEntity foundMedia1 = foundMediaList1.getFirst();
        assertEquals(ARM_RPO_PENDING.getId(), foundMedia1.getStatus().getId());

        List<ExternalObjectDirectoryEntity> foundMediaList2 = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media2, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList2.size());
        ExternalObjectDirectoryEntity foundMedia2 = foundMediaList2.getFirst();
        assertEquals(ARM_RPO_PENDING.getId(), foundMedia2.getStatus().getId());
    }

    @Test
    void reconcileArmRpoCsvData_UpdatesStatus_WhenCsvDataMatches() {
        // given
        var externalObjectDirectoryEntity = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(26));
        dartsDatabase.save(externalObjectDirectoryEntity);

        armRpoExecutionDetailEntity.setCreatedDateTime(OffsetDateTime.now());

        File file = TestUtils.getFile("tests/arm/rpo/armRpoCsvData.csv");

        // when
        armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, Collections.singletonList(file), BATCH_SIZE);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(STORED.getId(), foundMedia.getStatus().getId());
    }

    @Test
    void reconcileArmRpoCsvData_UpdatesEodStatusToReplay_WhenCsvDataContainsEodsThatDoNotMatch() {
        // given
        var externalObjectDirectoryEntity = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(26));
        dartsDatabase.save(externalObjectDirectoryEntity);

        armRpoExecutionDetailEntity.setCreatedDateTime(OffsetDateTime.now());

        File file = TestUtils.getFile("tests/arm/rpo/armRpoCsvDataNotFoundEods.csv");

        // when
        armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, Collections.singletonList(file), BATCH_SIZE);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(ARM_REPLAY.getId(), foundMedia.getStatus().getId());
        assertNotNull(foundMedia.getInputUploadProcessedTs());
    }

    @Test
    void reconcileArmRpoCsvData_DoesNothing_WhenNoEodsInRange() {
        // given
        var externalObjectDirectoryEntity = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(1));
        dartsDatabase.save(externalObjectDirectoryEntity);

        armRpoExecutionDetailEntity.setCreatedDateTime(OffsetDateTime.now());

        File file = TestUtils.getFile("tests/arm/rpo/armRpoCsvDataNotFoundEods.csv");

        // when
        armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, Collections.singletonList(file), BATCH_SIZE);

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(ARM_RPO_PENDING.getId(), foundMedia.getStatus().getId());
    }

}
