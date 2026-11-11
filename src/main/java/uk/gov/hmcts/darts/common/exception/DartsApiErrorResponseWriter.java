package uk.gov.hmcts.darts.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;

import java.io.IOException;
import java.net.URI;

public final class DartsApiErrorResponseWriter {

    private DartsApiErrorResponseWriter() {
        // Utility class
    }

    public static boolean isInactiveUserException(Exception exception) {
        return exception instanceof DartsApiException dartsApiException
            && dartsApiException.getError() != null
            && AuthorisationError.USER_NOT_ACTIVE.getType().equals(dartsApiException.getError().getType());
    }

    public static void writeErrorResponse(HttpServletResponse servletResponse, ObjectMapper mapper, Exception exception) throws IOException {
        writeErrorResponse(null, servletResponse, mapper, exception);
    }

    public static void writeErrorResponse(HttpServletRequest servletRequest, HttpServletResponse servletResponse, ObjectMapper mapper, Exception exception)
        throws IOException {
        HttpStatus httpStatus = isInactiveUserException(exception) ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        servletResponse.setStatus(httpStatus.value());
        servletResponse.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        var dartsException = exception instanceof DartsApiException
            ? (DartsApiException) exception
            : new DartsApiException(AuthorisationError.USER_DETAILS_INVALID);
        ProblemDetail problemDetail = DartsApiProblemDetailFactory.createProblemDetail(dartsException);

        if (servletRequest != null) {
            problemDetail.setInstance(URI.create(servletRequest.getRequestURI()));
        }

        servletResponse.getWriter().write(getJsonForProblem(mapper, problemDetail));
    }

    static String getJsonForProblem(ObjectMapper mapper, ProblemDetail problem) throws JsonProcessingException {
        return mapper.writeValueAsString(problem);
    }
}
