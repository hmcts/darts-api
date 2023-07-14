package uk.gov.hmcts.darts.common.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import uk.gov.hmcts.darts.common.service.AutomatedTaskService;
import uk.gov.hmcts.darts.common.task.TestAutomatedTaskOne;

import java.util.Set;

@SpringBootTest
@Slf4j
@ContextConfiguration(classes = {AutomatedTaskConfig.class}, loader = AnnotationConfigContextLoader.class)
class AutomatedTaskConfigTest {

    @Autowired
    private AutomatedTaskService automatedTaskService;

    @Autowired
    private ScheduledTaskHolder taskHolder;

    @Autowired
    private TaskScheduler taskScheduler;

    @Test
    void configureTasks() throws InterruptedException {
        Thread.sleep(35000);

        Set<ScheduledTask> scheduledTasks = taskHolder.getScheduledTasks();
        displayTasks(scheduledTasks);

        log.info("----- About to cancel task 1");
        automatedTaskService.cancelTaskTrigger(TestAutomatedTaskOne.TASKNAME);
        log.info("----- Current tasks");
        displayTasks(scheduledTasks);

        Thread.sleep(21000);
        log.info("----- Tasks after sleep");
        displayTasks(scheduledTasks);
        log.info("----- About to add task 1");
        automatedTaskService.loadAutomatedTaskOne(taskScheduler);
        Thread.sleep(31000);
        displayTasks(scheduledTasks);
    }


    private static void displayTasks(Set<ScheduledTask> scheduledTasks) {
        log.info("Checking scheduled tasks " + scheduledTasks.size());

        /*List<Task> result = new ArrayList<Task>();
        result.addAll(this.scheduledTaskRegistrar.getTriggerTaskList());
        result.addAll(this.scheduledTaskRegistrar.getCronTaskList());
        result.addAll(this.scheduledTaskRegistrar.getFixedRateTaskList());
        result.addAll(this.scheduledTaskRegistrar.getFixedDelayTaskList());*/

        scheduledTasks.forEach(
            scheduledTask -> {
                Task task = scheduledTask.getTask();
                if (task instanceof CronTask) {
                    CronTask cronTask = (CronTask) task;
                    log.info("Cron expression: " + cronTask.getExpression());
                } else if (task instanceof TriggerTask) {
                    TriggerTask triggerTask = (TriggerTask) task;
                    log.info("TriggerTask runnable: " + triggerTask.getRunnable());
                } else {
                    log.info("not cron: " + task);
                }
            }
        );
    }
}
