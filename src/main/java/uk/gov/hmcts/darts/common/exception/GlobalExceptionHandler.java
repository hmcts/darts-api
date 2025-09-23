package uk.gov.hmcts.darts.common.exception;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;
import org.zalando.problem.spring.common.HttpStatusAdapter;
import org.zalando.problem.spring.web.advice.ProblemHandling;


@ControllerAdvice
@EnableAutoConfiguration(exclude = ErrorMvcAutoConfiguration.class)
public class GlobalExceptionHandler implements ProblemHandling, DartsApiTrait, ErrorResponseAdviceTrait {

    // Override the default HttpMessageNotReadableException as this reveals class names in the exception message (DMP-3682)
    @Override
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Problem> handleMessageNotReadableException(HttpMessageNotReadableException exception,
                                                                     NativeWebRequest request) {
        Problem problem = Problem.builder()
            .withTitle("Bad Request")
            .withStatus(new HttpStatusAdapter(HttpStatus.BAD_REQUEST))
            .withDetail("JSON parse error")
            .build();
        return create(exception, problem, request);
    }
}