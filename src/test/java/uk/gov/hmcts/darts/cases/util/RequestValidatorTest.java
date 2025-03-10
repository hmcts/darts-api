package uk.gov.hmcts.darts.cases.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestValidatorTest {

    @Test
    void okCaseNumberLongEnough() {
        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .caseNumber("gfdgfd")
            .build();
        RequestValidator.validate(request);
    }

    @Test
    void raiseNoCriteriaException() {
        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .build();

        var exception = assertThrows(
            DartsApiException.class,
            () -> RequestValidator.validate(request)
        );
        assertEquals(
            "No search criteria has been specified, please add at least 1 criteria to search for.",
            exception.getMessage()
        );
    }

    @Test
    void raiseNotEnoughCriteriaException() {
        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courthouse("swansea")
            .build();

        var exception = assertThrows(
            DartsApiException.class,
            () -> RequestValidator.validate(request)
        );
        assertEquals(
            "Search criteria is too broad, please add at least 1 more criteria to search for.",
            exception.getMessage()
        );
    }

    @Test
    void raiseNotEnoughCriteriaException2() {
        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courthouse("swansea")
            .courtroom("1")
            .caseNumber("1")
            .build();

        var exception = assertThrows(
            DartsApiException.class,
            () -> RequestValidator.validate(request)
        );
        assertEquals(
            "Search criteria is too broad, please add at least 1 more criteria to search for.",
            exception.getMessage()
        );
    }

    @Test
    void raiseNotEnoughCriteria2Exception() {
        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courtroom("3")
            .build();

        var exception = assertThrows(
            DartsApiException.class,
            () -> RequestValidator.validate(request)
        );
        assertEquals(
            "Search criteria is too broad, please add at least 1 more criteria to search for.",
            exception.getMessage()
        );
    }

    @Test
    void fromDateAfterToDate() {
        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .dateFrom(LocalDate.of(2023, 6, 20))
            .dateTo(LocalDate.of(2023, 6, 19))
            .caseNumber("123456")
            .build();

        var exception = assertThrows(
            DartsApiException.class,
            () -> RequestValidator.validate(request)
        );
        assertEquals(
            "The request is not valid... The 'From' date cannot be after the 'To' date.",
            exception.getMessage()
        );
    }

    @Test
    void fromDateEqualsToDate() {
        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .dateFrom(LocalDate.of(2023, 6, 20))
            .dateTo(LocalDate.of(2023, 6, 20))
            .caseNumber("123456")
            .build();

        RequestValidator.validate(request);
    }

    @Test
    void courthouseExists_butIsTooShort_shouldThrowError() {
        GetCasesSearchRequest request = buildValidRequest();
        request.setCourthouse("a");

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> RequestValidator.validate(request)
        );
        assertThat(exception.getError()).isEqualTo(CaseApiError.CRITERIA_TOO_BROAD);
        assertThat(exception.getMessage()).contains("Please include at least 3 characters.");
    }

    @Test
    void judgeNameExists_butIsTooShort_shouldThrowError() {
        GetCasesSearchRequest request = buildValidRequest();
        request.setJudgeName("a");

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> RequestValidator.validate(request)
        );
        assertThat(exception.getError()).isEqualTo(CaseApiError.CRITERIA_TOO_BROAD);
        assertThat(exception.getMessage()).contains("Please include at least 3 characters.");
    }

    @Test
    void defendantNameExists_butIsTooShort_shouldThrowError() {
        GetCasesSearchRequest request = buildValidRequest();
        request.setDefendantName("a");

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> RequestValidator.validate(request)
        );
        assertThat(exception.getError()).isEqualTo(CaseApiError.CRITERIA_TOO_BROAD);
        assertThat(exception.getMessage()).contains("Please include at least 3 characters.");
    }

    @Test
    void eventTextExists_butIsTooShort_shouldThrowError() {
        GetCasesSearchRequest request = buildValidRequest();
        request.setEventTextContains("a");

        DartsApiException exception = assertThrows(
            DartsApiException.class,
            () -> RequestValidator.validate(request)
        );
        assertThat(exception.getError()).isEqualTo(CaseApiError.CRITERIA_TOO_BROAD);
        assertThat(exception.getMessage()).contains("Please include at least 3 characters.");
    }

    private GetCasesSearchRequest buildValidRequest() {
        return GetCasesSearchRequest.builder()
            .courthouse("swansea")
            .judgeName("judge")
            .defendantName("defendant")
            .eventTextContains("event")
            .caseNumber("123456")
            .build();
    }
}
