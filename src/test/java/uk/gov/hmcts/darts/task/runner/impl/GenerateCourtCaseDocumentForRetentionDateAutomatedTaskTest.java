package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentForRetentionDateProcessor;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.service.LockService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateCourtCaseDocumentForRetentionDateAutomatedTaskTest {

    public static final int BATCH_SIZE = 50;
    @Mock
    private GenerateCaseDocumentForRetentionDateProcessor processor;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @Mock
    private AutomatedTaskRepository automatedTaskRepository;

    @Test
    void runTask() {
        AutomatedTaskEntity automatedTask = new AutomatedTaskEntity();
        automatedTask.setBatchSize(BATCH_SIZE);
        when(automatedTaskRepository.findByTaskName(any())).thenReturn(Optional.of(automatedTask));
        GenerateCaseDocumentForRetentionDateAutomatedTask task = new GenerateCaseDocumentForRetentionDateAutomatedTask(
            automatedTaskRepository,
            null,
            logApi,
            lockService,
            processor
        );

        task.runTask();

        verify(processor).processGenerateCaseDocumentForRetentionDate(BATCH_SIZE);
    }
}
