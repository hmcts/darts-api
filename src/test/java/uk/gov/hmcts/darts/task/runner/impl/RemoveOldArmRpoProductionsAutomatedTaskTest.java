package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.RemoveRpoProductionsService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.RemoveRpoProductionsAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class RemoveOldArmRpoProductionsAutomatedTaskTest {

    @Mock
    private RemoveRpoProductionsAutomatedTaskConfig removeRpoProductionsAutomatedTaskConfig;
    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private RemoveRpoProductionsService removeRpoProductionsService;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;


    @Test
    void runTask() {
        //given
        Duration waitDuration = Duration.ofDays(30);
        when(removeRpoProductionsAutomatedTaskConfig.getWaitDuration()).thenReturn(waitDuration);
        
        RemoveOldArmRpoProductionsAutomatedTask removeOldArmRpoProductionsAutomatedTask =
            new RemoveOldArmRpoProductionsAutomatedTask(
                automatedTaskRepository,
                removeRpoProductionsService,
                removeRpoProductionsAutomatedTaskConfig,
                logApi,
                lockService
            );
        
        //when
        removeOldArmRpoProductionsAutomatedTask.runTask();
        
        //then
        verify(removeRpoProductionsService, times(1)).removeOldArmRpoProductions(false, waitDuration,
            0);
    }
    
}
