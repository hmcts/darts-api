package uk.gov.hmcts.darts.cases.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("PMD.ShortMethodName")
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
            "The request is not valid. The 'From' date cannot be after the 'To' date.",
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

    static Stream<Arguments> shortFieldTestProvider() {
        return Stream.of(
            Arguments.of("caseNumber", (Consumer<GetCasesSearchRequest>) (request) -> request.setCaseNumber("1")),
            Arguments.of("courtHouse", (Consumer<GetCasesSearchRequest>) (request) -> request.setCourthouse("1")),
            Arguments.of("courtRoom", (Consumer<GetCasesSearchRequest>) (request) -> request.setCourtroom("1")),
            Arguments.of("judgeName", (Consumer<GetCasesSearchRequest>) (request) -> request.setJudgeName("1")),
            Arguments.of("defendantName", (Consumer<GetCasesSearchRequest>) (request) -> request.setDefendantName("1"))
        );
    }

    @ParameterizedTest(name = "All fields with but has value for {0} should not error")
    @MethodSource("shortFieldTestProvider")
    void allFields_withShortField_shouldNotError(String field, Consumer<GetCasesSearchRequest> consumer) {
        GetCasesSearchRequest request = getPopulatedGetCasesSearchRequest();
        consumer.accept(request);
        RequestValidator.validate(request);
    }

    @Test
    void allFields_withShortEventText_shouldError() {
        GetCasesSearchRequest request = getPopulatedGetCasesSearchRequest();
        request.setEventTextContains("1");
        DartsApiException exception = assertThrows(DartsApiException.class, () -> RequestValidator.validate(request));
        assertThat(exception.getError()).isEqualTo(CaseApiError.CRITERIA_TOO_BROAD);
        assertThat(exception.getMessage())
            .isEqualTo("Search criteria is too broad, please include at least 3 characters.");

    }

    private GetCasesSearchRequest getPopulatedGetCasesSearchRequest() {
        return GetCasesSearchRequest.builder()
            .caseNumber("1234")
            .courthouse("1234")
            .courtroom("1234")
            .judgeName("1234")
            .defendantName("1234")
            .eventTextContains("1234")
            .dateFrom(LocalDate.of(2023, 6, 20))
            .dateTo(LocalDate.of(2023, 6, 20))
            .build();
    }


}