package uk.gov.hmcts.darts.cases.config;

import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.unit.DataSize;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CaseLinkImportConfigurationTest {

    private final CaseLinkImportConfiguration configuration = new CaseLinkImportConfiguration();

    @BeforeEach
    void setUp() {
        configuration.setMaxFileSize(DataSize.ofKilobytes(1));
        configuration.setMaxRows(1);
        configuration.setMaxRecordCharacters(1);
        configuration.setFailurePageSize(1);
        configuration.setAuditBatchSize(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void validate_shouldRejectConfiguration_whenMaximumRowsIsNotPositive(int maxRows) {
        configuration.setMaxRows(maxRows);

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(configuration)).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("maxRows"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void validate_shouldRejectConfiguration_whenFailurePageSizeIsNotPositive(int failurePageSize) {
        configuration.setFailurePageSize(failurePageSize);

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(configuration)).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("failurePageSize"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void validate_shouldRejectConfiguration_whenMaximumRecordCharactersIsNotPositive(int maxRecordCharacters) {
        configuration.setMaxRecordCharacters(maxRecordCharacters);

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(configuration)).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("maxRecordCharacters"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void validate_shouldRejectConfiguration_whenAuditBatchSizeIsNotPositive(int auditBatchSize) {
        configuration.setAuditBatchSize(auditBatchSize);

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(configuration)).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath().toString()).isEqualTo("auditBatchSize"));
        }
    }

    @Test
    void validate_shouldRejectMissingConfiguration_whenPropertiesHaveNotBeenBound() {
        CaseLinkImportConfiguration unboundConfiguration = new CaseLinkImportConfiguration();

        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(unboundConfiguration))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("maxFileSize", "maxRows", "maxRecordCharacters", "failurePageSize", "auditBatchSize");
        }
    }

    @Test
    void bind_shouldLoadDefaultsFromApplicationYaml_whenEnvironmentOverridesAreAbsent() throws IOException {
        StandardEnvironment environment = applicationEnvironment();

        CaseLinkImportConfiguration boundConfiguration = Binder.get(environment)
            .bind("darts.cases.link-import", Bindable.of(CaseLinkImportConfiguration.class)).get();

        assertThat(boundConfiguration.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(25));
        assertThat(boundConfiguration.getMaxRows()).isEqualTo(100_000);
        assertThat(boundConfiguration.getMaxRecordCharacters()).isEqualTo(65_536);
        assertThat(boundConfiguration.getFailurePageSize()).isEqualTo(100);
        assertThat(boundConfiguration.getAuditBatchSize()).isEqualTo(1000);
    }

    @Test
    void bind_shouldApplyEnvironmentOverrides_whenYamlPlaceholdersHaveOverrides() throws IOException {
        StandardEnvironment environment = applicationEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("case-link-overrides", Map.of(
            "CASE_LINK_IMPORT_MAX_FILE_SIZE", "2MB",
            "CASE_LINK_IMPORT_MAX_ROWS", "10",
            "CASE_LINK_IMPORT_MAX_RECORD_CHARACTERS", "2048",
            "CASE_LINK_IMPORT_FAILURE_PAGE_SIZE", "20",
            "CASE_LINK_IMPORT_AUDIT_BATCH_SIZE", "5"
        )));

        CaseLinkImportConfiguration boundConfiguration = Binder.get(environment)
            .bind("darts.cases.link-import", Bindable.of(CaseLinkImportConfiguration.class)).get();

        assertThat(boundConfiguration.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(2));
        assertThat(boundConfiguration.getMaxRows()).isEqualTo(10);
        assertThat(boundConfiguration.getMaxRecordCharacters()).isEqualTo(2048);
        assertThat(boundConfiguration.getFailurePageSize()).isEqualTo(20);
        assertThat(boundConfiguration.getAuditBatchSize()).isEqualTo(5);
    }

    private StandardEnvironment applicationEnvironment() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        for (var propertySource : new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml"))) {
            environment.getPropertySources().addLast(propertySource);
        }
        return environment;
    }
}
