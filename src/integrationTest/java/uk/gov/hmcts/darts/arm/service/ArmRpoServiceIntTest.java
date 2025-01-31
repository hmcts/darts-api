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

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private ArmRpoService armRpoService;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private MediaEntity media1;
    private MediaEntity media2;

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
            UUID.randomUUID()
        );
        externalObjectDirectoryEntity1.setDataIngestionTs(OffsetDateTime.now().minusHours(26));
        dartsDatabase.save(externalObjectDirectoryEntity1);

        var externalObjectDirectoryEntity2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media2,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID()
        );
        externalObjectDirectoryEntity1.setDataIngestionTs(OffsetDateTime.now().minusHours(26));
        dartsDatabase.save(externalObjectDirectoryEntity1);

        log.info("eod 1: {}, eod 2: {}", externalObjectDirectoryEntity1.getId(), externalObjectDirectoryEntity2.getId());

        armRpoExecutionDetailEntity.setCreatedDateTime(OffsetDateTime.now());

        File file = TestUtils.getFile("tests/arm/rpo/armRpoCsvData.csv");

        // when
        armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, Collections.singletonList(file));

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

    }

    @Test
    void reconcileArmRpoCsvData_DoesNothing_WhenIngestionDateOutsideOfRange() {
        // given
        var externalObjectDirectoryEntity1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID()
        );
        externalObjectDirectoryEntity1.setDataIngestionTs(OffsetDateTime.now().minusHours(1));
        dartsDatabase.save(externalObjectDirectoryEntity1);

        var externalObjectDirectoryEntity2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media2,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID()
        );
        externalObjectDirectoryEntity2.setDataIngestionTs(OffsetDateTime.now().minusHours(100));
        dartsDatabase.save(externalObjectDirectoryEntity2);

        armRpoExecutionDetailEntity.setCreatedDateTime(OffsetDateTime.now());

        File file = TestUtils.getFile("tests/arm/rpo/armRpoCsvData.csv");

        // when
        armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, Collections.singletonList(file));

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
            UUID.randomUUID()
        );
        externalObjectDirectoryEntity.setDataIngestionTs(OffsetDateTime.now().minusHours(26));
        dartsDatabase.save(externalObjectDirectoryEntity);

        armRpoExecutionDetailEntity.setCreatedDateTime(OffsetDateTime.now());

        File file = TestUtils.getFile("tests/arm/rpo/armRpoCsvData.csv");

        // when
        armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, Collections.singletonList(file));

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
            UUID.randomUUID()
        );
        externalObjectDirectoryEntity.setDataIngestionTs(OffsetDateTime.now().minusHours(26));
        dartsDatabase.save(externalObjectDirectoryEntity);

        armRpoExecutionDetailEntity.setCreatedDateTime(OffsetDateTime.now());

        File file = TestUtils.getFile("tests/arm/rpo/armRpoCsvDataNotFoundEods.csv");

        // when
        armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, Collections.singletonList(file));

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(ARM_REPLAY.getId(), foundMedia.getStatus().getId());
    }

    @Test
    void reconcileArmRpoCsvData_DoesNothing_WhenNoEodsInRange() {
        // given
        var externalObjectDirectoryEntity = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1,
            ARM_RPO_PENDING,
            ARM,
            UUID.randomUUID()
        );
        externalObjectDirectoryEntity.setDataIngestionTs(OffsetDateTime.now().minusHours(1));
        dartsDatabase.save(externalObjectDirectoryEntity);

        armRpoExecutionDetailEntity.setCreatedDateTime(OffsetDateTime.now());

        File file = TestUtils.getFile("tests/arm/rpo/armRpoCsvDataNotFoundEods.csv");

        // when
        armRpoService.reconcileArmRpoCsvData(armRpoExecutionDetailEntity, Collections.singletonList(file));

        // then
        List<ExternalObjectDirectoryEntity> foundMediaList = dartsDatabase.getExternalObjectDirectoryRepository()
            .findByMediaAndExternalLocationType(media1, dartsDatabase.getExternalLocationTypeEntity(ARM));

        assertEquals(1, foundMediaList.size());
        ExternalObjectDirectoryEntity foundMedia = foundMediaList.getFirst();
        assertEquals(ARM_RPO_PENDING.getId(), foundMedia.getStatus().getId());
    }

}
