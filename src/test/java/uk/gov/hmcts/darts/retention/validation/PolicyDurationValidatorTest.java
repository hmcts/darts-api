package uk.gov.hmcts.darts.retention.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyDurationValidatorTest {

    private PolicyDurationValidator policyDurationValidator;

    @BeforeEach
    void setUp() {
        policyDurationValidator = new PolicyDurationValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"00Y00M01D", "0Y0M1D", "0Y1M0D", "1Y0M0D", "1Y1M0D", "1Y1M1D"})
    void validateShouldCompleteWithoutExceptionForValidValues(String durationString) {
        assertDoesNotThrow(() -> policyDurationValidator.validate(durationString));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00Y00M00D", "0Y0M0D"})
    void validateShouldThrowExceptionForInvalidValues(String durationString) {
        var exception = assertThrows(DartsApiException.class, () -> policyDurationValidator.validate(durationString));
        assertEquals("Duration too short", exception.getMessage());
    }

}