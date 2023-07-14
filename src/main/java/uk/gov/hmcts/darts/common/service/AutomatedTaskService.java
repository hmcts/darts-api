package uk.gov.hmcts.darts.common.service;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;

public interface AutomatedTaskService {
    void loadAutomatedTasks(ScheduledTaskRegistrar taskRegistrar);

    AutomatedTaskEntity getAutomatedTask(String taskName);

    boolean cancelTask(String taskName);

    void loadAutomatedTaskOne(TaskScheduler taskScheduler);

}
