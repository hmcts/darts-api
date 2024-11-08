package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.task.service.ManualDeletionProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManualDeletionAutomatedTaskTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;

    @Mock
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;

    @Mock
    private ManualDeletionProcessor manualDeletionProcessor;

    @Mock
    private LogApi logApi;

    @Mock
    private LockService lockService;

    private ManualDeletionAutomatedTask manualDeletionAutomatedTask;

    @BeforeEach
    void setUp() {
        manualDeletionAutomatedTask = new ManualDeletionAutomatedTask(
            automatedTaskRepository,
            automatedTaskConfigurationProperties,
            manualDeletionProcessor,
            logApi,
            lockService
        );
    }

    @Test
    void testGetAutomatedTaskName() {
        assertEquals(AutomatedTaskName.MANUAL_DELETION, manualDeletionAutomatedTask.getAutomatedTaskName());
    }

    @Test
    void testRunTask() {
        manualDeletionAutomatedTask = spy(manualDeletionAutomatedTask);
        doReturn(123).when(manualDeletionAutomatedTask).getAutomatedTaskBatchSize();
        manualDeletionAutomatedTask.runTask();
        verify(manualDeletionProcessor, times(1)).process(123);
        verify(manualDeletionAutomatedTask, times(1)).getAutomatedTaskBatchSize();
    }
}