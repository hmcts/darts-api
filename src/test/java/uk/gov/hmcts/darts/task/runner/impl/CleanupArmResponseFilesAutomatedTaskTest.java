package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.CleanupArmResponseFilesService;

@ExtendWith(MockitoExtension.class)
class CleanupArmResponseFilesAutomatedTaskTest {

    @Mock
    private LockProvider lockProvider;
    @Mock
    private CleanupArmResponseFilesService cleanupArmResponseFilesService;

    @Test
    void runTask() {
        CleanupArmResponseFilesAutomatedTask cleanupArmResponseFilesAutomatedTask = new CleanupArmResponseFilesAutomatedTask(
                null,
                lockProvider,
                null,
                cleanupArmResponseFilesService
        );

        cleanupArmResponseFilesAutomatedTask.runTask();
        Mockito.verify(cleanupArmResponseFilesService, Mockito.times(1)).cleanupResponseFiles();
    }
}