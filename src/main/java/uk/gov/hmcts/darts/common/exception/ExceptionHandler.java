package uk.gov.hmcts.darts.common.exception;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@EnableAutoConfiguration(exclude = ErrorMvcAutoConfiguration.class)
public class ExceptionHandler implements DartsApiTrait, ErrorResponseAdviceTrait {

}