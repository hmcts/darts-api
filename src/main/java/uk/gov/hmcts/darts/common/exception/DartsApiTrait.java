package uk.gov.hmcts.darts.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.HashMap;
import java.util.Map.Entry;

public interface DartsApiTrait {

    Logger DARTS_API_EXCEPTION_LOGGER = LoggerFactory.getLogger(DartsApiTrait.class);

    @ExceptionHandler
    default ResponseEntity<ProblemDetail> handleDartsApiException(DartsApiException exception, NativeWebRequest request) {
        var error = exception.getError();

        DARTS_API_EXCEPTION_LOGGER.error("A darts exception occurred", exception);

        ProblemDetail problemDetail = ProblemDetail
            .forStatusAndDetail(error.getHttpStatus(), exception.getDetail());
        problemDetail.setType(error.getType());
        problemDetail.setTitle(error.getTitle());

        for (Entry<String, Object> stringStringEntry : exception.getCustomProperties().entrySet()) {
            if (problemDetail.getProperties() == null) {
                problemDetail.setProperties(new HashMap<>());
            }
            problemDetail.getProperties().put(stringStringEntry.getKey(), stringStringEntry.getValue());
        }

        return new ResponseEntity<ProblemDetail>(problemDetail, error.getHttpStatus());
    }


    @ExceptionHandler
    default ResponseEntity<ProblemDetail> exception(Exception exception, NativeWebRequest request) {
        DARTS_API_EXCEPTION_LOGGER.error("A  exception occurred", exception);

        ProblemDetail problemDetail = ProblemDetail
            .forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "A runtime exception occurred");

        return new ResponseEntity<ProblemDetail>(problemDetail, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}