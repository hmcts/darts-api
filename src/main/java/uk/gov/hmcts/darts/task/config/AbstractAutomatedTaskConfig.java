package uk.gov.hmcts.darts.task.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
public class AbstractAutomatedTaskConfig {
    @NotBlank
    private String systemUserEmail;
    @NotNull
    private Duration lockAtMostFor = Duration.ofMinutes(300);
    @NotNull
    private Duration lockAtLeastFor = Duration.ofMinutes(1);
}
