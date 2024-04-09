package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.impl.ArmResponseFilesProcessorImpl;

@ExtendWith(MockitoExtension.class)
class ProcessArmResponseFilesAutomatedTaskTest {
    @Mock
    private LockProvider lockProvider;
    @Mock
    private ArmResponseFilesProcessorImpl armResponseFilesProcessor;

    @Test
    void runTask() {
        // given
        ProcessArmResponseFilesAutomatedTask processArmResponseFilesAutomatedTask =
            new ProcessArmResponseFilesAutomatedTask(
                null,
                lockProvider,
                null,
                armResponseFilesProcessor
            );

        // when
        processArmResponseFilesAutomatedTask.runTask();

        //then
        Mockito.verify(armResponseFilesProcessor, Mockito.times(1)).processResponseFiles();
    }
}
