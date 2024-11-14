package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.impl.DetsBatchCleanupArmResponseFilesServiceImpl;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.BatchCleanupArmResponseFilesAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.DETS_CLEANUP_ARM_RESPONSE_FILES;

@ExtendWith(MockitoExtension.class)
class DetsCleanupArmResponseFilesAutomatedTaskTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private BatchCleanupArmResponseFilesAutomatedTaskConfig automatedTaskConfigurationProperties;
    @Mock
    private DetsBatchCleanupArmResponseFilesServiceImpl batchCleanupArmResponseFilesService;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;

    @InjectMocks
    private DetsCleanupArmResponseFilesAutomatedTask detsCleanupArmResponseFilesAutomatedTask;

    @Test
    void positiveGetAutomatedTaskName() {
        assertEquals(DETS_CLEANUP_ARM_RESPONSE_FILES, detsCleanupArmResponseFilesAutomatedTask.getAutomatedTaskName());
    }

    @Test
    void positiveRunTask() {
        detsCleanupArmResponseFilesAutomatedTask = spy(detsCleanupArmResponseFilesAutomatedTask);
        doReturn(100).when(detsCleanupArmResponseFilesAutomatedTask).getAutomatedTaskBatchSize();
        detsCleanupArmResponseFilesAutomatedTask.runTask();

        verify(batchCleanupArmResponseFilesService).cleanupResponseFiles(100);
        verify(detsCleanupArmResponseFilesAutomatedTask).getAutomatedTaskBatchSize();
    }
}
