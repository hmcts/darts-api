package uk.gov.hmcts.darts.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import uk.gov.hmcts.darts.common.service.AutomatedTaskService;
import uk.gov.hmcts.darts.common.task.AutomatedTask;
import uk.gov.hmcts.darts.common.task.TestAutomatedTaskOne;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@ComponentScan("uk.gov.hmcts.darts")
@EnableScheduling
public class AutomatedTaskConfig implements SchedulingConfigurer {

    public static final String THREAD_POOL_TASK_SCHEDULER_NAME = "ThreadPoolTaskScheduler";

    @Value("${darts.automated_tasks.thread_pool_size}")
    private int threadPoolSize;

    @Autowired
    private AutomatedTaskService automatedTaskService;

    @Bean
    public Executor taskExecutor() {
        System.out.println("Running Scheduler..." + Calendar.getInstance().getTime());
        return Executors.newSingleThreadScheduledExecutor();
    }

    public List<AutomatedTask> getAutomatedTasks() {
        return Arrays.asList(new AutomatedTask[]{
            new TestAutomatedTaskOne()
        });
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(threadPoolSize);
        threadPoolTaskScheduler.setThreadNamePrefix(THREAD_POOL_TASK_SCHEDULER_NAME);
        return threadPoolTaskScheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        automatedTaskService.loadAutomatedTasks(taskRegistrar);
    }
}
