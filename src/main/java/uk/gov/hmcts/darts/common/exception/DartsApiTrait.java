package uk.gov.hmcts.darts.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map.Entry;

public interface DartsApiTrait {

    Logger DARTS_API_EXCEPTION_LOGGER = LoggerFactory.getLogger(DartsApiTrait.class);

    @ExceptionHandler
    default ResponseEntity<ProblemDetail> handleDartsApiException(DartsApiException exception, NativeWebRequest request) {
        if (shouldLogException(exception)) {
            DARTS_API_EXCEPTION_LOGGER.error("A darts exception occurred", exception);
        }

        var problemDetail = getContentForException(exception);
        problemDetail.setInstance(getInstance(request));
        return new ResponseEntity<>(problemDetail, exception.getError().getHttpStatus());
    }

    static boolean isInactiveUserException(Exception exception) {
        return exception instanceof DartsApiException dartsApiException
            && dartsApiException.getError() != null
            && AuthorisationError.USER_NOT_ACTIVE.getType().equals(dartsApiException.getError().getType());
    }

    static void writeErrorResponse(HttpServletResponse servletResponse, ObjectMapper mapper, Exception exception) throws IOException {
        HttpStatus httpStatus = isInactiveUserException(exception) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        servletResponse.setStatus(httpStatus.value());
        servletResponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        var dartsException = exception instanceof DartsApiException
            ? (DartsApiException) exception
            : new DartsApiException(AuthorisationError.USER_DETAILS_INVALID);
        servletResponse.getWriter().write(getJsonForProblem(mapper, getContentForException(dartsException)));
    }

    static String getJsonForProblem(ObjectMapper mapper, ProblemDetail problem) throws JsonProcessingException {
        return mapper.writeValueAsString(problem);
    }

    static ProblemDetail getContentForException(DartsApiException exception) {
        var error = exception.getError();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(error.getHttpStatus(), exception.getDetail());
        problemDetail.setType(URI.create(error.getType()));
        problemDetail.setTitle(error.getTitle());

        if (!exception.getCustomProperties().isEmpty()) {
            problemDetail.setProperties(new HashMap<>());
        }

        for (Entry<String, Object> stringStringEntry : exception.getCustomProperties().entrySet()) {
            problemDetail.getProperties().put(stringStringEntry.getKey(), stringStringEntry.getValue());
        }

        return problemDetail;
    }

    private static URI getInstance(NativeWebRequest request) {
        HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
        if (servletRequest == null) {
            return null;
        }
        return URI.create(servletRequest.getRequestURI());
    }

    private static boolean shouldLogException(DartsApiException exception) {
        DartsApiError error = exception.getError();
        return error.shouldLogException() && error.getHttpStatus() != HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
