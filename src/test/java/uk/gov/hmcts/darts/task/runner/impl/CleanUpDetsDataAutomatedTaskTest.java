package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.CleanUpDetsDataProcessor;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.CleanUpDetsDataAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.darts.task.api.AutomatedTaskName.CLEAN_UP_DETS_DATA;

@ExtendWith(MockitoExtension.class)
class CleanUpDetsDataAutomatedTaskTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private CleanUpDetsDataAutomatedTaskConfig automatedTaskConfigurationProperties;
    @Mock
    private CleanUpDetsDataProcessor cleanUpDetsDataProcessor;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;

    @InjectMocks
    private CleanUpDetsDataAutomatedTask cleanUpDetsDataAutomatedTask;

    @Test
    void positiveGetAutomatedTaskName() {
        assertEquals(CLEAN_UP_DETS_DATA, cleanUpDetsDataAutomatedTask.getAutomatedTaskName());
    }

    @Test
    void positiveRunTask() {
        CleanUpDetsDataAutomatedTask automatedTask = spy(cleanUpDetsDataAutomatedTask);
        int batchSize = 25;
        doReturn(batchSize).when(automatedTask).getAutomatedTaskBatchSize();

        automatedTask.runTask();

        verify(automatedTask).getAutomatedTaskBatchSize();
        verify(cleanUpDetsDataProcessor).processCleanUpDetsData(batchSize, automatedTaskConfigurationProperties);
    }
}

