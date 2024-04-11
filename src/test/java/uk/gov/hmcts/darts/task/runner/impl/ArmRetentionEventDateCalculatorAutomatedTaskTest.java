package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;

class ArmRetentionEventDateCalculatorAutomatedTaskTest {

    @Mock
    private LockProvider lockProvider;
    @Mock
    private ArmRetentionEventDateProcessor armRetentionEventDateProcessor;

    @Test
    void runTask() {
        ArmRetentionEventDateCalculatorAutomatedTask armRetentionEventDateCalculatorAutomatedTask =
            new ArmRetentionEventDateCalculatorAutomatedTask(
                null,
                lockProvider,
                null,
                armRetentionEventDateProcessor
            );

        armRetentionEventDateCalculatorAutomatedTask.runTask();
        Mockito.verify(armRetentionEventDateProcessor, Mockito.times(1)).calculateEventDates();
    }
}