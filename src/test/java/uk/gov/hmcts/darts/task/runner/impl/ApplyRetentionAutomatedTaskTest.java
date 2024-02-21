package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionProcessor;

@ExtendWith(MockitoExtension.class)
class ApplyRetentionAutomatedTaskTest {
    @Mock
    private LockProvider lockProvider;
    @Mock
    private ApplyRetentionProcessor applyRetentionProcessor;

    @Test
    void runTask() {
        ApplyRetentionAutomatedTask applyRetentionAutomatedTaskTest =
            new ApplyRetentionAutomatedTask(
                null,
                lockProvider,
                null,
                applyRetentionProcessor
            );

        applyRetentionAutomatedTaskTest.runTask();
        Mockito.verify(applyRetentionProcessor, Mockito.times(1)).processApplyRetention();
    }
}
