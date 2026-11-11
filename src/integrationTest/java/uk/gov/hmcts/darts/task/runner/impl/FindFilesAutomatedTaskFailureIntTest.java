package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.JobLastSuccessfulRunTimeEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.JobLastSuccessfulRunTimeRepository;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@Execution(ExecutionMode.SAME_THREAD)
class FindFilesAutomatedTaskFailureIntTest extends PostgresIntegrationBase {

    private static final String JOB_NAME = "find_files";
    private static final Instant PREVIOUS_RUN_TIME = Instant.parse("2026-11-06T10:00:00Z");

    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;
    @MockitoSpyBean
    private JobLastSuccessfulRunTimeRepository lastRunRepository;
    @MockitoSpyBean
    private FindFilesAutomatedTask task;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void given_enabledTask_when_runFailsAfterTimestampWrite_then_rollbackTimestamp(boolean previousRunExists) {
        AutomatedTaskEntity taskDetails = automatedTaskRepository.findByTaskName("FindFilesFromCVPAndCAR").orElseThrow();
        boolean originallyEnabled = taskDetails.getTaskEnabled();
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
            if (previousRunExists) {
                assertThat(lastRunRepository.findById(JOB_NAME).orElseThrow().getLastSuccessfulRunTimestampUtc())
                    .isEqualTo(PREVIOUS_RUN_TIME);
            } else {
                assertThat(lastRunRepository.findById(JOB_NAME)).isEmpty();
            }
        } finally {
            lastRunRepository.deleteById(JOB_NAME);
            taskDetails.setTaskEnabled(originallyEnabled);
            automatedTaskRepository.saveAndFlush(taskDetails);
        }
    }
}
