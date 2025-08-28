package uk.gov.hmcts.darts.task.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.config.ProcessDailyListAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskSetupError;
import uk.gov.hmcts.darts.task.model.AutomatedTaskTrigger;
import uk.gov.hmcts.darts.task.runner.AutoloadingAutomatedTask;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.runner.impl.ProcessDailyListAutomatedTask;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class AutomatedTaskServiceImplTest {

    @Spy
    private final List<AutoloadingAutomatedTask> autoloadingAutomatedTasks = new ArrayList<>();

    @Mock
    private AutomatedTaskRepository mockAutomatedTaskRepository;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ScheduledTaskHolder scheduledTaskHolder;

    @Mock
    private AutomatedTaskConfigurationProperties mockAutomatedTaskConfigurationProperties;

    @Mock
    private LogApi logApi;

    @Mock
    private LockService lockService;

    private AutomatedTaskServiceImpl automatedTaskService;

    @BeforeEach
    void before() {
        this.automatedTaskService = new AutomatedTaskServiceImpl(
            mockAutomatedTaskRepository,
            scheduledTaskHolder,
            taskScheduler,
            mock(UserIdentity.class),
            autoloadingAutomatedTasks,
            true
        );
    }

    @Test
    void getAutomatedTaskUsingProcessDailyListAutomatedTask() {
        AutomatedTask processDailyListAutomatedTask = new ProcessDailyListAutomatedTask(
            mockAutomatedTaskRepository,
            mock(ProcessDailyListAutomatedTaskConfig.class),
            null,
            logApi,
            lockService
        );

        AutomatedTaskEntity expectedAutomatedTaskEntity = createAutomatedTaskEntity(
            processDailyListAutomatedTask,
            "*/7 * * * * *"
        );
        when(mockAutomatedTaskRepository.findByTaskName(processDailyListAutomatedTask.getTaskName()))
            .thenReturn(Optional.of(expectedAutomatedTaskEntity));

        Optional<AutomatedTaskEntity> actualAutomatedTaskEntity =
            automatedTaskService.getAutomatedTaskEntityByTaskName(processDailyListAutomatedTask.getTaskName());
        assertEquals(expectedAutomatedTaskEntity, actualAutomatedTaskEntity.get());
    }

    @Test
    void getAutomatedTaskCronExpressionWithNullCronExpression() {
        AutomatedTask processDailyListAutomatedTask = new ProcessDailyListAutomatedTask(
            mockAutomatedTaskRepository,
            mock(ProcessDailyListAutomatedTaskConfig.class),
            null,
            logApi,
            lockService
        );
        AutomatedTaskEntity expectedAutomatedTaskEntity = createAutomatedTaskEntity(
            processDailyListAutomatedTask,
            null
        );
        when(mockAutomatedTaskRepository.findByTaskName(processDailyListAutomatedTask.getTaskName()))
            .thenReturn(Optional.of(expectedAutomatedTaskEntity));
        AutomatedTask automatedTask = createAutomatedTask("ProcessDailyList");
        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> automatedTaskService.getAutomatedTaskCronExpression(automatedTask)
        );
        assertEquals("Invalid cron expression", exception.getError().getTitle());

    }

    @Test
    void reloadByTaskName() {
        ScheduledTask scheduledTask = mock(ScheduledTask.class);
        Set<ScheduledTask> scheduledTaskList = new HashSet<>();
        scheduledTaskList.add(scheduledTask);
        AbstractLockableAutomatedTask automatedTask = new AbstractLockableAutomatedTask<>(
            mockAutomatedTaskRepository,
            mockAutomatedTaskConfigurationProperties,
            logApi,
            lockService) {
            @Override
            protected void runTask() {
                // empty method
            }

            @Override
            protected void handleException(Exception exception) {
                // empty method
            }

            @Override
            public AutomatedTaskName getAutomatedTaskName() {
                return AutomatedTaskName.PROCESS_DAILY_LIST_TASK_NAME;
            }
        };
        autoloadingAutomatedTasks.add(automatedTask);
        Trigger trigger = triggerContext -> null;
        AutomatedTaskTrigger task = new AutomatedTaskTrigger(automatedTask, trigger);
        when(scheduledTaskHolder.getScheduledTasks()).thenReturn(scheduledTaskList);
        when(scheduledTask.getTask()).thenReturn(task);

        automatedTaskService.reloadTaskByName("ProcessDailyList");

        verify(taskScheduler).schedule(automatedTask, trigger);
    }

    @Test
    void reloadByTaskNameApplyRetention() {
        ScheduledTask scheduledTask = mock(ScheduledTask.class);
        Set<ScheduledTask> scheduledTaskList = new HashSet<>();
        scheduledTaskList.add(scheduledTask);

        AbstractLockableAutomatedTask automatedTask = new AbstractLockableAutomatedTask<>(
            mockAutomatedTaskRepository,
            mockAutomatedTaskConfigurationProperties,
            logApi,
            lockService) {
            @Override
            protected void runTask() {
                // empty method
            }

            @Override
            protected void handleException(Exception exception) {
                // empty method
            }

            @Override
            public AutomatedTaskName getAutomatedTaskName() {
                return AutomatedTaskName.APPLY_RETENTION_TASK_NAME;
            }
        };
        autoloadingAutomatedTasks.add(automatedTask);
        Trigger trigger = triggerContext -> null;
        AutomatedTaskTrigger task = new AutomatedTaskTrigger(automatedTask, trigger);
        when(scheduledTaskHolder.getScheduledTasks()).thenReturn(scheduledTaskList);
        when(scheduledTask.getTask()).thenReturn(task);

        automatedTaskService.reloadTaskByName("ApplyRetention");

        verify(taskScheduler).schedule(automatedTask, trigger);
    }

    @Test
    void reloadByTaskNameDailyListHouseKeeping() {
        ScheduledTask scheduledTask = mock(ScheduledTask.class);
        Set<ScheduledTask> scheduledTaskList = new HashSet<>();
        scheduledTaskList.add(scheduledTask);

        var automatedTask = new AbstractLockableAutomatedTask<>(
            mockAutomatedTaskRepository,
            mockAutomatedTaskConfigurationProperties,
            logApi,
            lockService) {
            @Override
            protected void runTask() {
                // empty method
            }

            @Override
            protected void handleException(Exception exception) {
                // empty method
            }

            @Override
            public AutomatedTaskName getAutomatedTaskName() {
                return AutomatedTaskName.DAILY_LIST_HOUSEKEEPING_TASK_NAME;
            }
        };
        autoloadingAutomatedTasks.add(automatedTask);
        Trigger trigger = triggerContext -> null;
        AutomatedTaskTrigger task = new AutomatedTaskTrigger(automatedTask, trigger);
        when(scheduledTaskHolder.getScheduledTasks()).thenReturn(scheduledTaskList);
        when(scheduledTask.getTask()).thenReturn(task);

        automatedTaskService.reloadTaskByName("DailyListHousekeeping");

        verify(taskScheduler).schedule(automatedTask, trigger);
    }


    @Test
    void overrideTaskLockDurations() {
        when(lockService.getLockAtMostFor()).thenReturn(Duration.ofMinutes(20));
        when(lockService.getLockAtLeastFor()).thenReturn(Duration.ofMinutes(1));

        var automatedTask = new AbstractLockableAutomatedTask<>(
            mockAutomatedTaskRepository,
            mockAutomatedTaskConfigurationProperties,
            logApi,
            lockService) {
            @Override
            protected void runTask() {
                // empty method
            }

            @Override
            protected void handleException(Exception exception) {
                // empty method
            }

            @Override
            public String getTaskName() {
                return "StandardTask";
            }

            @Override
            public AutomatedTaskName getAutomatedTaskName() {
                return null;
            }
        };
        var overriddenAutomatedTask = new AbstractLockableAutomatedTask<>(
            mockAutomatedTaskRepository,
            mockAutomatedTaskConfigurationProperties,
            logApi,
            lockService) {
            @Override
            protected void runTask() {
                // empty method
            }

            @Override
            protected void handleException(Exception exception) {
                // empty method
            }

            @Override
            public Duration getLockAtMostFor() {
                return Duration.ofDays(1);
            }

            @Override
            public Duration getLockAtLeastFor() {
                return Duration.ofSeconds(1);
            }

            @Override
            public String getTaskName() {
                return "TaskWithOverriddenLockTimes";
            }

            @Override
            public AutomatedTaskName getAutomatedTaskName() {
                return null;
            }
        };

        assertEquals(Duration.ofMinutes(1), automatedTask.getLockAtLeastFor());
        assertEquals(Duration.ofMinutes(20), automatedTask.getLockAtMostFor());

        assertEquals(Duration.ofSeconds(1), overriddenAutomatedTask.getLockAtLeastFor());
        assertEquals(Duration.ofDays(1), overriddenAutomatedTask.getLockAtMostFor());
    }

    @Test
    void taskFailedToBeStartedMovesToFailedStatus() {
        when(mockAutomatedTaskConfigurationProperties.getSystemUserEmail()).thenReturn("system@darts.test");
        UserAccountEntity userAccount = new UserAccountEntity();
        when(lockService.getLockAtMostFor()).thenReturn(Duration.of(1, ChronoUnit.HOURS));

        var failingAutomatedTask = new AbstractLockableAutomatedTask<>(
            mockAutomatedTaskRepository,
            mockAutomatedTaskConfigurationProperties,
            logApi,
            lockService) {
            @Override
            protected void runTask() {
                // empty method
            }

            @Override
            public String getTaskName() {
                return "FailedTask";
            }

            @Override
            public AutomatedTaskName getAutomatedTaskName() {
                return null;
            }

            @Override
            public String getLastCronExpression() {
                return "*/7 * * * * *";
            }
        };
        UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
        ReflectionTestUtils.setField(failingAutomatedTask, "userAccountRepository", userAccountRepository);
        when(userAccountRepository.findFirstByEmailAddressIgnoreCase("system@darts.test")).thenReturn(Optional.of(userAccount));

        when(mockAutomatedTaskRepository.findByTaskName(failingAutomatedTask.getTaskName()))
            .thenThrow(RuntimeException.class);

        failingAutomatedTask.run();
        verify(logApi).taskFailed(any(UUID.class), eq("FailedTask"));
        assertEquals(AutomatedTaskStatus.FAILED, failingAutomatedTask.getAutomatedTaskStatus());
    }

    @Test
    void createsNewTaskOnReloadWhenNonExists() {
        var automatedTask = new AbstractLockableAutomatedTask<>(
            mockAutomatedTaskRepository,
            mockAutomatedTaskConfigurationProperties,
            logApi,
            lockService) {
            @Override
            protected void runTask() {
                // empty method
            }

            @Override
            protected void handleException(Exception exception) {
                // empty method
            }

            @Override
            public AutomatedTaskName getAutomatedTaskName() {
                return AutomatedTaskName.DAILY_LIST_HOUSEKEEPING_TASK_NAME;
            }
        };
        autoloadingAutomatedTasks.add(automatedTask);
        when(scheduledTaskHolder.getScheduledTasks()).thenReturn(new HashSet<>());

        automatedTaskService.reloadTaskByName("DailyListHousekeeping");

        verify(taskScheduler).schedule(any(AbstractLockableAutomatedTask.class), any(Trigger.class));
    }


    @Test
    void reloadNonExistingTask() {
        assertThrows(DartsApiException.class, () -> automatedTaskService.reloadTaskByName("blah"));
    }

    @Test
    void cancelAutomatedTask() {
        ScheduledTask scheduledTask = mock(ScheduledTask.class);
        Set<ScheduledTask> scheduledTaskList = new HashSet<>();
        scheduledTaskList.add(scheduledTask);

        AutomatedTaskTrigger task = getTriggerTask();
        when(scheduledTaskHolder.getScheduledTasks()).thenReturn(scheduledTaskList);
        when(scheduledTask.getTask()).thenReturn(task);

        assertTrue(automatedTaskService.cancelAutomatedTask("ProcessDailyList", true));

    }

    private AutomatedTaskTrigger getTriggerTask() {
        AbstractLockableAutomatedTask automatedTask = new AbstractLockableAutomatedTask<>(
            mockAutomatedTaskRepository,
            mockAutomatedTaskConfigurationProperties,
            logApi,
            lockService) {
            @Override
            protected void runTask() {
                // empty method
            }

            @Override
            protected void handleException(Exception exception) {
                // empty method
            }

            @Override
            public String getTaskName() {
                return "ProcessDailyList";
            }

            @Override
            public AutomatedTaskName getAutomatedTaskName() {
                return AutomatedTaskName.PROCESS_DAILY_LIST_TASK_NAME;
            }
        };
        Trigger trigger = triggerContext -> null;
        return new AutomatedTaskTrigger(automatedTask, trigger);
    }

    @Test
    void cancelNonExistingAutomatedTask() {
        assertFalse(automatedTaskService.cancelAutomatedTask("Test", true));
    }

    @Test
    void getNonExistingAutomatedTaskThrowsException() {
        assertThrows(DartsApiException.class, () ->
            automatedTaskService.getAutomatedTaskCronExpression("Test"));
    }

    @Test
    void updateNonExistingAutomatedTaskCronExpressionThrowsException() {

        assertThrows(DartsApiException.class, () ->
            automatedTaskService.updateAutomatedTaskCronExpression("Test", "*/8 * * * * *"));
    }

    @Test
    void updateNonExistingAbstractLockableAutomatedTaskCronExpressionThrowsException() {

        AbstractLockableAutomatedTask automatedTask = new AbstractLockableAutomatedTask<>(
            mockAutomatedTaskRepository,
            mockAutomatedTaskConfigurationProperties,
            logApi,
            lockService) {
            @Override
            public String getTaskName() {
                return "Test";
            }

            @Override
            public AutomatedTaskName getAutomatedTaskName() {
                return null;
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


    @Test
    void positiveGetArmAutomatedTaskEntity() {
        AutomatedTaskEntity automatedTask = mock(AutomatedTaskEntity.class);
        ArmAutomatedTaskEntity armAutomatedTaskEntity = mock(ArmAutomatedTaskEntity.class);
        when(automatedTask.getArmAutomatedTaskEntity()).thenReturn(armAutomatedTaskEntity);
        when(mockAutomatedTaskRepository.findByTaskName("Test")).thenReturn(Optional.of(automatedTask));

        AutomatedTaskName automatedTaskName = mock(AutomatedTaskName.class);
        when(automatedTaskName.getTaskName()).thenReturn("Test");

        ArmAutomatedTaskEntity actualArmAutomatedTaskEntity =
            automatedTaskService.getArmAutomatedTaskEntity(automatedTaskName);

        assertEquals(armAutomatedTaskEntity, actualArmAutomatedTaskEntity);

        verify(mockAutomatedTaskRepository).findByTaskName("Test");
        verify(automatedTask).getArmAutomatedTaskEntity();
    }

    @Test
    void negativeGetArmAutomatedTaskEntityTaskNotFound() {
        when(mockAutomatedTaskRepository.findByTaskName("Test")).thenReturn(Optional.empty());

        AutomatedTaskName automatedTaskName = mock(AutomatedTaskName.class);
        when(automatedTaskName.getTaskName()).thenReturn("Test");

        DartsApiException exception = assertThrows(DartsApiException.class, () ->
            automatedTaskService.getArmAutomatedTaskEntity(automatedTaskName));
        assertEquals(AutomatedTaskSetupError.FAILED_TO_FIND_AUTOMATED_TASK, exception.getError());

        verify(mockAutomatedTaskRepository).findByTaskName("Test");
    }

    @Test
    void negativeGetArmAutomatedTaskEntityNotArmTask() {
        AutomatedTaskEntity automatedTask = mock(AutomatedTaskEntity.class);
        when(automatedTask.getArmAutomatedTaskEntity()).thenReturn(null);
        when(mockAutomatedTaskRepository.findByTaskName("Test")).thenReturn(Optional.of(automatedTask));

        AutomatedTaskName automatedTaskName = mock(AutomatedTaskName.class);
        when(automatedTaskName.getTaskName()).thenReturn("Test");

        DartsApiException exception = assertThrows(DartsApiException.class, () ->
            automatedTaskService.getArmAutomatedTaskEntity(automatedTaskName));
        assertEquals(AutomatedTaskSetupError.FAILED_TO_FIND_AUTOMATED_TASK, exception.getError());

        verify(mockAutomatedTaskRepository).findByTaskName("Test");
        verify(automatedTask).getArmAutomatedTaskEntity();

    }

    private AutomatedTaskEntity createAutomatedTaskEntity(AutomatedTask automatedTask, String cronExpression) {
        AutomatedTaskEntity automatedTaskEntity1 = new AutomatedTaskEntity();
        automatedTaskEntity1.setId(1);
        automatedTaskEntity1.setTaskName(automatedTask.getTaskName());
        automatedTaskEntity1.setCronExpression(cronExpression);
        automatedTaskEntity1.setTaskDescription("Test Automated Task");
        return automatedTaskEntity1;
    }


    private AutomatedTask createAutomatedTask(String taskName) {
        return new AutomatedTask() {
            private String lastCronExpression = "*/10 * * * * *";

            @Override
            public String getTaskName() {
                return taskName;
            }

            @Override
            public AutomatedTaskStatus getAutomatedTaskStatus() {
                return AutomatedTaskStatus.NOT_STARTED;
            }

            @Override
            public String getLastCronExpression() {
                return lastCronExpression;
            }

            @Override
            public void setLastCronExpression(String cronExpression) {
                this.lastCronExpression = cronExpression;
            }

            @Override
            public void run(boolean isManualRun) {
                log.debug("Running test automated task");
            }

        };
    }

}
