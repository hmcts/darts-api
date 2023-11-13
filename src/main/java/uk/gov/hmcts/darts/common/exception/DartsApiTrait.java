package uk.gov.hmcts.darts.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemBuilder;
import org.zalando.problem.spring.common.HttpStatusAdapter;
import org.zalando.problem.spring.web.advice.AdviceTrait;

import java.util.Map.Entry;

public interface DartsApiTrait extends AdviceTrait {

    @ExceptionHandler
    default ResponseEntity<Problem> handleDartsApiException(DartsApiException exception, NativeWebRequest request) {
        var error = exception.getError();

        HttpStatusAdapter problemHttpStatus = new HttpStatusAdapter(error.getHttpStatus());

        ProblemBuilder problemBuilder = Problem.builder()
            .withType(error.getType())
            .withStatus(problemHttpStatus)
            .withTitle(error.getTitle())
            .withDetail(exception.getDetail());

        for (Entry<String, Object> stringStringEntry : exception.getCustomProperties().entrySet()) {
            problemBuilder.with(stringStringEntry.getKey(), stringStringEntry.getValue());
        }

        return create(exception, problemBuilder.build(), request);
    }

}
