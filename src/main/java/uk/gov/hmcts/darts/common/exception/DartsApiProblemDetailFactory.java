package uk.gov.hmcts.darts.common.exception;

import org.springframework.http.ProblemDetail;

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
}


