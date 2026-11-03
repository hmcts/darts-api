package uk.gov.hmcts.darts.cases.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Set;

@Component
public class CaseLinkRowValidator {

    private static final int COLUMN_COUNT = 3;
    private static final int CASE_NUMBER_LENGTH = 32;
    private static final int COURTHOUSE_LENGTH = 255;

    public void validate(CaseLinkImportResultEntity row, Set<Mapping> seen) {
        if (row.getColumnCount() != COLUMN_COUNT) {
            throw new DartsApiException(CaseApiError.INVALID_COLUMN_COUNT);
        }
        validateValue(row.getCase1CaseNumber(), CASE_NUMBER_LENGTH,
                      CaseApiError.CASE_1_CASE_NUMBER_REQUIRED, CaseApiError.CASE_1_CASE_NUMBER_TOO_LONG);
        validateValue(row.getCase1Courthouse(), COURTHOUSE_LENGTH,
                      CaseApiError.CASE_1_COURTHOUSE_REQUIRED, CaseApiError.CASE_1_COURTHOUSE_TOO_LONG);
        validateValue(row.getCase2CaseNumber(), CASE_NUMBER_LENGTH,
                      CaseApiError.CASE_2_CASE_NUMBER_REQUIRED, CaseApiError.CASE_2_CASE_NUMBER_TOO_LONG);
        String case1 = row.getCase1CaseNumber().trim();
        String case2 = row.getCase2CaseNumber().trim();
        String courthouse = StringUtils.toRootUpperCase(row.getCase1Courthouse().trim());
        Mapping mapping = case1.compareTo(case2) <= 0 ? new Mapping(courthouse, case1, case2) : new Mapping(courthouse, case2, case1);
        if (!seen.add(mapping)) {
            throw new DartsApiException(CaseApiError.DUPLICATE_MAPPING);
        }
    }

    private void validateValue(String value, int maxLength, CaseApiError requiredError, CaseApiError lengthError) {
        if (StringUtils.isBlank(value)) {
            throw new DartsApiException(requiredError);
        }
        if (value.trim().length() > maxLength) {
            throw new DartsApiException(lengthError);
        }
    }

    public record Mapping(String courthouse, String firstCase, String secondCase) {
    }
}
