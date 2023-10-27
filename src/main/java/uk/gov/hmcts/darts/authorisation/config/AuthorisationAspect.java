package uk.gov.hmcts.darts.authorisation.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.authorisation.service.ControllerAuthorisationFactory;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Set;

@Aspect
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuthorisationAspect {

    private final ControllerAuthorisationFactory controllerAuthorisationFactory;
    private final ObjectMapper objectMapper;

    @Pointcut("within(uk.gov.hmcts.darts.*.controller..*)")
    public void withinControllerPointcut() {
    }

    @Pointcut("@annotation(uk.gov.hmcts.darts.authorisation.annotation.Authorisation)")
    public void authorisationPointcut() {
    }

    @Around("authorisationPointcut() && withinControllerPointcut() && args(@RequestBody body)")
    public Object handleRequestBodyAuthorisationAdvice(ProceedingJoinPoint joinPoint, Object body)
        throws Throwable {
        uk.gov.hmcts.darts.authorisation.annotation.Authorisation authorisationAnnotation = ((MethodSignature) joinPoint.getSignature()).getMethod()
            .getAnnotation(uk.gov.hmcts.darts.authorisation.annotation.Authorisation.class);

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        if (!authorisationAnnotation.bodyAuthorisation() || !handleRequestBodyAuthorisation(request.getMethod())) {
            return joinPoint.proceed();
        }

        Set<SecurityRoleEnum> roles = Set.of(authorisationAnnotation.securityRoles());
        if (roles.isEmpty()) {
            throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_FOR_COURTHOUSE);
        }

        JsonNode jsonNode = objectMapper.valueToTree(body);

        Set<ContextIdEnum> contexts = Set.of(authorisationAnnotation.contextId());
        for ()
        controllerAuthorisationFactory.getHandler(authorisationAnnotation.contextId())
            .checkAuthorisation(jsonNode, roles);

        return joinPoint.proceed();
    }

    @Before("authorisationPointcut() && withinControllerPointcut()")
    public void handleRequestParametersAuthorisationAdvice(JoinPoint joinPoint) {
        uk.gov.hmcts.darts.authorisation.annotation.Authorisation authorisationAnnotation = ((MethodSignature) joinPoint.getSignature()).getMethod()
            .getAnnotation(uk.gov.hmcts.darts.authorisation.annotation.Authorisation.class);

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        if (authorisationAnnotation.bodyAuthorisation() || !handleRequestParametersAuthorisation(request.getMethod())) {
            return;
        }

        Set<SecurityRoleEnum> roles = Set.of(authorisationAnnotation.securityRoles());
        if (roles.isEmpty()) {
            throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_FOR_COURTHOUSE);
        }

        controllerAuthorisationFactory.getHandler(authorisationAnnotation.contextId())
            .checkAuthorisation(request, roles);
    }

    private boolean handleRequestBodyAuthorisation(@NotNull String method) {
        return switch (method) {
            case "POST", "PUT", "PATCH" -> true;
            default -> false;
        };
    }

    private boolean handleRequestParametersAuthorisation(@NotNull String method) {
        return switch (method) {
            case "GET", "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }

}
