package uk.gov.hmcts.darts.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemBuilder;
import org.zalando.problem.spring.common.HttpStatusAdapter;
import org.zalando.problem.spring.web.advice.AdviceTrait;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;

import java.io.IOException;
import java.net.URI;
import java.util.Map.Entry;

public interface DartsApiTrait extends AdviceTrait {

    Logger DARTS_API_EXCEPTION_LOGGER = LoggerFactory.getLogger(DartsApiTrait.class);

    @ExceptionHandler
    default ResponseEntity<Problem> handleDartsApiException(DartsApiException exception, NativeWebRequest request) {
        var error = exception.getError();
        if (shouldLogException(exception)) {
            DARTS_API_EXCEPTION_LOGGER.error("A darts exception occurred", exception);
        }
        HttpStatusAdapter problemHttpStatus = new HttpStatusAdapter(error.getHttpStatus());

        ProblemBuilder problemBuilder = Problem.builder()
            .withType(URI.create(error.getType()))
            .withStatus(problemHttpStatus)
            .withTitle(error.getTitle())
            .withDetail(exception.getDetail());

        for (Entry<String, Object> stringStringEntry : exception.getCustomProperties().entrySet()) {
            problemBuilder.with(stringStringEntry.getKey(), stringStringEntry.getValue());
        }

        return create(exception, getContentForException(exception), request);
    }

    static void writeErrorResponse(HttpServletResponse servletResponse, ObjectMapper mapper) throws IOException {
        servletResponse.setStatus(HttpStatus.FORBIDDEN.value());
        servletResponse.setHeader("Content-Type", "application/problem+json");
        servletResponse.getWriter().write(DartsApiTrait.getJsonForProblem(mapper, DartsApiTrait.getContentForException(new DartsApiException(
            AuthorisationError.USER_DETAILS_INVALID))));
    }

    static String getJsonForProblem(ObjectMapper mapper, Problem problem) throws JsonProcessingException {
        return mapper.writeValueAsString(problem);
    }

    static Problem getContentForException(DartsApiException exception) {
        var error = exception.getError();

        if (shouldLogException(exception)) {
            DARTS_API_EXCEPTION_LOGGER.error("A darts exception occurred", exception);
        }
        HttpStatusAdapter problemHttpStatus = new HttpStatusAdapter(error.getHttpStatus());

        ProblemBuilder problemBuilder = Problem.builder()
            .withType(URI.create(error.getType()))
            .withStatus(problemHttpStatus)
            .withTitle(error.getTitle())
            .withDetail(exception.getDetail());

        for (Entry<String, Object> stringStringEntry : exception.getCustomProperties().entrySet()) {
            problemBuilder.with(stringStringEntry.getKey(), stringStringEntry.getValue());
        }

        return problemBuilder.build();
    }

    private static boolean shouldLogException(DartsApiException exception) {
        return exception.getError() != null
            && exception.getError().getHttpStatus() != HttpStatus.UNPROCESSABLE_ENTITY;
    }
}