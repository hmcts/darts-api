package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.darts.cases.exception.CaseLinkRowException;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseLinkRowValidatorTest {

    private final CaseLinkRowValidator validator = new CaseLinkRowValidator();

    @Test
    void given_normalisedOrReversedDuplicate_when_validating_then_rejectDuplicate() {
        var seen = new HashSet<CaseLinkRowValidator.Mapping>();
        validator.validate(row("X1", "YORK", "CP1"), seen);
        assertThatThrownBy(() -> validator.validate(row(" X1 ", " york ", " CP1 "), seen))
            .isInstanceOf(CaseLinkRowException.class).hasMessageContaining("duplicates");
        assertThatThrownBy(() -> validator.validate(row("CP1", "YORK", "X1"), seen))
            .isInstanceOf(CaseLinkRowException.class).hasMessageContaining("duplicates");
    }

    @Test
    void given_sharedCase2OrDifferentCourthouseOrCase_when_validating_then_allowDistinctMappings() {
        var seen = new HashSet<CaseLinkRowValidator.Mapping>();
        validator.validate(row("X1", "YORK", "CP1"), seen);
        assertThatCode(() -> {
            validator.validate(row("X2", "YORK", "CP1"), seen);
            validator.validate(row("X1", "LEEDS", "CP1"), seen);
            validator.validate(row("x1", "YORK", "CP1"), seen);
        }).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"'',YORK,CP1,CASE_1_CASE_NUMBER_REQUIRED", "X1,'',CP1,CASE_1_COURTHOUSE_REQUIRED", "X1,YORK,'',CASE_2_CASE_NUMBER_REQUIRED"})
    void given_missingValue_when_validating_then_returnGenericCaseCode(String first, String courthouse, String second, String code) {
        assertThatThrownBy(() -> validator.validate(row(first, courthouse, second), new HashSet<>()))
            .isInstanceOfSatisfying(CaseLinkRowException.class, exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                .isEqualTo(code));
    }

    @Test
    void given_fieldLengthsAtBoundary_when_validating_then_accept() {
        assertThatCode(() -> validator.validate(row("A".repeat(32), "B".repeat(255), "C".repeat(32)), new HashSet<>()))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"33,1,1,CASE_1_CASE_NUMBER_TOO_LONG", "1,256,1,CASE_1_COURTHOUSE_TOO_LONG", "1,1,33,CASE_2_CASE_NUMBER_TOO_LONG"})
    void given_excessiveFieldLength_when_validating_then_fail(int first, int courthouse, int second, String code) {
        assertThatThrownBy(() -> validator.validate(row("A".repeat(first), "B".repeat(courthouse), "C".repeat(second)), new HashSet<>()))
            .isInstanceOfSatisfying(CaseLinkRowException.class, exception -> org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
                .isEqualTo(code));
    }

    @Test
    void given_wrongColumnCount_when_validating_then_fail() {
        var row = row("X1", "YORK", "CP1");
        row.setColumnCount(4);
        assertThatThrownBy(() -> validator.validate(row, new HashSet<>())).hasMessageContaining("exactly three");
    }

    private CaseLinkImportResultEntity row(String first, String courthouse, String second) {
        var row = new CaseLinkImportResultEntity();
        row.setCase1CaseNumber(first);
        row.setCase1Courthouse(courthouse);
        row.setCase2CaseNumber(second);
        row.setColumnCount(3);
        return row;
    }
}
