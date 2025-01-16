package uk.gov.hmcts.darts.task.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import uk.gov.hmcts.darts.task.service.AutomatedTaskService;

@Configuration
@EnableScheduling
@AllArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "darts", name = "automated-tasks-pod", havingValue = "true")
public class AutomatedTaskConfigurationStrategy implements SchedulingConfigurer {

    private final AutomatedTaskService automatedTaskService;

    private final TaskScheduler taskScheduler;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler);
        log.debug("Configuring automated tasks");
        automatedTaskService.configureAndLoadAutomatedTasks(taskRegistrar);
        log.debug("Automated tasks configured and loaded");
    }

}
