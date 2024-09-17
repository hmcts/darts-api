package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourtCaseEntityTest {

    @Test
    void negativeValidateIsExpiredIsExpired() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setDataAnonymised(true);
        DartsApiException exception = assertThrows(DartsApiException.class, courtCase::validateIsExpired);
        assertThat(exception.getError()).isEqualTo(CaseApiError.CASE_EXPIRED);
        assertThat(exception.getMessage()).isEqualTo("Case has expired.");
    }

    @Test
    void positiveValidateIsExpiredIsNotExpired() {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setDataAnonymised(false);
        assertDoesNotThrow(courtCase::validateIsExpired);
    }
}
