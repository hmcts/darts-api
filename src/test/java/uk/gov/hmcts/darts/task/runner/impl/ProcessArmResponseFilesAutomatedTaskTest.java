package uk.gov.hmcts.darts.task.runner.impl;

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
import uk.gov.hmcts.darts.task.service.LockService;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessArmResponseFilesAutomatedTaskTest {
    @Mock
    private ArmResponseFilesProcessorImpl armResponseFilesProcessor;
    @Mock
    private LogApi logApi;
    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private AutomatedTaskProcessorFactory processorFactory;
    @Mock
    private LockService lockService;

    @Test
    void runTask() {
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setId(1);
        automatedTask.setTaskName("ProcessArmResponseFiles");
        automatedTask.setBatchSize(10);
        // given
        ProcessArmResponseFilesAutomatedTask processArmResponseFilesAutomatedTask =
            new ProcessArmResponseFilesAutomatedTask(
                automatedTaskRepository,
                null,
                processorFactory,
                logApi,
                lockService
            );

        when(automatedTaskRepository.findByTaskName("ProcessArmResponseFiles")).thenReturn(Optional.of(automatedTask));
        when(processorFactory.createArmResponseFilesProcessor(10)).thenReturn(armResponseFilesProcessor);

        // when
        processArmResponseFilesAutomatedTask.runTask();

        //then
        Mockito.verify(armResponseFilesProcessor, Mockito.times(1)).processResponseFiles();
    }
}
