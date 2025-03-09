package uk.gov.hmcts.darts.task.config;

import java.time.Duration;

public interface AsyncTaskConfig {

    int getThreads();

    Duration getAsyncTimeout();

}
