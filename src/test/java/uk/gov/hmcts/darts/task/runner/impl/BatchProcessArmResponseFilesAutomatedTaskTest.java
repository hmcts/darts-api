package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.ArmBatchProcessResponseFiles;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BatchProcessArmResponseFilesAutomatedTaskTest {

    @Mock
    private LockProvider lockProvider;

    @Mock
    private ArmBatchProcessResponseFiles armBatchProcessResponseFiles;

    @Test
    void runTask() {
        BatchProcessArmResponseFilesAutomatedTask batchProcessArmResponseFilesAutomatedTask = new BatchProcessArmResponseFilesAutomatedTask(
            null,
            lockProvider,
            null,
            armBatchProcessResponseFiles
        );

        batchProcessArmResponseFilesAutomatedTask.runTask();

        verify(armBatchProcessResponseFiles).batchProcessResponseFiles();
    }
}