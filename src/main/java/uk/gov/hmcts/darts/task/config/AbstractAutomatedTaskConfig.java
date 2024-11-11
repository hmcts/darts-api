package uk.gov.hmcts.darts.task.config;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;

@Getter
@Setter
public class AbstractAutomatedTaskConfig {

    private String systemUserEmail;
    private Duration lockAtMostFor = Duration.ofMinutes(300);
    private Duration lockAtLeastFor = Duration.ofMinutes(1);
}
