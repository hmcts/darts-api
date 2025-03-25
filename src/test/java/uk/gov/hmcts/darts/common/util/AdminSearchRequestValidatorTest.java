package uk.gov.hmcts.darts.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.model.PostAdminSearchRequest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(MockitoExtension.class)
class AdminSearchRequestValidatorTest {

    private AdminSearchRequestValidator validator;

    private PostAdminSearchRequest request;

    @BeforeEach
    void setUp() {
        validator = new AdminSearchRequestValidator();
        validator.hearingDatesMaxSearchPeriod = "P1Y";

        request = PostAdminSearchRequest.builder().build();
    }

    @Test
    void validate_shouldNotThrowException_WhenCaseNumberIsProvided() {
        request.setCaseNumber("12345");
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    void validate_shouldThrowException_WhenCourthouseIdsAndCaseNumberAreEmpty() {
        request.setCaseNumber("");
        request.setCourthouseIds(List.of());

        assertThrows(DartsApiException.class, () -> validator.validate(request));
    }

    @Test
    void validate_shouldThrowException_WhenHearingStartAtIsAfterHearingEndAt() {
        request.setHearingStartAt(LocalDate.of(2023, 1, 2));
        request.setHearingEndAt(LocalDate.of(2023, 1, 1));

        var exception = assertThrows(DartsApiException.class, () -> validator.validate(request));

        assertEquals("Invalid request. The hearing start date cannot be after the end date.", exception.getMessage());
    }

    @Test
    void validate_shouldThrowException_WhenHearingPeriodExceedsMaxSearchPeriod() {
        request.setCourthouseIds(List.of(1));
        request.setHearingStartAt(LocalDate.of(2022, 1, 1));
        request.setHearingEndAt(LocalDate.of(2023, 2, 1));

        var exception = assertThrows(DartsApiException.class, () -> validator.validate(request));
        assertEquals("Invalid request. The time between the start and end date cannot be more than 12 months", exception.getMessage());
    }

    @Test
    void validate_shouldNotThrowException_WhenHearingPeriodIsWithinMaxSearchPeriodAndCourthouseIdProvided() {
        request.setCourthouseIds(List.of(1));
        request.setHearingStartAt(LocalDate.of(2022, 1, 2));
        request.setHearingEndAt(LocalDate.of(2023, 1, 2));

        assertDoesNotThrow(() -> validator.validate(request));
    }
}