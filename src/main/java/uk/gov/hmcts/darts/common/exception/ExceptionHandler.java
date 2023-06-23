package uk.gov.hmcts.darts.common.exception;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.zalando.problem.spring.web.advice.ProblemHandling;

@ControllerAdvice
@EnableAutoConfiguration(exclude = ErrorMvcAutoConfiguration.class)
public class ExceptionHandler implements ProblemHandling, DartsApiTrait {

}
