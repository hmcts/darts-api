package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.darts.arm.service.ArmBatchProcessResponseFiles;

import static org.mockito.Mockito.verify;

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