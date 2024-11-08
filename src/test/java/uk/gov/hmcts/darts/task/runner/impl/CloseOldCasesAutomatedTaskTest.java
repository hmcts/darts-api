package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseOldCasesAutomatedTaskTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @Mock
    private CloseOldCasesProcessor closeOldCasesProcessor;
    private static final int BATCH_SIZE = 5;


    @Test
    void runTask() {
        // given
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setId(1);
        automatedTask.setTaskName("CloseOldCases");
        automatedTask.setBatchSize(BATCH_SIZE);

        CloseOldCasesAutomatedTask closeOldCasesAutomatedTask =
            new CloseOldCasesAutomatedTask(
                automatedTaskRepository,
                automatedTaskConfigurationProperties,
                logApi,
                lockService,
                closeOldCasesProcessor
            );

        when(automatedTaskRepository.findByTaskName(any())).thenReturn(Optional.of(automatedTask));

        // when
        closeOldCasesAutomatedTask.runTask();

        // then
        Mockito.verify(closeOldCasesProcessor, Mockito.times(1)).closeCases(BATCH_SIZE);
    }
}