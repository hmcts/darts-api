package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DartsApiExceptionHandlerDartsApiExceptionTest {

    private static final String REQUEST_URI = "/darts-api-exception-handler";

    private final DartsApiExceptionHandler exceptionHandler = new DartsApiExceptionHandler();

    @Test
    void givenDartsApiExceptionWithDetail_whenHandled_thenReturnsProblemDetailWithInstance() {
        DartsApiException exception = new DartsApiException(TraitError.TEST_ERROR, "Some descriptive details");

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDartsApiException(exception, webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
        assertThat(response.getBody()).satisfies(problemDetail -> {
            assertThat(problemDetail.getType()).isEqualTo(URI.create(TraitError.TEST_ERROR.getType()));
            assertThat(problemDetail.getTitle()).isEqualTo(TraitError.TEST_ERROR.getTitle());
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.I_AM_A_TEAPOT.value());
            assertThat(problemDetail.getDetail()).isEqualTo("Some descriptive details");
            assertThat(problemDetail.getInstance()).isEqualTo(URI.create(REQUEST_URI));
        });
    }

    @Test
    void givenNativeWebRequestWithoutServletRequest_whenDartsApiExceptionHandled_thenReturnsProblemDetailWithoutInstance() {
        NativeWebRequest request = mock(NativeWebRequest.class);
        DartsApiException exception = new DartsApiException(TraitError.TEST_ERROR);

        ResponseEntity<ProblemDetail> response = exceptionHandler.handleDartsApiException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
        assertThat(response.getBody()).satisfies(problemDetail -> assertThat(problemDetail.getInstance()).isNull());
    }

    private static ServletWebRequest webRequest() {
        return new ServletWebRequest(new MockHttpServletRequest("GET", REQUEST_URI));
    }

    @Getter
    @RequiredArgsConstructor
    private enum TraitError implements DartsApiError {
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



