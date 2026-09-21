package uk.gov.hmcts.darts.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Locale;

@ControllerAdvice
@EnableAutoConfiguration(exclude = ErrorMvcAutoConfiguration.class)
public class DartsApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DartsApiExceptionHandler.class);
    private static final String JSON_PARSE_ERROR_DETAIL = "JSON parse error";

    @ExceptionHandler
    public ResponseEntity<ProblemDetail> handleDartsApiException(DartsApiException exception, NativeWebRequest request) {
        if (shouldLogException(exception)) {
            LOGGER.error("A darts exception occurred", exception);
        }

        var problemDetail = DartsApiProblemDetailFactory.createProblemDetail(exception, request);
        return new ResponseEntity<>(problemDetail, exception.getError().getHttpStatus());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problemDetail = DartsApiProblemDetailFactory.createConstraintViolationProblemDetail(request);

        for (FieldError fieldError : exception.getFieldErrors()) {
            problemDetail.setProperty(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return handleExceptionInternal(exception, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        ProblemDetail problemDetail = DartsApiProblemDetailFactory.createConstraintViolationProblemDetail(request);
        Locale locale = LocaleContextHolder.getLocale();

        for (ParameterValidationResult validationResult : exception.getParameterValidationResults()) {
            String parameterName = validationResult.getMethodParameter().getParameterName();
            if (parameterName == null || parameterName.isBlank()) {
                parameterName = "arg" + validationResult.getMethodParameter().getParameterIndex();
            }
            List<String> messages = validationResult.getResolvableErrors()
                .stream()
                .map(error -> getMessageSource().getMessage(error, locale))
                .toList();

            if (!messages.isEmpty()) {
                problemDetail.setProperty(parameterName, String.join(", ", messages));
            }
        }

        return handleExceptionInternal(exception, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException exception,
                                                                        NativeWebRequest request) {
        ProblemDetail problemDetail = DartsApiProblemDetailFactory.createConstraintViolationProblemDetail(request);

        for (ConstraintViolation<?> constraintViolation : exception.getConstraintViolations()) {
            problemDetail.setProperty(constraintViolation.getPropertyPath().toString(), constraintViolation.getMessage());
        }

        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, JSON_PARSE_ERROR_DETAIL);
        problemDetail.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        DartsApiProblemDetailFactory.setInstance(problemDetail, request);

        return handleExceptionInternal(exception, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        ProblemDetail problemDetail = exception.getBody();
        problemDetail.setStatus(HttpStatus.BAD_REQUEST);
        DartsApiProblemDetailFactory.setInstance(problemDetail, request);

        return handleExceptionInternal(exception, problemDetail, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<ProblemDetail> handleRuntimeException(RuntimeException exception, NativeWebRequest request) {
        LOGGER.error("An unexpected exception occurred", exception);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage());
        problemDetail.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        DartsApiProblemDetailFactory.setInstance(problemDetail, request);

        return new ResponseEntity<>(problemDetail, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static boolean shouldLogException(DartsApiException exception) {
        DartsApiError error = exception.getError();
        return error.shouldLogException() && error.getHttpStatus() != HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
