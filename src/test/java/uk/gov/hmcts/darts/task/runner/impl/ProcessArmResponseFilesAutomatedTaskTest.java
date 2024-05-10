package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.arm.service.impl.ArmResponseFilesProcessorImpl;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessArmResponseFilesAutomatedTaskTest {
    @Mock
    private LockProvider lockProvider;
    @Mock
    private ArmResponseFilesProcessorImpl armResponseFilesProcessor;
    @Mock
    private LogApi logApi;
    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private AutomatedTaskProcessorFactory processorFactory;

    @Test
    void runTask() {
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setId(1);
        automatedTask.setTaskName("ProcessArmResponseFiles");
        // given
        ProcessArmResponseFilesAutomatedTask processArmResponseFilesAutomatedTask =
            new ProcessArmResponseFilesAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                null,
                processorFactory,
                logApi
            );

        when(automatedTaskRepository.findByTaskName("ProcessArmResponseFiles")).thenReturn(Optional.of(automatedTask));
        when(processorFactory.createArmResponseFilesProcessor(false)).thenReturn(armResponseFilesProcessor);

        // when
        processArmResponseFilesAutomatedTask.runTask();

        //then
        Mockito.verify(armResponseFilesProcessor, Mockito.times(1)).processResponseFiles();
    }
}
