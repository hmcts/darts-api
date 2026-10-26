package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DartsApiProblemDetailFactoryTest {

    @Test
    void givenDartsApiExceptionWithCustomProperties_whenProblemDetailCreated_thenCopiesProperties() {
        DartsApiException exception = new DartsApiException(
            TestError.TEST_ERROR,
            "Some descriptive details",
            Map.of("field", "name", "reason", "invalid")
        );

        ProblemDetail problemDetail = DartsApiProblemDetailFactory.createProblemDetail(exception);

        assertThat(problemDetail.getType()).isEqualTo(URI.create(TestError.TEST_ERROR.getType()));
        assertThat(problemDetail.getTitle()).isEqualTo(TestError.TEST_ERROR.getTitle());
        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.I_AM_A_TEAPOT.value());
        assertThat(problemDetail.getDetail()).isEqualTo("Some descriptive details");
        assertThat(problemDetail.getProperties())
            .containsEntry("field", "name")
            .containsEntry("reason", "invalid");
    }

    @Test
    void givenDartsApiExceptionWithoutCustomProperties_whenProblemDetailCreated_thenDoesNotSetProperties() {
        DartsApiException exception = new DartsApiException(TestError.TEST_ERROR);

        ProblemDetail problemDetail = DartsApiProblemDetailFactory.createProblemDetail(exception);

        assertThat(problemDetail.getProperties()).isNull();
    }

    @Test
    void givenValidationMessagesForSameField_whenAdded_thenRetainsEveryMessageAndIgnoresMissingMessages() {
        ProblemDetail problemDetail = DartsApiProblemDetailFactory.createConstraintViolationProblemDetail(
            new ServletWebRequest(new MockHttpServletRequest()));

        DartsApiProblemDetailFactory.addValidationMessage(problemDetail, "name", "must not be blank");
        DartsApiProblemDetailFactory.addValidationMessage(problemDetail, "name", null);
        DartsApiProblemDetailFactory.addValidationMessage(problemDetail, "name", "must only contain letters");
        DartsApiProblemDetailFactory.addValidationMessage(problemDetail, "email", "must be a valid email address");

        assertThat(problemDetail.getProperties())
            .containsEntry("name", "must not be blank, must only contain letters")
            .containsEntry("email", "must be a valid email address");
    }

    @Getter
    @RequiredArgsConstructor
    private enum TestError implements DartsApiError {
        TEST_ERROR(
            "TEST_999",
            HttpStatus.I_AM_A_TEAPOT,
            "A descriptive title"
        );

        private static final String ERROR_TYPE_PREFIX = "TEST";

        private final String errorTypeNumeric;
        private final HttpStatus httpStatus;
        private final String title;

        @Override
        public String getErrorTypePrefix() {
            return ERROR_TYPE_PREFIX;
        }
    }
}

