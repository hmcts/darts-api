package uk.gov.hmcts.darts.common.task;

import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;

@Slf4j
public class TestAutomatedTaskTwo implements AutomatedTask {
    private static final String TASKNAME = "TestAutomatedTaskTwo";

    public static final String DEFAULT_CRON_EXPRESSION = "*/20 * * * * *";

    public String getTaskName() {
        return TASKNAME;
    }

    @Override
    public String getDefaultCronExpression() {
        return DEFAULT_CRON_EXPRESSION;
    }

    @Override
    public void run() {
        log.info("Task: {} running at: {}", getTaskName(), Calendar.getInstance().getTime());
    }
}
