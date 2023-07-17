package uk.gov.hmcts.darts.common.task;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AutomatedTaskTwo implements AutomatedTask {
    public static final String TASKNAME = "AutomatedTaskTwo";

    public static final String DEFAULT_CRON_EXPRESSION = "*/20 * * * * *";

    @Override
    public String getTaskName() {
        return TASKNAME;
    }

    @Override
    public String getDefaultCronExpression() {
        return DEFAULT_CRON_EXPRESSION;
    }

    @Override
    public void run() {
        log.info("Task: {} running at: {}", getTaskName(), LocalDateTime.now());
    }
}
