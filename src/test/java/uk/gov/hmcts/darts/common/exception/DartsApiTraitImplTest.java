package uk.gov.hmcts.darts.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DartsApiTraitImplTest {

    private static final String REQUEST_URI = "/validation-test";

    private final TestDartsApiTraitImpl trait = new TestDartsApiTraitImpl();

    @Test
    void givenMethodArgumentNotValidException_whenHandled_thenReturnsBadRequestProblemDetailWithValidationProperties()
        throws NoSuchMethodException {
        TestRequest requestBody = new TestRequest("too-long");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(requestBody, "testRequest");
        bindingResult.addError(new FieldError("testRequest", "name", "size must be between 1 and 5"));
        MethodParameter methodParameter = new MethodParameter(
            TestController.class.getDeclaredMethod("test", TestRequest.class),
            0
        );
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<Object> response = trait.handleMethodArgumentNotValid(
            exception,
            HttpHeaders.EMPTY,
            HttpStatus.BAD_REQUEST,
            webRequest()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOfSatisfying(ProblemDetail.class, problemDetail -> {
            assertThat(problemDetail.getType()).isEqualTo(URI.create(CommonApiError.BAD_REQUEST.getType()));
            assertThat(problemDetail.getTitle()).isEqualTo(CommonApiError.BAD_REQUEST.getTitle());
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problemDetail.getInstance()).isEqualTo(URI.create(REQUEST_URI));
            assertThat(problemDetail.getProperties()).containsEntry("name", "size must be between 1 and 5");
        });
    }

    @Test
    void givenConstraintViolationException_whenHandled_thenReturnsBadRequestProblemDetailWithViolationProperties() {
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn("request.name");

        ConstraintViolation<?> constraintViolation = mock(ConstraintViolation.class);
        when(constraintViolation.getPropertyPath()).thenReturn(propertyPath);
        when(constraintViolation.getMessage()).thenReturn("must not be blank");

        ConstraintViolationException exception = new ConstraintViolationException(Set.of(constraintViolation));

        ResponseEntity<Object> response = trait.handleConstraintViolationException(exception, webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOfSatisfying(ProblemDetail.class, problemDetail -> {
            assertThat(problemDetail.getType()).isEqualTo(URI.create(CommonApiError.BAD_REQUEST.getType()));
            assertThat(problemDetail.getTitle()).isEqualTo(CommonApiError.BAD_REQUEST.getTitle());
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problemDetail.getInstance()).isEqualTo(URI.create(REQUEST_URI));
            assertThat(problemDetail.getProperties()).containsEntry("request.name", "must not be blank");
        });
    }

    @Test
    void givenHttpMessageNotReadableException_whenHandled_thenReturnsBadRequestProblemDetailWithJsonParseDetail() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Original parser message");

        ResponseEntity<Object> response = trait.handleHttpMessageNotReadable(
            exception,
            HttpHeaders.EMPTY,
            HttpStatus.BAD_REQUEST,
            webRequest()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOfSatisfying(ProblemDetail.class, problemDetail -> {
            assertThat(problemDetail.getType()).isEqualTo(URI.create("about:blank"));
            assertThat(problemDetail.getTitle()).isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase());
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problemDetail.getDetail()).isEqualTo("JSON parse error");
            assertThat(problemDetail.getInstance()).isEqualTo(URI.create(REQUEST_URI));
        });
    }

    @Test
    void givenMaxUploadSizeExceededException_whenHandled_thenConvertsSpringPayloadToBadRequestProblemDetail() {
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(1024L);

        ResponseEntity<Object> response = trait.handleMaxUploadSizeExceededException(
            exception,
            HttpHeaders.EMPTY,
            HttpStatus.PAYLOAD_TOO_LARGE,
            webRequest()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOfSatisfying(ProblemDetail.class, problemDetail -> {
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problemDetail.getInstance()).isEqualTo(URI.create(REQUEST_URI));
        });
    }

    @Test
    void givenRuntimeException_whenHandled_thenReturnsInternalServerErrorProblemDetail() {
        RuntimeException exception = new RuntimeException("Something failed");

        ResponseEntity<ProblemDetail> response = trait.handleRuntimeException(exception, webRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).satisfies(problemDetail -> {
            assertThat(problemDetail.getType()).isEqualTo(URI.create("about:blank"));
            assertThat(problemDetail.getTitle()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
            assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(problemDetail.getDetail()).isEqualTo("Something failed");
            assertThat(problemDetail.getInstance()).isEqualTo(URI.create(REQUEST_URI));
        });
    }

    private static ServletWebRequest webRequest() {
        return new ServletWebRequest(new MockHttpServletRequest("POST", REQUEST_URI));
    }

    private record TestRequest(String name) {
    }

    private static class TestController {
        void test(TestRequest request) {
        }
    }

    private static class TestDartsApiTraitImpl extends DartsApiTraitImpl {
        @Override
        public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                   HttpHeaders headers,
                                                                   HttpStatusCode status,
                                                                   WebRequest request) {
            return super.handleMethodArgumentNotValid(exception, headers, status, request);
        }

        @Override
        public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
                                                                   HttpHeaders headers,
                                                                   HttpStatusCode status,
                                                                   WebRequest request) {
            return super.handleHttpMessageNotReadable(exception, headers, status, request);
        }

        @Override
        public ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception,
                                                                           HttpHeaders headers,
                                                                           HttpStatusCode status,
                                                                           WebRequest request) {
            return super.handleMaxUploadSizeExceededException(exception, headers, status, request);
        }
    }
}
