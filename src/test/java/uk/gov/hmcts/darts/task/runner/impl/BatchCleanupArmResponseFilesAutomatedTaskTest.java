package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.impl.DartsBatchCleanupArmResponseFilesServiceImpl;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.DartsBatchCleanupArmResponseFilesAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.BATCH_CLEANUP_ARM_RESPONSE_FILES_TASK_NAME;

@ExtendWith(MockitoExtension.class)
class BatchCleanupArmResponseFilesAutomatedTaskTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private DartsBatchCleanupArmResponseFilesAutomatedTaskConfig automatedTaskConfigurationProperties;
    @Mock
    private DartsBatchCleanupArmResponseFilesServiceImpl batchCleanupArmResponseFilesService;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @InjectMocks
    private BatchCleanupArmResponseFilesAutomatedTask batchCleanupArmResponseFilesAutomatedTask;

    @Test
    void positiveGetAutomatedTaskName() {
        assertEquals(BATCH_CLEANUP_ARM_RESPONSE_FILES_TASK_NAME, batchCleanupArmResponseFilesAutomatedTask.getAutomatedTaskName());
    }

    @Test
    void positiveRunTask() {
        batchCleanupArmResponseFilesAutomatedTask = spy(batchCleanupArmResponseFilesAutomatedTask);
        doReturn(100).when(batchCleanupArmResponseFilesAutomatedTask).getAutomatedTaskBatchSize();
        batchCleanupArmResponseFilesAutomatedTask.runTask();

        verify(batchCleanupArmResponseFilesService).cleanupResponseFiles(100);
        verify(batchCleanupArmResponseFilesAutomatedTask).getAutomatedTaskBatchSize();
    }
}
