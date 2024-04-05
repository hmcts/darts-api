package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsProcessor;

@ExtendWith(MockitoExtension.class)
class ApplyRetentionCaseAssociatedObjectsAutomatedTaskTest {
    @Mock
    private LockProvider lockProvider;
    @Mock
    private ApplyRetentionCaseAssociatedObjectsProcessor applyRetentionCaseAssociatedObjectsProcessor;

    @Test
    void runTask() {
        ApplyRetentionCaseAssociatedObjectsAutomatedTask task =
            new ApplyRetentionCaseAssociatedObjectsAutomatedTask(
                null,
                lockProvider,
                null,
                applyRetentionCaseAssociatedObjectsProcessor
            );

        task.runTask();
        Mockito.verify(applyRetentionCaseAssociatedObjectsProcessor, Mockito.times(1)).processApplyRetentionToCaseAssociatedObjects();
    }
}
