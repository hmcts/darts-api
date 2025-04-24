package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.MediaRequestCleanUpAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.task.service.ManualDeletionProcessor;

import java.time.Duration;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaRequestCleanUpAutomatedTaskTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private MediaRequestCleanUpAutomatedTaskConfig config;
    @Mock
    private ManualDeletionProcessor manualDeletionProcessor;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private MediaRequestRepository mediaRequestRepository;

    private MediaRequestCleanUpAutomatedTask mediaRequestCleanUpAutomatedTask;

    @BeforeEach
    void setUp() {
        mediaRequestCleanUpAutomatedTask = new MediaRequestCleanUpAutomatedTask(
            automatedTaskRepository,
            config,
            logApi,
            lockService,
            currentTimeHelper,
            mediaRequestRepository
        );
    }

    @Test
    void runTask_shouldCallCleanupStuckRequests_withTheCorrectMaxTime() {
        OffsetDateTime currentTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentTime);

        Duration maxStuckTimeDuration = Duration.ofDays(1);
        when(config.getMaxStuckDuration()).thenReturn(maxStuckTimeDuration);

        mediaRequestCleanUpAutomatedTask.runTask();

        verify(mediaRequestRepository).cleanupStuckRequests(currentTime.minus(maxStuckTimeDuration));
    }

    @Test
    void getAutomatedTaskName_shouldReturnCorrectName() {
        assertThat(mediaRequestCleanUpAutomatedTask.getAutomatedTaskName())
            .isEqualTo(AutomatedTaskName.MEDIA_REQUEST_CLEANUP);
    }
}
