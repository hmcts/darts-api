package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.ArmRetentionEventDateProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.service.LockService;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class ArmRetentionEventDateCalculatorAutomatedTaskTest {

    @Mock
    private ArmRetentionEventDateProcessor armRetentionEventDateProcessor;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;

    @Test
    void runTask() {
        ArmRetentionEventDateCalculatorAutomatedTask armRetentionEventDateCalculatorAutomatedTask =
            spy(new ArmRetentionEventDateCalculatorAutomatedTask(
                null,
                null,
                armRetentionEventDateProcessor,
                logApi,
                lockService
            ));

        doReturn(123).when(armRetentionEventDateCalculatorAutomatedTask).getAutomatedTaskBatchSize();
        armRetentionEventDateCalculatorAutomatedTask.runTask();
        Mockito.verify(armRetentionEventDateProcessor, Mockito.times(1)).calculateEventDates(123);
        Mockito.verify(armRetentionEventDateCalculatorAutomatedTask, Mockito.times(1)).getAutomatedTaskBatchSize();
    }
}