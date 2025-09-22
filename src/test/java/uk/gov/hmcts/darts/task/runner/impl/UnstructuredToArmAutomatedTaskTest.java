package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.config.UnstructuredToArmAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import java.util.Optional;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnstructuredToArmAutomatedTaskTest {
    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @Mock
    private UnstructuredToArmBatchProcessorImpl unstructuredToArmBatchProcessor;

    @Test
    void runTask() {
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setId(1);
        automatedTask.setTaskName("UnstructuredToArmDataStore");

        UnstructuredToArmAutomatedTask unstructuredToArmAutomatedTask =
            spy(new UnstructuredToArmAutomatedTask(
                automatedTaskRepository,
                mock(UnstructuredToArmAutomatedTaskConfig.class),
                unstructuredToArmBatchProcessor,
                logApi,
                lockService
            ));
        doReturn(10).when(unstructuredToArmAutomatedTask).getAutomatedTaskBatchSize();

        unstructuredToArmAutomatedTask.runTask();

        //then
        Mockito.verify(unstructuredToArmBatchProcessor, Mockito.times(1)).processUnstructuredToArm(10);
        Mockito.verify(unstructuredToArmAutomatedTask, Mockito.times(1)).getAutomatedTaskBatchSize();
    }

    @Test
    void runTaskInBatchMode() {
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setId(1);
        automatedTask.setTaskName("UnstructuredToArmDataStore");
        automatedTask.setBatchSize(10);

        UnstructuredToArmAutomatedTaskConfig config = mock(UnstructuredToArmAutomatedTaskConfig.class);

        UnstructuredToArmAutomatedTask unstructuredToArmAutomatedTask =
            new UnstructuredToArmAutomatedTask(
                automatedTaskRepository,
                config,
                unstructuredToArmBatchProcessor,
                logApi,
                lockService
            );

        when(automatedTaskRepository.findByTaskName("UnstructuredToArmDataStore")).thenReturn(Optional.of(automatedTask));
        
        unstructuredToArmAutomatedTask.runTask();

        //then
        Mockito.verify(unstructuredToArmBatchProcessor, Mockito.times(1)).processUnstructuredToArm(10);
    }
}

