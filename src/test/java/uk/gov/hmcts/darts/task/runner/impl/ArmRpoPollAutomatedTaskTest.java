package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.ArmRpoPollService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.service.LockService;

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

    @Test
    void runTask() {

        // given
        ArmRpoPollingAutomatedTask armRpoPollAutomatedTask = new ArmRpoPollingAutomatedTask(
            null,
            null,
            armRpoPollService,
            logApi,
            lockService
        );

        // when
        armRpoPollAutomatedTask.runTask();

        // then
        Mockito.verify(armRpoPollService, Mockito.times(1)).pollArmRpo(false);
    }
}