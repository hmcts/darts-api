package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionProcessor;
import uk.gov.hmcts.darts.task.config.ApplyRetentionAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyRetentionAutomatedTaskTest {
    
    @Mock
    private ApplyRetentionProcessor applyRetentionProcessor;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @Mock
    private ApplyRetentionAutomatedTaskConfig applyRetentionAutomatedTaskConfig;

    private final Duration daysBetweenEvents = Duration.ofDays(10);

    @Test
    void runTask() {
        when(applyRetentionAutomatedTaskConfig.getDaysBetweenEvents()).thenReturn(daysBetweenEvents);
        ApplyRetentionAutomatedTask applyRetentionAutomatedTaskTest =
            spy(new ApplyRetentionAutomatedTask(
                null,
                applyRetentionAutomatedTaskConfig,
                applyRetentionProcessor,
                logApi,
                lockService
            ));

        doReturn(1000).when(applyRetentionAutomatedTaskTest).getAutomatedTaskBatchSize();

        applyRetentionAutomatedTaskTest.runTask();
        Mockito.verify(applyRetentionProcessor, Mockito.times(1)).processApplyRetention(1000, daysBetweenEvents);
        Mockito.verify(applyRetentionAutomatedTaskTest, Mockito.times(1)).getAutomatedTaskBatchSize();
    }
}
