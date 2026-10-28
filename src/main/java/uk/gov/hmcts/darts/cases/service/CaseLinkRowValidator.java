package uk.gov.hmcts.darts.cases.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.cases.exception.CaseLinkRowException;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;

import java.util.Set;

@Component
public class CaseLinkRowValidator {

    private static final int COLUMN_COUNT = 3;
    private static final int CASE_NUMBER_LENGTH = 32;
    private static final int COURTHOUSE_LENGTH = 255;

    public void validate(CaseLinkImportResultEntity row, Set<Mapping> seen) {
        if (row.getColumnCount() != COLUMN_COUNT) {
            throw new CaseLinkRowException("INVALID_COLUMN_COUNT", "Each row must contain exactly three columns");
        }
        validateValue(row.getCase1CaseNumber(), CASE_NUMBER_LENGTH, "CASE_1_CASE_NUMBER");
        validateValue(row.getCase1Courthouse(), COURTHOUSE_LENGTH, "CASE_1_COURTHOUSE");
        validateValue(row.getCase2CaseNumber(), CASE_NUMBER_LENGTH, "CASE_2_CASE_NUMBER");
        String case1 = row.getCase1CaseNumber().trim();
        String case2 = row.getCase2CaseNumber().trim();
        String courthouse = StringUtils.toRootUpperCase(row.getCase1Courthouse().trim());
        Mapping mapping = case1.compareTo(case2) <= 0 ? new Mapping(courthouse, case1, case2) : new Mapping(courthouse, case2, case1);
        if (!seen.add(mapping)) {
            throw new CaseLinkRowException("DUPLICATE_MAPPING", "The mapping duplicates an earlier row in either case order");
        }
    }

    private void validateValue(String value, int maxLength, String field) {
        if (StringUtils.isBlank(value)) {
            throw new CaseLinkRowException(field + "_REQUIRED", field + " is required");
        }
        if (value.trim().length() > maxLength) {
            throw new CaseLinkRowException(field + "_TOO_LONG", field + " exceeds the maximum length of " + maxLength);
        }
    }

    public record Mapping(String courthouse, String firstCase, String secondCase) {
    }
}
