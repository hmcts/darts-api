package uk.gov.hmcts.darts.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.util.HashMap;

final class DartsApiProblemDetailFactory {

    private DartsApiProblemDetailFactory() {
        // Utility class
    }

    static ProblemDetail createProblemDetail(DartsApiException exception) {
        var error = exception.getError();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(error.getHttpStatus(), exception.getDetail());
        problemDetail.setType(URI.create(error.getType()));
        problemDetail.setTitle(error.getTitle());

        if (exception.getCustomProperties().isEmpty()) {
            return problemDetail;
        }

        problemDetail.setProperties(new HashMap<>(exception.getCustomProperties()));

        return problemDetail;
    }

    static ProblemDetail createProblemDetail(DartsApiException exception, WebRequest request) {
        ProblemDetail problemDetail = createProblemDetail(exception);
        setInstance(problemDetail, request);
        return problemDetail;
    }

    static ProblemDetail createConstraintViolationProblemDetail(WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(CommonApiError.BAD_REQUEST.getHttpStatus());
        problemDetail.setType(URI.create(CommonApiError.BAD_REQUEST.getType()));
        problemDetail.setTitle(CommonApiError.BAD_REQUEST.getTitle());
        setInstance(problemDetail, request);
        problemDetail.setProperties(new HashMap<>());
        return problemDetail;
    }

    static void setInstance(ProblemDetail problemDetail, WebRequest request) {
        if (request instanceof NativeWebRequest nativeWebRequest) {
            problemDetail.setInstance(getNativeRequestUri(nativeWebRequest));
        }
    }

    private static URI getNativeRequestUri(NativeWebRequest request) {
        HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
        if (servletRequest == null) {
            return null;
        }
        return URI.create(servletRequest.getRequestURI());
    }
}

