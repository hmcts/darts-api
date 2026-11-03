package uk.gov.hmcts.darts.cases.config;

import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CaseLinkImportConfigurationTest {

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void validate_shouldRejectConfiguration_whenMaximumRowsIsNotPositive(int maxRows) {
        CaseLinkImportConfiguration configuration = new CaseLinkImportConfiguration();
        configuration.setMaxRows(maxRows);

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(configuration)).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("maxRows"));
        }
    }
}
