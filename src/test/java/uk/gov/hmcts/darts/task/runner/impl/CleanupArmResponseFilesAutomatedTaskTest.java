package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.CleanupArmResponseFilesService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.service.LockService;

@ExtendWith(MockitoExtension.class)
class CleanupArmResponseFilesAutomatedTaskTest {

    @Mock
    private CleanupArmResponseFilesService cleanupArmResponseFilesService;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;


    @Test
    void runTask() {
        CleanupArmResponseFilesAutomatedTask cleanupArmResponseFilesAutomatedTask = new CleanupArmResponseFilesAutomatedTask(
                null,
                null,
                cleanupArmResponseFilesService,
                logApi,
                lockService
        );

        cleanupArmResponseFilesAutomatedTask.runTask();
        Mockito.verify(cleanupArmResponseFilesService, Mockito.times(1)).cleanupResponseFiles();
    }
}