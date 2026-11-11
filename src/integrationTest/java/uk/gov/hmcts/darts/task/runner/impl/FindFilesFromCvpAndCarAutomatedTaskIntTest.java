package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.service.FindFilesService;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.JobLastSuccessfulRunTimeEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.JobLastSuccessfulRunTimeRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.task.config.FindFilesFromCvpAndCarAutomatedTaskConfig;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class FindFilesFromCvpAndCarAutomatedTaskIntTest extends PostgresIntegrationBase {

    @Autowired
    private FindFilesFromCvpAndCarAutomatedTask task;
    @Autowired
    private FindFilesService service;
    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;
    @Autowired
    private JobLastSuccessfulRunTimeRepository lastRunRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private FindFilesFromCvpAndCarAutomatedTaskConfig config;

    @Test
    void given_migration_when_taskLoaded_then_scheduleAndUserAndLocksMatch() {
        AutomatedTaskEntity taskDetails = automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow();
        assertThat(taskDetails.getTaskDescription()).isEqualTo(
            "Find files from the CVP and CAR containers in the cvp-recordings blob store, store their location, and record the last successful run time");
        assertThat(automatedTaskRepository.findByTaskName("FindFiles")).isEmpty();
        assertThat(taskDetails.getTaskEnabled()).isFalse();
        assertThat(taskDetails.getBatchSize()).isEqualTo(1000);
        CronExpression cron = CronExpression.parse(taskDetails.getCronExpression());
        LocalDateTime firstRun = cron.next(LocalDateTime.of(2026, 11, 6, 10, 0));
        assertThat(firstRun).isEqualTo(LocalDateTime.of(2026, 11, 6, 10, 10));
        assertThat(cron.next(firstRun)).isEqualTo(firstRun.plusMinutes(10));
        assertThat(config.getLock().getAtLeastFor()).isEqualTo(Duration.ofMinutes(1));
        assertThat(config.getLock().getAtMostFor()).isEqualTo(Duration.ofMinutes(9));
        assertThat(userAccountRepository.findFirstByEmailAddressIgnoreCase(config.getSystemUserEmail()).orElseThrow().getId()).isEqualTo(-54);
    }

    @Test
    void given_noPreviousRun_when_runTask_then_createTimestamp() {
        Instant before = Instant.now();

        task.runTask();

        assertThat(lastRunRepository.findById("find_files").orElseThrow().getLastSuccessfulRunTimestampUtc())
            .isBetween(before, Instant.now());
    }

    @Test
    void given_previousRun_when_findFiles_then_updateSingleRow() {
        JobLastSuccessfulRunTimeEntity previous = new JobLastSuccessfulRunTimeEntity();
        previous.setJobName("find_files");
        previous.setLastSuccessfulRunTimestampUtc(Instant.EPOCH);
        lastRunRepository.saveAndFlush(previous);
        Instant before = Instant.now();

        service.findFiles(1000);
        lastRunRepository.flush();

        assertThat(lastRunRepository.findById("find_files").orElseThrow().getLastSuccessfulRunTimestampUtc())
            .isBetween(before, Instant.now());
        assertThat(lastRunRepository.count()).isEqualTo(1);
    }

    @Test
    void given_disabledTask_when_scheduledRun_then_doNotCreateTimestamp() {
        task.setLastCronExpression("0 */10 * * * *");

        task.run(false);

        assertThat(lastRunRepository.findById("find_files")).isEmpty();
    }
}
