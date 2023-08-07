package uk.gov.hmcts.darts.task.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ProcessDailyListAutomatedTask;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class AutomatedTaskServiceImplTest {

    @InjectMocks
    private AutomatedTaskServiceImpl automatedTaskService;

    @Mock
    private AutomatedTaskRepository mockAutomatedTaskRepository;

    @Mock
    private LockProvider mockLockProvider;

    @Test
    void getAutomatedTaskUsingProcessDailyListAutomatedTask() {
        AutomatedTask processDailyListAutomatedTask = new ProcessDailyListAutomatedTask(mockAutomatedTaskRepository, mockLockProvider);
        assertEquals(Duration.ofSeconds(20), processDailyListAutomatedTask.getLockConfiguration().getLockAtLeastFor());
        assertEquals(Duration.ofSeconds(600), processDailyListAutomatedTask.getLockConfiguration().getLockAtMostFor());

        AutomatedTaskEntity expectedAutomatedTaskEntity = createAutomatedTaskEntity(processDailyListAutomatedTask, "*/7 * * * * *");
        when(mockAutomatedTaskRepository.findByTaskName(processDailyListAutomatedTask.getTaskName()))
            .thenReturn(Optional.of(expectedAutomatedTaskEntity));

        Optional<AutomatedTaskEntity> actualAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(processDailyListAutomatedTask.getTaskName());
        assertEquals(expectedAutomatedTaskEntity, actualAutomatedTaskEntity.get());
    }

    @Test
    void updateNonExistingAutomatedTaskCronExpressionThrowsException() {

        AutomatedTask automatedTask = new AutomatedTask() {
            private String lastCronExpression = "*/10 * * * * *";

            @Override
            public String getTaskName() {
                return "Test";
            }

            @Override
            public AutomatedTaskStatus getAutomatedTaskStatus() {
                return AutomatedTaskStatus.NOT_STARTED;
            }

            @Override
            public LockConfiguration getLockConfiguration() {
                return new LockConfiguration(Instant.now(), getTaskName(), getLockAtMostFor(), getLockAtLeastFor());
            }

            @Override
            public String getLastCronExpression() {
                return lastCronExpression;
            }

            @Override
            public void setLastCronExpression(String cronExpression) {
                this.lastCronExpression = lastCronExpression;
            }

            protected Duration getLockAtMostFor() {
                return Duration.ofSeconds(20);
            }

            protected static Duration getLockAtLeastFor() {
                return Duration.ofSeconds(10);
            }

            @Override
            public void run() {
                log.debug("Running test automated task");
            }
        };
        assertThrows(DartsApiException.class, () ->
            automatedTaskService.updateAutomatedTaskCronExpression(automatedTask.getTaskName(), "*/8 * * * * *"));
    }

    @Test
    void updateNonExistingAbstractLockableAutomatedTaskCronExpressionThrowsException() {

        AbstractLockableAutomatedTask automatedTask = new AbstractLockableAutomatedTask(mockAutomatedTaskRepository, mockLockProvider) {
            @Override
            public String getTaskName() {
                return "Test";
            }

            @Override
            public AutomatedTaskStatus getAutomatedTaskStatus() {
                return AutomatedTaskStatus.NOT_STARTED;
            }

            @Override
            protected void runTask() {
                throw new IllegalArgumentException("Tests throws exception");
            }

            @Override
            protected void handleException(Exception exception) {
                log.debug("Exception {}", exception.getMessage());
            }

            @Override
            public void run() {
                log.debug("Running test automated task");
            }
        };
        assertThrows(DartsApiException.class, () ->
            automatedTaskService.updateAutomatedTaskCronExpression(automatedTask.getTaskName(), "*/8 * * * * *"));
    }
    
    private AutomatedTaskEntity createAutomatedTaskEntity(AutomatedTask automatedTask, String cronExpression) {
        AutomatedTaskEntity automatedTaskEntity1 = new AutomatedTaskEntity();
        automatedTaskEntity1.setId(1);
        automatedTaskEntity1.setTaskName(automatedTask.getTaskName());
        automatedTaskEntity1.setCronExpression(cronExpression);
        automatedTaskEntity1.setTaskDescription("Test Automated Task");
        return automatedTaskEntity1;
    }
}
