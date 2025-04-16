package uk.gov.hmcts.darts.common.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.zalando.problem.Problem;
import org.zalando.problem.spring.common.HttpStatusAdapter;
import org.zalando.problem.spring.web.advice.AdviceTrait;

/**
 * AdviceTrait for handling Spring 6 {@link org.springframework.web.ErrorResponseException}.
 * Temporary workaround until <a href="https://github.com/zalando/problem-spring-web">problem-spring-web</a> is extended with support for {@link org.springframework.web.ErrorResponse}.
 * For now scope is restricted to {@link org.springframework.web.servlet.resource.NoResourceFoundException}, but additional exceptions can be added as needed.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")//False positive
public interface ErrorResponseAdviceTrait extends AdviceTrait {

    @ExceptionHandler({
        NoResourceFoundException.class,
    })
    default ResponseEntity<Problem> handleErrorResponseException(ErrorResponse errorResponse, Exception errorResponseAsException, NativeWebRequest request) {

        var problem = toProblem(errorResponse);

        return create(errorResponseAsException, problem, request);
    }

    private Problem toProblem(ErrorResponse errorResponse) {

        var problemHttpStatus = new HttpStatusAdapter(errorResponse.getStatusCode());
        ProblemDetail body = errorResponse.getBody();
        return Problem.builder()
            .withType(body.getType())
            .withStatus(problemHttpStatus)
            .withTitle(body.getTitle())
            .withDetail(body.getDetail())
            .build();
    }
}
