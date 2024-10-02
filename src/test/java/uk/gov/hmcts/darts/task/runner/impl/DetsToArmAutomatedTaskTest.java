package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.DetsToArmBatchPushProcessor;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.DETS_TO_ARM_TASK_NAME;

@ExtendWith(MockitoExtension.class)
class DetsToArmAutomatedTaskTest {
    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @Mock
    DetsToArmBatchPushProcessor detsToArmProcessor;

    @Test
    void runTask() {
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setId(1);
        automatedTask.setTaskName(DETS_TO_ARM_TASK_NAME.getTaskName());
        int batchSize = 10_000;
        automatedTask.setBatchSize(batchSize);

        DetsToArmPushAutomatedTask detsToArmAutomatedTask =
            new DetsToArmPushAutomatedTask(
                automatedTaskRepository,
                automatedTaskConfigurationProperties,
                detsToArmProcessor,
                logApi,
                lockService
            );

        when(automatedTaskRepository.findByTaskName(DETS_TO_ARM_TASK_NAME.getTaskName())).thenReturn(Optional.of(automatedTask));

        detsToArmAutomatedTask.runTask();

        //then
        Mockito.verify(detsToArmProcessor, Mockito.times(1)).processDetsToArm(batchSize);
    }
}