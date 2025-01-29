package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getArmRpoExecutionDetailTestData;

@Slf4j
public class ArmRpoReplayServiceIntTest extends PostgresIntegrationBase {

    @MockBean
    private UserIdentity userIdentity;

    private ArmAutomatedTaskEntity armAutomatedTaskEntity;
    private OffsetDateTime startTs = OffsetDateTime.now().minusMinutes(60);
    private OffsetDateTime endTs = OffsetDateTime.now().minusMinutes(10);
    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;

    @Autowired
    private ArmRpoReplayService armRpoReplayService;


    @BeforeEach
    void setUp() {
        UserAccountEntity userAccountEntity = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        lenient().when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armRpoExecutionDetailEntity = dartsPersistence.save(getArmRpoExecutionDetailTestData().minimalArmRpoExecutionDetailEntity());

        populateArmAutomatedTaskEntity();
    }

    @Test
    void replayArmRpo_updatesCorrectEntities() throws Exception {

        // given
        OffsetDateTime validDateTime = OffsetDateTime.now().minusMinutes(20);
        OffsetDateTime invalidDateTime = OffsetDateTime.now().minusMinutes(200);

        var validEods = dartsDatabase.getExternalObjectDirectoryStub().generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RPO_PENDING, 2, Optional.of(validDateTime));
        validEods.forEach(eod -> {
            eod.setTransferAttempts(1);
            dartsPersistence.getExternalObjectDirectoryRepository().saveAndFlush(eod);
        });

        var invalidEods = dartsDatabase.getExternalObjectDirectoryStub().generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RPO_PENDING, 2, Optional.of(invalidDateTime));
        invalidEods.forEach(eod -> {
            eod.setTransferAttempts(1);
            dartsPersistence.getExternalObjectDirectoryRepository().saveAndFlush(eod);
        });

        // when
        armRpoReplayService.replayArmRpo(10);

        // then
        var validEodResults = dartsPersistence.getExternalObjectDirectoryRepository().findAllById(
            validEods.stream().map(eod -> eod.getId()).toList());
        var invalidEodResults = dartsPersistence.getExternalObjectDirectoryRepository().findAllById(
            invalidEods.stream().map(eod -> eod.getId()).toList());


        validEodResults.forEach(eod -> {
            assertEquals(ARM_RAW_DATA_FAILED.getId(), eod.getStatus().getId());
            assertEquals(0, eod.getTransferAttempts());
        });

        invalidEodResults.forEach(eod -> {
            assertEquals(ARM_RPO_PENDING.getId(), eod.getStatus().getId());
            assertEquals(1, eod.getTransferAttempts());
        });
    }

    private void populateArmAutomatedTaskEntity() {
        ArmAutomatedTaskEntity taskEntity = dartsPersistence.getArmAutomatedTaskRepository().findById(1).orElseThrow();
        taskEntity.setArmReplayStartTs(startTs);
        taskEntity.setArmReplayEndTs(endTs);
        armAutomatedTaskEntity = dartsPersistence.getArmAutomatedTaskRepository().saveAndFlush(taskEntity);
    }

}
