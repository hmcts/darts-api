package uk.gov.hmcts.darts.common.error;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;
import java.util.Optional;


@SuppressWarnings("PMD.LawOfDemeter")
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        BasicRestException exception = new BasicRestException();
        exception.setCode(HttpStatus.BAD_REQUEST.toString());
        var message = Optional.of(ex).map(MethodArgumentNotValidException::getBindingResult).map(Errors::getFieldError).orElse(
            null);

        exception.setMessage(Objects.isNull(message) ? "unidentified error" : message.getField() + " " + message.getDefaultMessage());

        return new ResponseEntity<>(exception, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(DataIntegrityViolationException exception) {
        BasicRestException basicRestException = new BasicRestException();
        basicRestException.setCode(HttpStatus.CONFLICT.toString());
        basicRestException.setMessage(NestedExceptionUtils.getMostSpecificCause(exception).getMessage());
        return new ResponseEntity<>(basicRestException, HttpStatus.CONFLICT);
    }
}
