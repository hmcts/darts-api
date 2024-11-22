package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.impl.ArmMissingResponseCleanupImpl;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.DartsBatchCleanupArmResponseFilesAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.ARM_MISSING_RESPONSE_REPLY_TASK_NAME;

@ExtendWith(MockitoExtension.class)
class ArmMissingResponseReplayAutomatedTaskTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private DartsBatchCleanupArmResponseFilesAutomatedTaskConfig automatedTaskConfigurationProperties;
    @Mock
    private ArmMissingResponseCleanupImpl armMissingResponseCleanup;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @InjectMocks
    private ArmMissingResponseReplayAutomatedTask armMissingResponseReplayAutomatedTask;

    @Test
    void positiveGetAutomatedTaskName() {
        assertEquals(ARM_MISSING_RESPONSE_REPLY_TASK_NAME, armMissingResponseReplayAutomatedTask.getAutomatedTaskName());
    }

    @Test
    void positiveRunTask() {
        armMissingResponseReplayAutomatedTask = spy(armMissingResponseReplayAutomatedTask);
        doReturn(100).when(armMissingResponseReplayAutomatedTask).getAutomatedTaskBatchSize();
        armMissingResponseReplayAutomatedTask.runTask();

        verify(armMissingResponseCleanup).cleanupResponseFiles(100);
        verify(armMissingResponseReplayAutomatedTask).getAutomatedTaskBatchSize();
    }
}
