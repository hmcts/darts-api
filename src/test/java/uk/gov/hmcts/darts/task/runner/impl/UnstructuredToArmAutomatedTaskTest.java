package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.component.UnstructuredToArmProcessorFactory;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmProcessorImpl;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnstructuredToArmAutomatedTaskTest {
    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private LockProvider lockProvider;
    @Mock
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;
    @Mock
    UnstructuredToArmProcessorFactory processorFactory;
    @Mock
    private LogApi logApi;
    @Mock
    UnstructuredToArmProcessorImpl unstructuredToArmProcessor;
    @Mock
    UnstructuredToArmBatchProcessorImpl unstructuredToArmBatchProcessor;

    @Test
    void runTask() {
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setId(1);
        automatedTask.setTaskName("UnstructuredToArmDataStore");

        UnstructuredToArmAutomatedTask unstructuredToArmAutomatedTask =
            new UnstructuredToArmAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                processorFactory,
                logApi
            );

        when(automatedTaskRepository.findByTaskName("UnstructuredToArmDataStore")).thenReturn(Optional.of(automatedTask));
        when(processorFactory.createUnstructuredToArmProcessor(false)).thenReturn(unstructuredToArmProcessor);

        unstructuredToArmAutomatedTask.runTask();

        //then
        Mockito.verify(unstructuredToArmProcessor, Mockito.times(1)).processUnstructuredToArm();
    }

    @Test
    void runTaskInBatchMode() {
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setId(1);
        automatedTask.setTaskName("UnstructuredToArmDataStore");
        automatedTask.setBatchSize(1);

        UnstructuredToArmAutomatedTask unstructuredToArmAutomatedTask =
            new UnstructuredToArmAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                processorFactory,
                logApi
            );

        when(automatedTaskRepository.findByTaskName("UnstructuredToArmDataStore")).thenReturn(Optional.of(automatedTask));
        when(processorFactory.createUnstructuredToArmProcessor(true)).thenReturn(unstructuredToArmBatchProcessor);

        unstructuredToArmAutomatedTask.runTask();

        //then
        Mockito.verify(unstructuredToArmBatchProcessor, Mockito.times(1)).processUnstructuredToArm();
    }

    @Test
    void runTaskInvalidTaskName() {
        UnstructuredToArmAutomatedTask unstructuredToArmAutomatedTask =
            new UnstructuredToArmAutomatedTask(
                automatedTaskRepository,
                lockProvider,
                automatedTaskConfigurationProperties,
                processorFactory,
                logApi
            );

        UnstructuredToArmAutomatedTask spy = Mockito.spy(unstructuredToArmAutomatedTask);
        when(automatedTaskRepository.findByTaskName("UnstructuredToArmDataStore")).thenReturn(Optional.empty());

        spy.runTask();

        Mockito.verify(spy, Mockito.times(1)).handleException(any());

    }
}

