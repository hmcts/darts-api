package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsProcessor;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class ApplyRetentionCaseAssociatedObjectsAutomatedTaskTest {

    @Mock
    private ApplyRetentionCaseAssociatedObjectsProcessor applyRetentionCaseAssociatedObjectsProcessor;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;


    @Test
    void runTask() {
        ApplyRetentionCaseAssociatedObjectsAutomatedTask task =
            spy(new ApplyRetentionCaseAssociatedObjectsAutomatedTask(
                null,
                null,
                applyRetentionCaseAssociatedObjectsProcessor,
                logApi,
                lockService
            ));
        doReturn(1234).when(task).getAutomatedTaskBatchSize();

        task.runTask();
        Mockito.verify(applyRetentionCaseAssociatedObjectsProcessor, Mockito.times(1)).processApplyRetentionToCaseAssociatedObjects(1234);
        Mockito.verify(task, Mockito.times(1)).getAutomatedTaskBatchSize();
    }
}
