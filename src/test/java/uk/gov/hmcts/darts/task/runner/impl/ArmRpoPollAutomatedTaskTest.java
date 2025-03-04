package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.ArmRpoPollService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.ArmRpoPollAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoPollAutomatedTaskTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private ArmRpoPollService armRpoPollService;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @Mock
    private ArmRpoPollAutomatedTaskConfig armRpoPollAutomatedTaskConfig;

    @Test
    void runTask() {
        // given
        Duration pollDuration = Duration.ofSeconds(5);
        when(armRpoPollAutomatedTaskConfig.getPollDuration()).thenReturn(pollDuration);

        ArmRpoPollingAutomatedTask armRpoPollAutomatedTask = new ArmRpoPollingAutomatedTask(
            automatedTaskRepository,
            armRpoPollAutomatedTaskConfig,
            armRpoPollService,
            logApi,
            lockService
        );

        // when
        armRpoPollAutomatedTask.runTask();

        // then
        verify(armRpoPollService, times(1)).pollArmRpo(false, pollDuration, 0);
    }
}