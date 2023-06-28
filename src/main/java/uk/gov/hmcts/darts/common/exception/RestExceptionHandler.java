package uk.gov.hmcts.darts.common.exception;

import jakarta.validation.ValidationException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.annotation.Order;
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

/*
 * @deprecated in favour of RFC-7807 compliant handler uk.gov.hmcts.darts.common.exception.ExceptionHandler
 * TODO: Eliminate this class (DMP-516)
 */
@Deprecated(forRemoval = true)
@SuppressWarnings("PMD.LawOfDemeter")
@RestControllerAdvice
/*
 * Temporarily prioritise this handler over uk.gov.hmcts.darts.common.exception.ExceptionHandler to keep behaviour
 * consistent with existing test expectations
 */
@Order(1)
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @Deprecated
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

    @Deprecated
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(DataIntegrityViolationException exception) {
        BasicRestException basicRestException = new BasicRestException();
        basicRestException.setCode(HttpStatus.CONFLICT.toString());
        basicRestException.setMessage(NestedExceptionUtils.getMostSpecificCause(exception).getMessage());
        return new ResponseEntity<>(basicRestException, HttpStatus.CONFLICT);
    }


    @Deprecated
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidationException(ValidationException exception) {
        BasicRestException basicRestException = new BasicRestException();
        basicRestException.setCode(HttpStatus.BAD_REQUEST.toString());
        basicRestException.setMessage(NestedExceptionUtils.getMostSpecificCause(exception).getMessage());
        return new ResponseEntity<>(basicRestException, HttpStatus.BAD_REQUEST);
    }
}
