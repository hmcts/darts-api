package uk.gov.hmcts.darts.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.model.PostAdminSearchRequest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;


@ExtendWith(MockitoExtension.class)
class AdminSearchRequestValidatorTest {

    @InjectMocks
    private AdminSearchRequestValidator validator;

    private PostAdminSearchRequest request;
    private DartsApiError criteriaTooBroad;
    private DartsApiError invalidRequest;

    @BeforeEach
    void setUp() {
        validator.hearingDatesMaxSearchPeriod = "P1Y";

        request = PostAdminSearchRequest.builder().build();
        criteriaTooBroad = mock(DartsApiError.class);
        invalidRequest = mock(DartsApiError.class);

    }

    @Test
    void validate_shouldNotThrowException_WhenCaseNumberIsProvided() {
        request.setCaseNumber("12345");
        assertDoesNotThrow(() -> validator.validate(request, criteriaTooBroad, invalidRequest));
    }

    @Test
    void validate_shouldThrowException_WhenCourthouseIdsAreEmpty() {
        request.setCaseNumber("");
        request.setCourthouseIds(null);

        assertThrows(DartsApiException.class, () -> validator.validate(request, criteriaTooBroad, invalidRequest));
    }

    @Test
    void validate_shouldThrowException_WhenHearingStartAtIsAfterHearingEndAt() {
        request.setHearingStartAt(LocalDate.of(2023, 1, 2));
        request.setHearingEndAt(LocalDate.of(2023, 1, 1));

        assertThrows(DartsApiException.class, () -> validator.validate(request, criteriaTooBroad, invalidRequest));
    }

    @Test
    void validate_shouldThrowException_WhenHearingPeriodExceedsMaxSearchPeriod() {
        request.setCourthouseIds(List.of(1));
        request.setHearingStartAt(LocalDate.of(2022, 1, 1));
        request.setHearingEndAt(LocalDate.of(2023, 2, 1));

        assertThrows(DartsApiException.class, () -> validator.validate(request, criteriaTooBroad, invalidRequest));
    }

    @Test
    void validate_shouldNotThrowException_WhenHearingPeriodIsWithinMaxSearchPeriod() {
        request.setCourthouseIds(List.of(1));
        request.setHearingStartAt(LocalDate.of(2022, 1, 2));
        request.setHearingEndAt(LocalDate.of(2023, 1, 1));

        assertDoesNotThrow(() -> validator.validate(request, criteriaTooBroad, invalidRequest));
    }
}