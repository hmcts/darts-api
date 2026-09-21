package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

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

