package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.ArmBatchProcessResponseFiles;

@ExtendWith(MockitoExtension.class)
class ProcessArmResponseFilesAutomatedTaskTest {
    @Mock
    private LockProvider lockProvider;
    @Mock
    private ArmBatchProcessResponseFiles armBatchProcessResponseFiles;

    @Test
    void runTask() {
        // given
        ProcessArmResponseFilesAutomatedTask processArmResponseFilesAutomatedTask =
            new ProcessArmResponseFilesAutomatedTask(
                null,
                lockProvider,
                null,
                armBatchProcessResponseFiles
            );

        // when
        processArmResponseFilesAutomatedTask.runTask();

        //then
        Mockito.verify(armBatchProcessResponseFiles, Mockito.times(1)).batchProcessResponseFiles();
    }
}
