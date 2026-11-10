package uk.gov.hmcts.darts.cases.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.cases.config.CaseLinkImportConfiguration;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailure;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

@Component
@RequiredArgsConstructor
public class CaseLinkImportPaginationValidator implements Validator<PaginationDto<CaseLinkImportFailure>> {

    private final CaseLinkImportConfiguration configuration;

    @Override
    public void validate(PaginationDto<CaseLinkImportFailure> paginationDto) {
        if (paginationDto.getPageSize() > configuration.getFailurePageSize()) {
            throw new DartsApiException(CaseApiError.INVALID_REQUEST,
                                        "page_size must not exceed the configured failure page size of " + configuration.getFailurePageSize());
        }
    }
}
