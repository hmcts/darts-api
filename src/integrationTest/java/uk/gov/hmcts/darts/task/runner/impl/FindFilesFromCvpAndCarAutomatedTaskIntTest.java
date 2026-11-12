package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.service.FindFilesFromCvpAndCarService;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.JobLastSuccessfulRunTimeEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.JobLastSuccessfulRunTimeRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.task.config.FindFilesFromCvpAndCarAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@Execution(ExecutionMode.SAME_THREAD)
class FindFilesFromCvpAndCarAutomatedTaskIntTest extends PostgresIntegrationBase {

    private static final String JOB_NAME = "find-files-from-cvp-and-car";
    private static final Instant PREVIOUS_RUN_TIME = Instant.parse("2026-11-06T10:00:00Z");

    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;
    @MockitoSpyBean
    private JobLastSuccessfulRunTimeRepository lastRunRepository;
    @MockitoSpyBean
    private FindFilesFromCvpAndCarAutomatedTask task;

    @Autowired
    private FindFilesFromCvpAndCarService service;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private FindFilesFromCvpAndCarAutomatedTaskConfig config;

    @Test
    @Transactional
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
    @Transactional
    void given_noPreviousRun_when_runTask_then_createTimestamp() {
        Instant before = Instant.now();

        task.runTask();

        assertThat(lastRunRepository.findById("find-files-from-cvp-and-car").orElseThrow().getLastSuccessfulRunTimestampUtc())
            .isBetween(before, Instant.now());
    }

    @Test
    @Transactional
    void given_previousRun_when_findFiles_then_updateSingleRow() {
        JobLastSuccessfulRunTimeEntity previous = new JobLastSuccessfulRunTimeEntity();
        previous.setJobName("find-files-from-cvp-and-car");
        previous.setLastSuccessfulRunTimestampUtc(Instant.EPOCH);
        lastRunRepository.saveAndFlush(previous);
        Instant before = Instant.now();

        service.findFilesFromCvpAndCar(1000);
        lastRunRepository.flush();

        assertThat(lastRunRepository.findById("find-files-from-cvp-and-car").orElseThrow().getLastSuccessfulRunTimestampUtc())
            .isBetween(before, Instant.now());
        assertThat(lastRunRepository.count()).isEqualTo(1);
    }

    @Test
    @Transactional
    void given_disabledTask_when_scheduledRun_then_doNotCreateTimestamp() {
        task.setLastCronExpression("0 */10 * * * *");

        task.run(false);

        assertThat(lastRunRepository.findById("find-files-from-cvp-and-car")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void given_enabledTask_when_runFailsAfterTimestampWrite_then_rollbackTimestamp(boolean previousRunExists) {
        AutomatedTaskEntity taskDetails = automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow();
        boolean originallyEnabled = taskDetails.getTaskEnabled();
        prepareTask(taskDetails, previousRunExists);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            // Send the change to PostgreSQL before failing, so the test proves database rollback.
            lastRunRepository.flush();
            throw new IllegalStateException("Simulated failure after writing the successful-run timestamp");
        }).when(lastRunRepository).save(any(JobLastSuccessfulRunTimeEntity.class));

        try {
            task.run(false);

            assertThat(task.getAutomatedTaskStatus()).isEqualTo(AutomatedTaskStatus.FAILED);
            verify(lastRunRepository).save(any(JobLastSuccessfulRunTimeEntity.class));
            assertCheckpoint(previousRunExists);
        } finally {
            restoreTask(taskDetails, originallyEnabled);
        }
    }

    @ParameterizedTest
    @CsvSource({"false, true", "true, true", "false, false", "true, false"})
    void given_connectionFailure_when_scheduledRun_then_preserveCheckpointAndRecover(boolean previousRunExists, boolean readFailure) {
        AutomatedTaskEntity taskDetails = automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow();
        boolean originallyEnabled = taskDetails.getTaskEnabled();
        prepareTask(taskDetails, previousRunExists);
        CannotGetJdbcConnectionException failure = new CannotGetJdbcConnectionException("Simulated database connection failure");
        if (readFailure) {
            doThrow(failure).when(lastRunRepository).findById(JOB_NAME);
        } else {
            doThrow(failure).when(lastRunRepository).save(any(JobLastSuccessfulRunTimeEntity.class));
        }

        try {
            task.run(false);

            assertThat(task.getAutomatedTaskStatus()).isEqualTo(AutomatedTaskStatus.FAILED);
            if (readFailure) {
                verify(lastRunRepository, never()).save(any(JobLastSuccessfulRunTimeEntity.class));
            } else {
                verify(lastRunRepository).save(any(JobLastSuccessfulRunTimeEntity.class));
            }
            reset(lastRunRepository);
            assertCheckpoint(previousRunExists);

            Instant beforeRecovery = Instant.now();
            task.run(false);

            assertThat(task.getAutomatedTaskStatus()).isEqualTo(AutomatedTaskStatus.COMPLETED);
            assertThat(lastRunRepository.findById(JOB_NAME).orElseThrow().getLastSuccessfulRunTimestampUtc())
                .isBetween(beforeRecovery, Instant.now());
        } finally {
            restoreTask(taskDetails, originallyEnabled);
        }
    }

    private void prepareTask(AutomatedTaskEntity taskDetails, boolean previousRunExists) {
        taskDetails.setTaskEnabled(true);
        automatedTaskRepository.saveAndFlush(taskDetails);
        lastRunRepository.deleteById(JOB_NAME);
        if (previousRunExists) {
            JobLastSuccessfulRunTimeEntity previousRun = new JobLastSuccessfulRunTimeEntity();
            previousRun.setJobName(JOB_NAME);
            previousRun.setLastSuccessfulRunTimestampUtc(PREVIOUS_RUN_TIME);
            lastRunRepository.saveAndFlush(previousRun);
        }
        task.setLastCronExpression(taskDetails.getCronExpression());
        // Keep the real locking flow without waiting one minute between test invocations.
        doReturn(Duration.ofMillis(1)).when(task).getLockAtLeastFor();
    }

    private void assertCheckpoint(boolean previousRunExists) {
        if (previousRunExists) {
            assertThat(lastRunRepository.findById(JOB_NAME).orElseThrow().getLastSuccessfulRunTimestampUtc())
                .isEqualTo(PREVIOUS_RUN_TIME);
        } else {
            assertThat(lastRunRepository.findById(JOB_NAME)).isEmpty();
        }
    }

    private void restoreTask(AutomatedTaskEntity taskDetails, boolean originallyEnabled) {
        reset(lastRunRepository);
        lastRunRepository.deleteById(JOB_NAME);
        taskDetails.setTaskEnabled(originallyEnabled);
        automatedTaskRepository.saveAndFlush(taskDetails);
    }

}
