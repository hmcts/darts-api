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

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void validate_shouldRejectConfiguration_whenFailurePageSizeIsNotPositive(int failurePageSize) {
        CaseLinkImportConfiguration configuration = new CaseLinkImportConfiguration();
        configuration.setFailurePageSize(failurePageSize);

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(configuration)).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("failurePageSize"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void validate_shouldRejectConfiguration_whenMaximumRecordCharactersIsNotPositive(int maxRecordCharacters) {
        CaseLinkImportConfiguration configuration = new CaseLinkImportConfiguration();
        configuration.setMaxRecordCharacters(maxRecordCharacters);

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(configuration)).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("maxRecordCharacters"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void validate_shouldRejectConfiguration_whenAuditBatchSizeIsNotPositive(int auditBatchSize) {
        CaseLinkImportConfiguration configuration = new CaseLinkImportConfiguration();
        configuration.setAuditBatchSize(auditBatchSize);

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(configuration)).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("auditBatchSize"));
        }
    }
}
