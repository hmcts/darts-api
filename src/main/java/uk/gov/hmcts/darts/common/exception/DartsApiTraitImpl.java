package uk.gov.hmcts.darts.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

@ControllerAdvice
public class DartsApiTraitImpl extends ResponseEntityExceptionHandler implements DartsApiTrait {


    /**
     * We can influence JSR 303 errors here in a standardised RFC 7808 manner. We could convert field based
     * exceptions to unique error codes making our whole API consistent
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {

        ProblemDetail problemDetail = ex.getBody();
        problemDetail.setTitle("Constraint Violation");
        problemDetail.setDetail("");

        // add the failure specifics to the problem detail properties
        for (FieldError fieldError : ex.getFieldErrors()) {
            if (problemDetail.getProperties() == null) {
                problemDetail.setProperties(new HashMap<>());
            }
            problemDetail.getProperties().put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return new ResponseEntity<Object>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        return super.handleHandlerMethodValidationException(ex, headers, status, request);
    }

    @ExceptionHandler
    protected ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, NativeWebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle("Constraint Violation");
        problemDetail.setDetail(ex.getMessage());

        Set<ConstraintViolation<?>> constraintViolationExceptionSet = ex.getConstraintViolations();

        for (ConstraintViolation fieldError : constraintViolationExceptionSet) {

            if (problemDetail.getProperties() == null) {
                problemDetail.setProperties(new HashMap<>());
            }

            problemDetail.getProperties().put(fieldError.getPropertyPath().toString(), fieldError.getMessage());
        }

        return new ResponseEntity<Object>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status,
                                                                          WebRequest request) {
        ProblemDetail problemDetail = ex.getBody();
        problemDetail.setStatus(HttpStatus.BAD_REQUEST);

        MultiValueMap<String, String> myheaders = new HttpHeaders();
        myheaders.put("Content-Type", List.of("application/json"));

        return new ResponseEntity<Object>(problemDetail, myheaders, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex, HttpHeaders headers, HttpStatusCode status,
                                                                     WebRequest request) {
        return super.handleMissingServletRequestPart(ex, headers, status, request);
    }
}