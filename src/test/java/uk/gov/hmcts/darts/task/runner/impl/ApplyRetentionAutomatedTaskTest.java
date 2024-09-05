package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionProcessor;
import uk.gov.hmcts.darts.task.service.LockService;

@ExtendWith(MockitoExtension.class)
class ApplyRetentionAutomatedTaskTest {
    @Mock
    private ApplyRetentionProcessor applyRetentionProcessor;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;

    @Test
    void runTask() {
        ApplyRetentionAutomatedTask applyRetentionAutomatedTaskTest =
            new ApplyRetentionAutomatedTask(
                null,
                null,
                applyRetentionProcessor,
                logApi,
                lockService
            );

        applyRetentionAutomatedTaskTest.runTask();
        Mockito.verify(applyRetentionProcessor, Mockito.times(1)).processApplyRetention();
    }
}
