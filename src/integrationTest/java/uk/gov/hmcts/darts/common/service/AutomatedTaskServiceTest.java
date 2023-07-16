package uk.gov.hmcts.darts.common.service;


import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.test.context.ActiveProfiles;

import uk.gov.hmcts.darts.common.task.AutomatedTaskOne;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@Slf4j
public class AutomatedTaskServiceTest extends IntegrationBase {
    @Autowired
    private AutomatedTaskService automatedTaskService;

    @Autowired
    private ScheduledTaskHolder taskHolder;

    @Autowired
    private TaskScheduler taskScheduler;

    @Test
    void configureTasks() throws InterruptedException {
        Thread.sleep(35_000);

        Set<ScheduledTask> scheduledTasks = taskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        log.info("----- About to cancel task 1");
        boolean taskCancelled = automatedTaskService.cancelAutomatedTask(AutomatedTaskOne.TASKNAME);
        assertTrue(taskCancelled);

        Thread.sleep(31_000);
        log.info("----- Tasks after sleep");
        scheduledTasks = taskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);
        automatedTaskService.updateAutomatedTaskCronExpression(AutomatedTaskOne.TASKNAME, getRandomCronExpression());
        log.info("----- About to add task 1");
        automatedTaskService.loadAutomatedTaskOne(taskScheduler);
        Thread.sleep(25_000);
        scheduledTasks = taskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);
    }

    private String getRandomCronExpression() {
        List<String> cronExpressions = Arrays.asList(
            "*/20 * * * * *",
            "*/6 * * * * *",
            "*/8 * * * * *",
            "*/5 * * * * *",
            "*/4 * * * * *",
            "*/7 * * * * *"
        );
        Random random = new Random();
        int index = random.nextInt(5);
        String cronExpression = cronExpressions.get(index);

        log.info("Changing cronExpression: {}", cronExpression);
        return cronExpression;
    }

    private static void displayTasks(Set<ScheduledTask> scheduledTasks) {
        log.info("Number of scheduled tasks " + scheduledTasks.size());
        scheduledTasks.forEach(
            scheduledTask -> {
                Task task = scheduledTask.getTask();
                if (task instanceof CronTask) {
                    CronTask cronTask = (CronTask) task;
                    log.info("Cron expression: {}", cronTask.getExpression());
                } else if (task instanceof TriggerTask) {
                    TriggerTask triggerTask = (TriggerTask) task;
                    log.info("TriggerTask runnable: {}", triggerTask.getRunnable());
                } else if (task instanceof FixedRateTask) {
                    FixedRateTask fixedRateTask = (FixedRateTask) task;
                    log.info("FixedRateTask initial delay duration: {}", fixedRateTask.getInitialDelayDuration());
                } else if (task instanceof FixedDelayTask) {
                    FixedDelayTask fixedDelayTask = (FixedDelayTask) task;
                    log.info("FixedDelayTask initial delay duration: {}", fixedDelayTask.getInitialDelayDuration());
                } else {
                    log.info("Unknown task type: " + task);
                }
            }
        );
    }
}
