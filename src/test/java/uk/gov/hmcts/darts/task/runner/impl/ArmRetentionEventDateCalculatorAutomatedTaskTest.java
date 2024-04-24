package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;

@ExtendWith(MockitoExtension.class)
class ArmRetentionEventDateCalculatorAutomatedTaskTest {

    @Mock
    private LockProvider lockProvider;
    @Mock
    private ArmRetentionEventDateProcessor armRetentionEventDateProcessor;
    @Mock
    private LogApi logApi;

    @Test
    void runTask() {
        ArmRetentionEventDateCalculatorAutomatedTask armRetentionEventDateCalculatorAutomatedTask =
            new ArmRetentionEventDateCalculatorAutomatedTask(
                null,
                lockProvider,
                null,
                armRetentionEventDateProcessor,
                logApi
            );

        armRetentionEventDateCalculatorAutomatedTask.runTask();
        Mockito.verify(armRetentionEventDateProcessor, Mockito.times(1)).calculateEventDates();
    }
}