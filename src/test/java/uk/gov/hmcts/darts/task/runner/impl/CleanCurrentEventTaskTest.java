package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.component.AutomatedTaskProcessorFactory;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.event.service.CleanupCurrentFlagEventProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanCurrentEventTaskTest {

    public static final int BATCH_SIZE = 50;
    @Mock
    private LockProvider lockProvider;
    @Mock
    private AutomatedTaskProcessorFactory factory;
    @Mock
    private CleanupCurrentFlagEventProcessor processor;
    @Mock
    private LogApi logApi;
    @Mock
    private AutomatedTaskRepository automatedTaskRepository;

    @Test
    void runTask() {
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setBatchSize(BATCH_SIZE);
        when(automatedTaskRepository.findByTaskName(any())).thenReturn(Optional.of(automatedTask));
        when(factory.createCleanupCurrentFlagEventProcessor(BATCH_SIZE)).thenReturn(processor);
        CleanupCurrentEventTask task = new CleanupCurrentEventTask(
            automatedTaskRepository,
            lockProvider,
            null,
            factory,
            logApi
        );

        task.runTask();

        verify(processor).processCurrentEvent();
    }
}