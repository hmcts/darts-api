package uk.gov.hmcts.darts.common.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * AdviceTrait for handling Spring 6 {@link org.springframework.web.ErrorResponseException}.
 * Temporary workaround until <a href="https://github.com/zalando/problem-spring-web">problem-spring-web</a> is extended with support for {@link org.springframework.web.ErrorResponse}.
 * For now scope is restricted to {@link org.springframework.web.servlet.resource.NoResourceFoundException}, but additional exceptions can be added as needed.
 */
public interface ErrorResponseAdviceTrait {

    @ExceptionHandler({
        NoResourceFoundException.class,
    })
    default ResponseEntity<ProblemDetail> handleErrorResponseException(ErrorResponse errorResponse,
                                                                       Exception errorResponseAsException, NativeWebRequest request) {
        ProblemDetail body = errorResponse.getBody();
        return  new ResponseEntity<ProblemDetail>(body, errorResponse.getStatusCode());
    }
}