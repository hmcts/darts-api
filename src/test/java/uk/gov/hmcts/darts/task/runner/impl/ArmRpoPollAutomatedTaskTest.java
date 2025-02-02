package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.ArmRpoPollService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.ArmRpoPollAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

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
        armRpoPollAutomatedTaskConfig.setPollDuration(Duration.ofSeconds(5));
        // given
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
        Mockito.verify(armRpoPollService, Mockito.times(1)).pollArmRpo(false, Duration.ofSeconds(5), 1);
    }
}