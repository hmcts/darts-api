package uk.gov.hmcts.darts.task.api.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.service.LockService;

@ExtendWith(MockitoExtension.class)
class AutomatedTasksApiImplTest {

    @InjectMocks
    private AutomatedTasksApiImpl automatedTasksApi;

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;

    @Mock
    private LockService lockService;

    @Test
    void getTaskByName() {
        var taskName = "TestTask";
        automatedTasksApi.getTaskByName(taskName);
        Mockito.verify(automatedTaskRepository).findByTaskName(taskName);
    }

    @Test
    void isLocked() {
        AutomatedTaskEntity automatedTaskEntity = new AutomatedTaskEntity();
        automatedTasksApi.isLocked(automatedTaskEntity);
        Mockito.verify(lockService).isLocked(automatedTaskEntity);
    }

    @Test
    void getLockingTaskExecutor() {
        automatedTasksApi.getLockingTaskExecutor();
        Mockito.verify(lockService).getLockingTaskExecutor();
    }

    @Test
    void getLockAtMostFor() {
        automatedTasksApi.getLockAtMostFor();
        Mockito.verify(lockService).getLockAtMostFor();
    }

    @Test
    void getLockAtLeastFor() {
        automatedTasksApi.getLockAtLeastFor();
        Mockito.verify(lockService).getLockAtLeastFor();
    }
}