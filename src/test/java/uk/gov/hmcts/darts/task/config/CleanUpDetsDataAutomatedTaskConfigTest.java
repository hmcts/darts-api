package uk.gov.hmcts.darts.task.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CleanUpDetsDataAutomatedTaskConfigTest {

    private static final String CLEANUP_TIMEOUT_PROPERTY = "cleanupTimeout";

    @ParameterizedTest
    @CsvSource(value = {
        "vacuumTimeout, null, false", "vacuumTimeout, PT0S, false", "vacuumTimeout, -PT1S, false", "vacuumTimeout, PT0.999S, false",
        "vacuumTimeout, PT1S, true", "vacuumTimeout, PT1H55M, true",
        "cleanupTimeout, null, false", "cleanupTimeout, PT0S, false", "cleanupTimeout, -PT1S, false", "cleanupTimeout, PT0.999S, false",
        "cleanupTimeout, PT1S, true", "cleanupTimeout, PT1H, true"
    }, nullValues = "null")
    void timeouts_shouldRequireAtLeastOneSecond_whenConfigurationIsValidated(String property, String timeout, boolean valid) {
        CleanUpDetsDataAutomatedTaskConfig config = new CleanUpDetsDataAutomatedTaskConfig();
        config.setSystemUserEmail("systemCleanUpDetsDataAutomatedTask@hmcts.net");
        config.setCleanupTimeout(Duration.ofHours(1));
        config.setVacuumTimeout(Duration.ofMinutes(115));
        Duration duration = timeout == null ? null : Duration.parse(timeout);
        if (CLEANUP_TIMEOUT_PROPERTY.equals(property)) {
            config.setCleanupTimeout(duration);
        } else {
            config.setVacuumTimeout(duration);
        }

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<CleanUpDetsDataAutomatedTaskConfig>> violations = validatorFactory.getValidator().validate(config);

            assertThat(violations).hasSize(valid ? 0 : 1);
            assertThat(violations).allSatisfy(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo(property));
        }
    }
}
