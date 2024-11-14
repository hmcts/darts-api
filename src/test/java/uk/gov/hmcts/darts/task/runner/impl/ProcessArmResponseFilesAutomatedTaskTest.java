package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.impl.ArmBatchProcessResponseFilesImpl;
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
    private ArmBatchProcessResponseFilesImpl armBatchProcessResponseFiles;
    @Mock
    private LogApi logApi;
    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private LockService lockService;

    @Test
    void runTaskZeroBatchSize() {
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setId(1);
        automatedTask.setTaskName("ProcessArmResponseFiles");
        automatedTask.setBatchSize(0);
        // given
        ProcessArmResponseFilesAutomatedTask processArmResponseFilesAutomatedTask =
            new ProcessArmResponseFilesAutomatedTask(
                automatedTaskRepository,
                null,
                logApi,
                lockService,
                armBatchProcessResponseFiles,
                armResponseFilesProcessor
            );

        when(automatedTaskRepository.findByTaskName("ProcessArmResponseFiles")).thenReturn(Optional.of(automatedTask));
        // when
        processArmResponseFilesAutomatedTask.runTask();

        //then
        Mockito.verify(armResponseFilesProcessor, Mockito.times(1)).processResponseFiles(Integer.MAX_VALUE);
        Mockito.verifyNoInteractions(armBatchProcessResponseFiles);
    }

    @Test
    void runTaskWithPositiveBatchSize() {
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setId(1);
        automatedTask.setTaskName("ProcessArmResponseFiles");
        automatedTask.setBatchSize(10);
        // given
        ProcessArmResponseFilesAutomatedTask processArmResponseFilesAutomatedTask =
            new ProcessArmResponseFilesAutomatedTask(
                automatedTaskRepository,
                null,
                logApi,
                lockService,
                armBatchProcessResponseFiles,
                armResponseFilesProcessor
            );

        when(automatedTaskRepository.findByTaskName("ProcessArmResponseFiles")).thenReturn(Optional.of(automatedTask));
        // when
        processArmResponseFilesAutomatedTask.runTask();

        //then
        Mockito.verify(armBatchProcessResponseFiles, Mockito.times(1)).processResponseFiles(10);
        Mockito.verifyNoInteractions(armResponseFilesProcessor);
    }
}
