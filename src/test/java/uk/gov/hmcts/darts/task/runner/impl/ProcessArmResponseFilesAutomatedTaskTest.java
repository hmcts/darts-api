package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;

@ExtendWith(MockitoExtension.class)
class ProcessArmResponseFilesAutomatedTaskTest {
    @Mock
    private LockProvider lockProvider;
    @Mock
    private ArmResponseFilesProcessor armResponseFilesProcessor;

    @Test
    void runTask() {
        ProcessArmResponseFilesAutomatedTask processArmResponseFilesAutomatedTask =
            new ProcessArmResponseFilesAutomatedTask(
                null,
                lockProvider,
                null,
                armResponseFilesProcessor
            );

        processArmResponseFilesAutomatedTask.runTask();
        Mockito.verify(armResponseFilesProcessor, Mockito.times(1)).processResponseFiles();
    }
}
