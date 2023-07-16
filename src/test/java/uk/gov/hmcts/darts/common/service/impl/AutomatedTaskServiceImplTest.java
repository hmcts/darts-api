package uk.gov.hmcts.darts.common.service.impl;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.task.AutomatedTaskOne;
import uk.gov.hmcts.darts.common.task.AutomatedTaskTwo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class AutomatedTaskServiceImplTest {

    @InjectMocks
    private AutomatedTaskServiceImpl automatedTaskService;

    @Test
    void getAutomatedTaskUsingAutomatedTaskOne() {
        List<AutomatedTaskEntity> automatedTaskEntities =
            automatedTaskService.getAutomatedTaskEntitiesByTaskName(AutomatedTaskOne.TASKNAME);
        assertNotNull(automatedTaskEntities);
    }

    @Test
    void getAutomatedTaskCronExpression() {
        String cronExpression = automatedTaskService.getAutomatedTaskCronExpression(new AutomatedTaskTwo());
        String expectedCronExpression = "*/12 * * * * *";
        assertEquals(expectedCronExpression, cronExpression);
    }



    @Test
    void createAutomatedTaskTrigger() {

    }

    @Test
    void cancelAutomatedTask() {

    }
}
