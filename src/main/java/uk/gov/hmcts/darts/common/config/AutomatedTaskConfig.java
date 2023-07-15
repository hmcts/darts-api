package uk.gov.hmcts.darts.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import uk.gov.hmcts.darts.common.service.AutomatedTaskService;

@Configuration
@EnableScheduling
public class AutomatedTaskConfig implements SchedulingConfigurer {

    public static final String THREAD_POOL_TASK_SCHEDULER_NAME = "ThreadPoolTaskScheduler";

    private static final Integer THREAD_POOL_SIZE = 5;

    @Autowired
    private AutomatedTaskService automatedTaskService;

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(THREAD_POOL_SIZE);
        threadPoolTaskScheduler.setThreadNamePrefix(THREAD_POOL_TASK_SCHEDULER_NAME);
        return threadPoolTaskScheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        automatedTaskService.loadAutomatedTasks(taskRegistrar);
    }
}
