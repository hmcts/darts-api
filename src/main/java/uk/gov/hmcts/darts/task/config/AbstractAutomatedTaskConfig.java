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

    private Lock lock;

    @Getter
    @Setter
    @Validated
    @SuppressWarnings("PMD.ShortClassName")
    public static class Lock {
        @NotNull
        private Duration atLeastFor = Duration.ofMinutes(300);
        @NotNull
        private Duration atMostFor = Duration.ofMinutes(1);
    }
}
