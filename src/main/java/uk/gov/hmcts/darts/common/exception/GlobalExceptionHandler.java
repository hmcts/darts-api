package uk.gov.hmcts.darts.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.zalando.problem.Problem;
import org.zalando.problem.spring.common.HttpStatusAdapter;
import org.zalando.problem.spring.web.advice.ProblemHandling;

import java.io.IOException;

@Slf4j
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

    @ExceptionHandler({
        AsyncRequestNotUsableException.class,
        ClientAbortException.class
    })
    public ResponseEntity<Void> handleClientAbortDuringWrite(Exception ex, NativeWebRequest request) {
        log.warn("Client aborted connection while streaming audio");
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Void> handleIoException(IOException ex, NativeWebRequest request) throws IOException {
        String msg = ex.getMessage();
        if (msg != null 
            &&
            (msg.contains("Connection reset by peer") || msg.contains("Broken pipe"))) {
            log.warn("Client aborted connection while streaming audio");
            return ResponseEntity.noContent().build();
        }
        throw ex;
    }
}