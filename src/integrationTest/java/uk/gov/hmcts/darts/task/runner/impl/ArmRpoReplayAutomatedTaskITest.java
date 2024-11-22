package uk.gov.hmcts.darts.task.runner.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArmRpoReplayAutomatedTask test")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ArmRpoReplayAutomatedTaskITest extends PostgresIntegrationBase {

    private final ArmRpoReplayAutomatedTask armRpoReplayAutomatedTask;
    private final ArmAutomatedTaskRepository armAutomatedTaskRepository;

    @Test
    void positiveTypical() {
        final OffsetDateTime startTs = OffsetDateTime.now().minusDays(1);
        final OffsetDateTime endTs = OffsetDateTime.now();

        transactionalUtil.executeInTransaction(() -> {
            ArmAutomatedTaskEntity armAutomatedTaskEntity = armAutomatedTaskRepository.findById(1).orElseThrow();
            armAutomatedTaskEntity.setArmReplayStartTs(startTs);
            armAutomatedTaskEntity.setArmReplayEndTs(endTs);
            armAutomatedTaskRepository.saveAndFlush(armAutomatedTaskEntity);
        });

        final ObjectRecordStatusEntity correctStatus = dartsDatabase.getObjectRecordStatusRepository().findById(22).orElseThrow();
        final ObjectRecordStatusEntity incorrectStatus = dartsDatabase.getObjectRecordStatusRepository().findById(1).orElseThrow();
        final OffsetDateTime correctLastModifiedDateTime1 = startTs.plusMinutes(1);
        final OffsetDateTime correctLastModifiedDateTime2 = endTs.minusMinutes(1);
        final OffsetDateTime incorrectLastModifiedDateTime1 = endTs.plusMinutes(1);
        final OffsetDateTime incorrectLastModifiedDateTime2 = startTs.minusMinutes(1);


        final ExternalObjectDirectoryEntity insideRangeCorrectStatus1 = createEod(correctStatus, correctLastModifiedDateTime1);
        final ExternalObjectDirectoryEntity insideRangeCorrectStatus2 = createEod(correctStatus, correctLastModifiedDateTime2);

        final ExternalObjectDirectoryEntity insideRangeIncorrectStatus = createEod(incorrectStatus, correctLastModifiedDateTime1);

        final ExternalObjectDirectoryEntity outsideRangeCorrectStatus1 = createEod(correctStatus, incorrectLastModifiedDateTime1);
        final ExternalObjectDirectoryEntity outsideRangeCorrectStatus2 = createEod(correctStatus, incorrectLastModifiedDateTime2);

        transactionalUtil.executeInTransaction(() -> {
            armRpoReplayAutomatedTask.runTask();
        });
        //Commit the old transaction and start a new one to ensure the changes are visible
        transactionalUtil.executeInTransaction(() -> {
            assertEodUpdated(insideRangeCorrectStatus1);
            assertEodUpdated(insideRangeCorrectStatus2);

            assertEodNotUpdated(insideRangeIncorrectStatus);
            assertEodNotUpdated(outsideRangeCorrectStatus1);
            assertEodNotUpdated(outsideRangeCorrectStatus2);
        });
    }

    private void assertEodUpdated(ExternalObjectDirectoryEntity oldEod) {
        transactionalUtil.executeInTransaction(() -> {
            ExternalObjectDirectoryEntity newEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(oldEod.getId()).orElseThrow();
            assertThat(newEod.getStatus().getId()).isEqualTo(14);
            assertThat(newEod.getTransferAttempts()).isEqualTo(0);
        });
    }

    private void assertEodNotUpdated(ExternalObjectDirectoryEntity oldEod) {
        transactionalUtil.executeInTransaction(() -> {
            ExternalObjectDirectoryEntity newEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(oldEod.getId()).orElseThrow();
            assertThat(newEod.getStatus().getId()).isEqualTo(oldEod.getStatus().getId());
            //Ensures root object not updated
            assertThat(newEod.getStatus().getId()).isNotEqualTo(14);
            assertThat(newEod.getStatus().getId()).isEqualTo(oldEod.getStatus().getId());
            assertThat(newEod.getTransferAttempts()).isEqualTo(oldEod.getTransferAttempts());
        });
    }

    @SneakyThrows
    private ExternalObjectDirectoryEntity createEod(ObjectRecordStatusEntity status, OffsetDateTime lastModifiedDateTime) {
        ExternalObjectDirectoryEntity eod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .status(status)
            .lastModifiedDateTime(lastModifiedDateTime)
            .build().getEntity();
        dartsDatabase.save(eod.getMedia().getCourtroom().getCourthouse());
        dartsDatabase.save(eod.getMedia().getCourtroom());
        dartsDatabase.save(eod.getMedia());
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = dartsDatabase.save(eod);

        dartsDatabase.getExternalObjectDirectoryStub()
            .getDateConfigurer()
            .setLastModifiedDateNoRefresh(eod, lastModifiedDateTime);
        return externalObjectDirectoryEntity;
    }
}
