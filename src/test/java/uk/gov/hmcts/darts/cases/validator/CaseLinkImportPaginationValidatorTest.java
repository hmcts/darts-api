package uk.gov.hmcts.darts.cases.validator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.darts.cases.config.CaseLinkImportConfiguration;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailure;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseLinkImportPaginationValidatorTest {

    private final CaseLinkImportConfiguration configuration = new CaseLinkImportConfiguration();
    private final CaseLinkImportPaginationValidator validator = new CaseLinkImportPaginationValidator(configuration);

    @ParameterizedTest
    @ValueSource(ints = {1, 25, 100})
    void validate_shouldAcceptPageSize_whenWithinConfiguredMaximum(int pageSize) {
        PaginationDto<CaseLinkImportFailure> paginationDto = new PaginationDto<>(1, pageSize, List.of(), List.of());

        assertThatCode(() -> validator.validate(paginationDto)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {101, 1000, Integer.MAX_VALUE})
    void validate_shouldRejectPageSize_whenAboveConfiguredMaximum(int pageSize) {
        PaginationDto<CaseLinkImportFailure> paginationDto = new PaginationDto<>(1, pageSize, List.of(), List.of());

        assertThatThrownBy(() -> validator.validate(paginationDto))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> {
                assertThat(exception.getError()).isEqualTo(CaseApiError.INVALID_REQUEST);
                assertThat(exception.getMessage()).contains("100");
            });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 1000, Integer.MAX_VALUE})
    void validate_shouldAllowAnyPageNumber_whenPageSizeIsWithinConfiguredMaximum(int pageNumber) {
        PaginationDto<CaseLinkImportFailure> paginationDto = new PaginationDto<>(pageNumber, 100, List.of(), List.of());

        assertThatCode(() -> validator.validate(paginationDto)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {26, 100})
    void validate_shouldUseConfiguredMaximum_whenPageSizeIsOverridden(int pageSize) {
        configuration.setFailurePageSize(25);
        PaginationDto<CaseLinkImportFailure> paginationDto = new PaginationDto<>(1, pageSize, List.of(), List.of());

        assertThatThrownBy(() -> validator.validate(paginationDto))
            .isInstanceOfSatisfying(DartsApiException.class, exception -> assertThat(exception.getError()).isEqualTo(CaseApiError.INVALID_REQUEST));
    }
}
