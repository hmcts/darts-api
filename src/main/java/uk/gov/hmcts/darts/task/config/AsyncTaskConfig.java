package uk.gov.hmcts.darts.task.config;

import java.time.Duration;

public interface AsyncTaskConfig {
    public int getThreads();

    public Duration getAsyncTimeout();
}
