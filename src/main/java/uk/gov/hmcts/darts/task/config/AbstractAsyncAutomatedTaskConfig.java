package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
public class AbstractAsyncAutomatedTaskConfig extends AbstractAutomatedTaskConfig
    implements AsyncTaskConfig {
    private int threads;
    private Duration asyncTimeout;
}
