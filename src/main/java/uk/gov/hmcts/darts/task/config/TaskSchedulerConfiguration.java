package uk.gov.hmcts.darts.task.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskSchedulerConfiguration {
    private static final String THREAD_POOL_TASK_SCHEDULER_NAME = "ThreadPoolTaskScheduler";

    @Value("${darts.automated.task.common-config.thread-pool-size:5}")
    private int threadPoolSize;

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(threadPoolSize);
        threadPoolTaskScheduler.setThreadNamePrefix(THREAD_POOL_TASK_SCHEDULER_NAME);
        return threadPoolTaskScheduler;
    }

}
