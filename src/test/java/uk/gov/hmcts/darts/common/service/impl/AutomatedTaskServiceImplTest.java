package uk.gov.hmcts.darts.common.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.Trigger;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.task.AutomatedTask;
import uk.gov.hmcts.darts.common.task.AutomatedTaskOne;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutomatedTaskServiceImplTest {

    @InjectMocks
    private AutomatedTaskServiceImpl automatedTaskService;

    @Mock
    private AutomatedTaskRepository mockAutomatedTaskRepository;

    @Test
    void getAutomatedTaskUsingAutomatedTaskOne() {
        AutomatedTaskOne automatedTaskOne = new AutomatedTaskOne();
        List<AutomatedTaskEntity> automatedTaskEntities1 = new ArrayList<>();
        automatedTaskEntities1.add(createAutomatedTaskEntity(automatedTaskOne));
        when(mockAutomatedTaskRepository.findAllByTaskName(automatedTaskOne.getTaskName()))
            .thenReturn(automatedTaskEntities1);

        List<AutomatedTaskEntity> automatedTaskEntities2 =
            automatedTaskService.getAutomatedTaskEntitiesByTaskName(AutomatedTaskOne.TASKNAME);
        assertNotNull(automatedTaskEntities2);
    }

    @Test
    void createAutomatedTaskTrigger() {
        Trigger taskTrigger = automatedTaskService.createAutomatedTaskTrigger(new AutomatedTaskOne());
        assertNotNull(taskTrigger);
    }

    private AutomatedTaskEntity createAutomatedTaskEntity(AutomatedTask automatedTask) {
        AutomatedTaskEntity automatedTaskEntity1 = new AutomatedTaskEntity();
        automatedTaskEntity1.setId(1);
        automatedTaskEntity1.setTaskName(automatedTask.getTaskName());
        automatedTaskEntity1.setCronExpression(automatedTask.getDefaultCronExpression());
        automatedTaskEntity1.setTaskDescription("Test Automated Task");
        return automatedTaskEntity1;
    }
}
