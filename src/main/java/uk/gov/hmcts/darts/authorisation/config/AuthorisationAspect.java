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

import java.util.List;
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

        List<ContextIdEnum> contextIds = List.of(authorisationAnnotation.contextId());

        contextIds.stream().forEach(
            contextId -> controllerAuthorisationFactory.getHandler(contextId).checkAuthorisation(jsonNode, roles)
        );
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

        List<ContextIdEnum> contextIds = List.of(authorisationAnnotation.contextId());

        contextIds.stream().forEach(
            contextId -> controllerAuthorisationFactory.getHandler(contextId).checkAuthorisation(request, roles)
        );

        contextIds.stream().filter(
            contextId -> handleContext(contextId, request, roles)
        ).findFirst().orElseThrow(() -> new DartsApiException(AuthorisationError.BAD_REQUEST));
    }

    private boolean handleContext(ContextIdEnum contextId, HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        boolean success = false;
        try {
            controllerAuthorisationFactory.getHandler(contextId).checkAuthorisation(request, roles);
            success = true;
        } catch (DartsApiException e) {
            log.error("Unable to authenticate ", e);
        }
        return success;
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
