package uk.gov.hmcts.darts.authorisation.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_CASE_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_HEARING_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_MEDIA_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_MEDIA_REQUEST_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_TRANSCRIPTION_ID;

@Aspect
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuthorisationAspect {

    private static final String CASE_ID_PARAM = "case_id";
    private static final String HEARING_ID_PARAM = "hearing_id";
    private static final String MEDIA_REQUEST_ID_PARAM = "media_request_id";
    private static final String MEDIA_ID_PARAM = "media_id";
    private static final String TRANSCRIPTION_ID_PARAM = "transcription_id";

    private static final String BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE =
        "Unable to extract the %s in request path, query or header params for this Authorisation endpoint: %s";

    private final Authorisation authorisation;

    private final ObjectMapper objectMapper;

    @Pointcut("within(uk.gov.hmcts.darts.*.controller..*)")
    public void withinControllerPointcut() {
    }

    @Pointcut("@annotation(uk.gov.hmcts.darts.authorisation.annotation.Authorisation)")
    public void authorisationPointcut() {
    }

    private boolean handleRequestBodyAuthorisation(String method) {
        Assert.notNull(method, "Method must not be null");
        return switch (method) {
            case "POST", "PUT", "PATCH" -> true;
            default -> false;
        };
    }

    private boolean handleRequestParametersAuthorisation(String method) {
        Assert.notNull(method, "Method must not be null");
        return switch (method) {
            case "GET", "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
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

        ContextIdEnum contextId = authorisationAnnotation.contextId();

        JsonNode jsonNode = objectMapper.valueToTree(body);

        switch (contextId) {
            case CASE_ID -> authorisation.authoriseByCaseId(jsonNode.path(CASE_ID_PARAM).intValue(), roles);
            case HEARING_ID -> authorisation.authoriseByHearingId(jsonNode.path(HEARING_ID_PARAM).intValue(), roles);
            case MEDIA_REQUEST_ID ->
                authorisation.authoriseByMediaRequestId(jsonNode.path(MEDIA_REQUEST_ID_PARAM).intValue(), roles);
            case MEDIA_ID -> authorisation.authoriseByMediaId(jsonNode.path(MEDIA_ID_PARAM).intValue(), roles);
            case TRANSCRIPTION_ID ->
                authorisation.authoriseByTranscriptionId(jsonNode.path(TRANSCRIPTION_ID_PARAM).intValue(), roles);
            default -> throw new IllegalStateException(String.format(
                "The Authorisation annotation contextId is not known: %s",
                contextId
            ));
        }

        return joinPoint.proceed();
    }

    @Before("@annotation(uk.gov.hmcts.darts.authorisation.annotation.Authorisation)")
    public void handleRequestParametersAuthorisationAdvice(JoinPoint joinPoint) throws Throwable {
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

        ContextIdEnum contextId = authorisationAnnotation.contextId();

        switch (contextId) {
            case CASE_ID -> checkAuthorisationByCaseId(request, roles);
            case HEARING_ID -> checkAuthorisationByHearingId(request, roles);
            case MEDIA_REQUEST_ID -> checkAuthorisationByMediaRequestId(request, roles);
            case MEDIA_ID -> checkAuthorisationByMediaId(request, roles);
            case TRANSCRIPTION_ID -> checkAuthorisationByTranscriptionId(request, roles);
            default -> throw new IllegalStateException(String.format(
                "The Authorisation annotation contextId is not known: %s",
                contextId
            ));
        }
    }

    private void checkAuthorisationByCaseId(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> caseIdParamOptional = getPathParamValue(request, CASE_ID_PARAM);
        checkAuthorisationByCaseId(caseIdParamOptional, roles);

        if (caseIdParamOptional.isEmpty()) {
            caseIdParamOptional = Optional.ofNullable(request.getParameter(CASE_ID_PARAM));
            checkAuthorisationByCaseId(caseIdParamOptional, roles);
        }

        if (caseIdParamOptional.isEmpty()) {
            caseIdParamOptional = Optional.ofNullable(request.getHeader(CASE_ID_PARAM));
            checkAuthorisationByCaseId(caseIdParamOptional, roles);
        }

        if (caseIdParamOptional.isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                CASE_ID_PARAM,
                request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_CASE_ID);
        }
    }

    private void checkAuthorisationByCaseId(Optional<String> caseIdParamOptional, Set<SecurityRoleEnum> roles) {
        if (caseIdParamOptional.isPresent()) {
            try {
                Integer caseId = Integer.valueOf(caseIdParamOptional.get());
                authorisation.authoriseByCaseId(caseId, roles);
            } catch (NumberFormatException e) {
                log.error("Unable to parse case_id for checkAuthorisation", e);
                throw new DartsApiException(BAD_REQUEST_CASE_ID);
            }
        }
    }

    private void checkAuthorisationByHearingId(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> hearingIdParamOptional = getPathParamValue(request, HEARING_ID_PARAM);
        checkAuthorisationByHearingId(hearingIdParamOptional, roles);

        if (hearingIdParamOptional.isEmpty()) {
            hearingIdParamOptional = Optional.ofNullable(request.getParameter(HEARING_ID_PARAM));
            checkAuthorisationByHearingId(hearingIdParamOptional, roles);
        }

        if (hearingIdParamOptional.isEmpty()) {
            hearingIdParamOptional = Optional.ofNullable(request.getHeader(HEARING_ID_PARAM));
            checkAuthorisationByHearingId(hearingIdParamOptional, roles);
        }

        if (hearingIdParamOptional.isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                HEARING_ID_PARAM,
                request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_HEARING_ID);
        }
    }

    private void checkAuthorisationByHearingId(Optional<String> hearingIdParamOptional, Set<SecurityRoleEnum> roles) {
        if (hearingIdParamOptional.isPresent()) {
            try {
                Integer hearingId = Integer.valueOf(hearingIdParamOptional.get());
                authorisation.authoriseByHearingId(hearingId, roles);
            } catch (NumberFormatException e) {
                log.error("Unable to parse hearing_id for checkAuthorisation", e);
                throw new DartsApiException(BAD_REQUEST_HEARING_ID);
            }
        }
    }

    private void checkAuthorisationByMediaRequestId(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> mediaRequestIdParamOptional = getPathParamValue(request, MEDIA_REQUEST_ID_PARAM);
        checkAuthorisationByMediaRequestId(mediaRequestIdParamOptional, roles);

        if (mediaRequestIdParamOptional.isEmpty()) {
            mediaRequestIdParamOptional = Optional.ofNullable(request.getParameter(MEDIA_REQUEST_ID_PARAM));
            checkAuthorisationByMediaRequestId(mediaRequestIdParamOptional, roles);
        }

        if (mediaRequestIdParamOptional.isEmpty()) {
            mediaRequestIdParamOptional = Optional.ofNullable(request.getHeader(MEDIA_REQUEST_ID_PARAM));
            checkAuthorisationByMediaRequestId(mediaRequestIdParamOptional, roles);
        }

        if (mediaRequestIdParamOptional.isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                MEDIA_REQUEST_ID_PARAM,
                request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_MEDIA_REQUEST_ID);
        }
        Integer mediaRequestId = Integer.valueOf(mediaRequestIdParamOptional.get());
        authorisation.authoriseMediaRequestAgainstUser(mediaRequestId);
    }

    private void checkAuthorisationByMediaRequestId(Optional<String> mediaRequestIdParamOptional,
                                                    Set<SecurityRoleEnum> roles) {
        if (mediaRequestIdParamOptional.isPresent()) {
            try {
                Integer mediaRequestId = Integer.valueOf(mediaRequestIdParamOptional.get());
                authorisation.authoriseByMediaRequestId(mediaRequestId, roles);
            } catch (NumberFormatException e) {
                log.error("Unable to parse audio_request_id for checkAuthorisation", e);
                throw new DartsApiException(BAD_REQUEST_MEDIA_REQUEST_ID);
            }
        }
    }

    private void checkAuthorisationByMediaId(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> mediaIdParamOptional = getPathParamValue(request, MEDIA_ID_PARAM);
        checkAuthorisationByMediaId(mediaIdParamOptional, roles);

        if (mediaIdParamOptional.isEmpty()) {
            mediaIdParamOptional = Optional.ofNullable(request.getParameter(MEDIA_ID_PARAM));
            checkAuthorisationByMediaId(mediaIdParamOptional, roles);
        }

        if (mediaIdParamOptional.isEmpty()) {
            mediaIdParamOptional = Optional.ofNullable(request.getHeader(MEDIA_ID_PARAM));
            checkAuthorisationByMediaId(mediaIdParamOptional, roles);
        }

        if (mediaIdParamOptional.isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                MEDIA_ID_PARAM,
                request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_MEDIA_ID);
        }
    }

    private void checkAuthorisationByMediaId(Optional<String> mediaIdParamOptional, Set<SecurityRoleEnum> roles) {
        if (mediaIdParamOptional.isPresent()) {
            try {
                Integer mediaId = Integer.valueOf(mediaIdParamOptional.get());
                authorisation.authoriseByMediaId(mediaId, roles);
            } catch (NumberFormatException e) {
                log.error("Unable to parse media_id for checkAuthorisation", e);
                throw new DartsApiException(BAD_REQUEST_MEDIA_ID);
            }
        }
    }

    private void checkAuthorisationByTranscriptionId(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> transcriptionIdParamOptional = getPathParamValue(request, TRANSCRIPTION_ID_PARAM);
        checkAuthorisationByTranscriptionId(transcriptionIdParamOptional, roles);

        if (transcriptionIdParamOptional.isEmpty()) {
            transcriptionIdParamOptional = Optional.ofNullable(request.getParameter(TRANSCRIPTION_ID_PARAM));
            checkAuthorisationByTranscriptionId(transcriptionIdParamOptional, roles);
        }

        if (transcriptionIdParamOptional.isEmpty()) {
            transcriptionIdParamOptional = Optional.ofNullable(request.getHeader(TRANSCRIPTION_ID_PARAM));
            checkAuthorisationByTranscriptionId(transcriptionIdParamOptional, roles);
        }

        if (transcriptionIdParamOptional.isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                TRANSCRIPTION_ID_PARAM,
                request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_ID);
        }
    }

    private void checkAuthorisationByTranscriptionId(Optional<String> transcriptionIdParamOptional,
                                                     Set<SecurityRoleEnum> roles) {
        if (transcriptionIdParamOptional.isPresent()) {
            try {
                Integer transcriptionId = Integer.valueOf(transcriptionIdParamOptional.get());
                authorisation.authoriseByTranscriptionId(transcriptionId, roles);
            } catch (NumberFormatException e) {
                log.error("Unable to parse transcription_id for checkAuthorisation", e);
                throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_ID);
            }
        }
    }

    private Optional<String> getPathParamValue(HttpServletRequest request, String pathParam) {
        Map pathVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String pathParamValue = (String) pathVariables.get(pathParam);
        return Optional.ofNullable(pathParamValue);
    }

}
