package uk.gov.hmcts.darts.common.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.service.AutomatedTaskService;
import uk.gov.hmcts.darts.common.task.AutomatedTask;
import uk.gov.hmcts.darts.common.task.TestAutomatedTaskOne;
import uk.gov.hmcts.darts.common.task.TestAutomatedTaskTwo;

import java.time.Instant;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
public class AutomatedTaskServiceImpl implements AutomatedTaskService {

    private AutomatedTaskRepository automatedTaskRepository;

    @Autowired
    private ScheduledTaskHolder taskHolder;

    private Map<String, Trigger> taskTriggers = new ConcurrentHashMap<>();

    @Override
    public void loadAutomatedTasks(ScheduledTaskRegistrar taskRegistrar) {
        loadAutomatedTaskOne(taskRegistrar);
        loadAutomatedTaskTwo(taskRegistrar);
    }

    @Override
    public AutomatedTaskEntity getAutomatedTask(String taskName) {
        return automatedTaskRepository.findByTaskName(taskName);
    }

    public String getAutomatedTaskCronExpression(AutomatedTask automatedTask) {
        String cronExpression = automatedTask.getDefaultCronExpression();
        AutomatedTaskEntity automatedTaskEntity = getAutomatedTask(automatedTask.getTaskName());
        if (automatedTaskEntity != null) {
            cronExpression = automatedTaskEntity.getCronExpression();
        }
        return cronExpression;
    }

    @Override
    public void loadAutomatedTaskOne(TaskScheduler taskScheduler) {
        TestAutomatedTaskOne taskOne = new TestAutomatedTaskOne();
        Trigger trigger = createAutomatedTaskTrigger(taskOne);
        taskScheduler.schedule(taskOne, trigger);
    }

    private void loadAutomatedTaskOne(ScheduledTaskRegistrar taskRegistrar) {
        TestAutomatedTaskOne taskOne = new TestAutomatedTaskOne();
        Trigger trigger = createAutomatedTaskTrigger(taskOne);
        taskRegistrar.addTriggerTask(taskOne, trigger);
    }

    public void loadAutomatedTaskTwo(TaskScheduler taskScheduler) {
        TestAutomatedTaskTwo taskTwo = new TestAutomatedTaskTwo();
        Trigger trigger = createAutomatedTaskTrigger(taskTwo);
        taskScheduler.schedule(taskTwo, trigger);
    }

    private void loadAutomatedTaskTwo(ScheduledTaskRegistrar taskRegistrar) {
        TestAutomatedTaskTwo taskTwo = new TestAutomatedTaskTwo();
        Trigger trigger = createAutomatedTaskTrigger(taskTwo);
        taskRegistrar.addTriggerTask(taskTwo, trigger);
    }

    public Trigger createAutomatedTaskTrigger(AutomatedTask automatedTask) {
        Trigger trigger = createCronTrigger(automatedTask);
        taskTriggers.put(automatedTask.getTaskName(), trigger);
        return trigger;
    }

    @Override
    public boolean cancelTaskTrigger(String taskName) {
        boolean result = false;
        if (taskTriggers.containsKey(taskName)) {
            Set<ScheduledTask> scheduledTasks = taskHolder.getScheduledTasks();
            for (ScheduledTask  scheduledTask: scheduledTasks) {
                Task task = scheduledTask.getTask();
                if (task instanceof TriggerTask) {
                    TriggerTask triggerTask = (TriggerTask) task;

                    if (triggerTask.getRunnable() instanceof AutomatedTask) {
                        AutomatedTask automatedTask = (AutomatedTask) triggerTask.getRunnable();
                        if (automatedTask.getTaskName().equals(taskName)) {
                            log.info("About to cancel task: " + taskName);
                            scheduledTask.cancel(false);
                            result = true;
                            taskTriggers.remove(taskName);
                            break;
                        }
                    }
                }
            }
        } else {
            //TODO Throw some kind of exception
            log.error("Unable to cancel task: {}", taskName);
        }

        return result;
    }

    private Trigger createCronTrigger(AutomatedTask automatedTask) {
        Trigger trigger = new Trigger() {
            @Override
            public Instant nextExecution(TriggerContext triggerContext) {
                String cronExpression = getAutomatedTaskCronExpression(automatedTask);
                CronTrigger crontrigger = new CronTrigger(cronExpression);
                return crontrigger.nextExecution(triggerContext);
            }
        };
        return trigger;
    }

}
