package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.JobLastSuccessfulRunTimeEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.JobLastSuccessfulRunTimeRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.task.config.FindFilesFromCvpAndCarAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
class FindFilesFromCvpAndCarAutomatedTaskIntTest extends PostgresIntegrationBase {

    private static final String JOB_NAME = "find-files-from-cvp-and-car";
    private static final Instant PREVIOUS_RUN_TIME = Instant.parse("2026-11-06T10:00:00Z");
    private static final Instant NOW_UTC_TIME = Instant.parse("2026-11-06T10:10:00Z");

    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;
    @MockitoSpyBean
    private JobLastSuccessfulRunTimeRepository lastRunRepository;
    @MockitoSpyBean
    private FindFilesFromCvpAndCarAutomatedTask task;

    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private FindFilesFromCvpAndCarAutomatedTaskConfig config;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUpClock() {
        when(clock.instant()).thenReturn(NOW_UTC_TIME);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @AfterEach
    void cleanUp() {
        reset(lastRunRepository);
        lastRunRepository.deleteById(JOB_NAME);

        AutomatedTaskEntity taskDetails =
            automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow();

        taskDetails.setTaskEnabled(false);
        automatedTaskRepository.saveAndFlush(taskDetails);
    }

    @Test
    @Transactional
    void findByTaskName_returnsDisabledTask_whenFindFilesTaskExistsAndDisabled() {
        AutomatedTaskEntity taskDetails =
            automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow();

        taskDetails.setTaskEnabled(false);
        automatedTaskRepository.saveAndFlush(taskDetails);

        assertThat(automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow().getTaskEnabled())
            .isFalse();
    }

    @Test
    @Transactional
    void findByTaskName_returnsEnabledTask_whenFindFilesTaskExistsAndEnabled() {
        AutomatedTaskEntity taskDetails =
            automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow();

        taskDetails.setTaskEnabled(true);
        automatedTaskRepository.saveAndFlush(taskDetails);

        assertThat(automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow().getTaskEnabled())
            .isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "2026-11-06T23:59:59, 2026-11-07T00:00:00",
        "2026-11-07T00:00:00, 2026-11-07T00:10:00",
        "2026-11-07T00:00:01, 2026-11-07T00:10:00"
    })
    @Transactional
    void findByTaskName_returnsTenMinuteSchedule_whenFindFilesTaskExists(
        LocalDateTime currentTime, LocalDateTime expectedNextRun
    ) {
        AutomatedTaskEntity taskDetails =
            automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow();

        CronExpression cron = CronExpression.parse(taskDetails.getCronExpression());

        assertThat(cron.next(currentTime)).isEqualTo(expectedNextRun);
    }

    @Test
    @Transactional
    void findFirstByEmailAddressIgnoreCase_returnsUser_whenUsingConfiguredSystemUser() {
        assertThat(userAccountRepository.findFirstByEmailAddressIgnoreCase(config.getSystemUserEmail()))
            .isPresent();
    }

    @Test
    @Transactional
    void runTask_shouldCreateLastSuccessfulRunTimestamp_whenNoPreviousRunExists() {
        assertThat(lastRunRepository.findById(JOB_NAME)).isEmpty();

        task.runTask();
        lastRunRepository.flush();

        assertThat(lastRunRepository.findById(JOB_NAME).orElseThrow().getLastSuccessfulRunTimestampUtc())
            .isEqualTo(NOW_UTC_TIME);
    }

    @Test
    @Transactional
    void runTask_shouldUpdateLastSuccessfulRunTimestamp_whenPreviousRunExists() {
        savePreviousSuccessfulRun();

        task.runTask();
        lastRunRepository.flush();

        assertThat(lastRunRepository.findById(JOB_NAME).orElseThrow().getLastSuccessfulRunTimestampUtc())
            .isEqualTo(NOW_UTC_TIME);
    }

    @Test
    @Transactional
    void run_shouldNotCreateLastSuccessfulRunTimestamp_whenTaskDisabled() {
        AutomatedTaskEntity taskDetails =
            automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow();

        assertThat(taskDetails.getTaskEnabled()).isFalse();
        task.setLastCronExpression(taskDetails.getCronExpression());

        task.run(false);

        assertThat(task.getAutomatedTaskStatus()).isEqualTo(AutomatedTaskStatus.SKIPPED);
        assertThat(lastRunRepository.findById(JOB_NAME)).isEmpty();
    }

    @Test
    void run_shouldNotUpdateLastSuccessfulRunTimestamp_whenReadingFails() {
        savePreviousSuccessfulRun();
        enableTaskAndSetUpForExecution();

        doThrow(new CannotGetJdbcConnectionException("Simulated read failure"))
            .when(lastRunRepository).findById(JOB_NAME);

        task.run(false);

        assertThat(task.getAutomatedTaskStatus()).isEqualTo(AutomatedTaskStatus.FAILED);
        verify(lastRunRepository).findById(JOB_NAME);
        verify(lastRunRepository, never()).save(any(JobLastSuccessfulRunTimeEntity.class));

        reset(lastRunRepository);

        assertThat(lastRunRepository.findById(JOB_NAME).orElseThrow().getLastSuccessfulRunTimestampUtc())
            .isEqualTo(PREVIOUS_RUN_TIME);
    }

    @Test
    void run_shouldNotUpdateLastSuccessfulRunTimestamp_whenSavingFails() {
        savePreviousSuccessfulRun();
        enableTaskAndSetUpForExecution();

        doThrow(new CannotGetJdbcConnectionException("Simulated save failure"))
            .when(lastRunRepository).save(any(JobLastSuccessfulRunTimeEntity.class));

        task.run(false);

        assertThat(task.getAutomatedTaskStatus()).isEqualTo(AutomatedTaskStatus.FAILED);
        verify(lastRunRepository).save(any(JobLastSuccessfulRunTimeEntity.class));

        reset(lastRunRepository);

        assertThat(lastRunRepository.findById(JOB_NAME).orElseThrow().getLastSuccessfulRunTimestampUtc())
            .isEqualTo(PREVIOUS_RUN_TIME);
    }

    private void savePreviousSuccessfulRun() {
        JobLastSuccessfulRunTimeEntity previousRun = new JobLastSuccessfulRunTimeEntity();
        previousRun.setJobName(JOB_NAME);
        previousRun.setLastSuccessfulRunTimestampUtc(PREVIOUS_RUN_TIME);
        lastRunRepository.saveAndFlush(previousRun);
    }

    private void enableTaskAndSetUpForExecution() {
        AutomatedTaskEntity taskDetails =
            automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow();

        taskDetails.setTaskEnabled(true);
        automatedTaskRepository.saveAndFlush(taskDetails);

        task.setLastCronExpression(taskDetails.getCronExpression());
        // Avoid waiting one minute between task executions.
        doReturn(Duration.ofMillis(1)).when(task).getLockAtLeastFor();
    }
}
