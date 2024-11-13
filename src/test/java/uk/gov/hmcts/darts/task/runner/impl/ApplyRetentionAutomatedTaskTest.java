package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionProcessor;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

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
            spy(new ApplyRetentionAutomatedTask(
                null,
                null,
                applyRetentionProcessor,
                logApi,
                lockService
            ));

        doReturn(1000).when(applyRetentionAutomatedTaskTest).getAutomatedTaskBatchSize();

        applyRetentionAutomatedTaskTest.runTask();
        Mockito.verify(applyRetentionProcessor, Mockito.times(1)).processApplyRetention(1000);
        Mockito.verify(applyRetentionAutomatedTaskTest, Mockito.times(1)).getAutomatedTaskBatchSize();
    }
}
