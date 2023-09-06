package uk.gov.hmcts.darts.authorisation.configuration;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_CASE_ID_REQUEST;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_HEARING_ID_REQUEST;

@Aspect
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuthorisationAspect {

    private static final String CASE_ID_PARAM = "case_id";
    private static final String HEARING_ID_PARAM = "hearing_id";

    private final Authorisation authorisation;

    /**
     * Regex to capture any {case_id} path param which may not have a trailing slash e.g. /cases/{case_id}
     */
    private static final Pattern CASES_ID_PATH_PATTERN = Pattern.compile("(?<=\\/cases\\/)(\\d+?)(?=\\/|$)");
    /**
     * Regex to capture any {hearing_id} path param which may not have a trailing slash e.g.
     * /hearings/{hearing_id}/events
     */
    private static final Pattern HEARINGS_ID_PATH_PATTERN = Pattern.compile("(?<=\\/hearings\\/)(\\d+?)(?=\\/|$)");


    @Around("@annotation(uk.gov.hmcts.darts.authorisation.annotation.Authorisation)")
    public void authorisation(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        uk.gov.hmcts.darts.authorisation.annotation.Authorisation authorisationAnnotation = ((MethodSignature) joinPoint.getSignature()).getMethod()
            .getAnnotation(uk.gov.hmcts.darts.authorisation.annotation.Authorisation.class);
        ContextIdEnum contextId = authorisationAnnotation.contextId();

        switch (contextId) {
            case CASE_ID:
                checkAuthorisationByCaseId(request);
                break;
            case HEARING_ID:
                checkAuthorisationByHearingId(request);
                break;
            default:
                log.warn("Unrecognised contextId");
                break;
        }
    }


    private void checkAuthorisationByCaseId(HttpServletRequest request) {
        Optional<String> caseIdParamOptional = Optional.empty();

        Matcher matcher = CASES_ID_PATH_PATTERN.matcher(request.getRequestURI());
        if (matcher.find()) {
            caseIdParamOptional = Optional.ofNullable(matcher.group(0));
            checkAuthorisationByCaseId(caseIdParamOptional);
        }

        if (caseIdParamOptional.isEmpty()) {
            caseIdParamOptional = Optional.ofNullable(request.getParameter(CASE_ID_PARAM));
            checkAuthorisationByCaseId(caseIdParamOptional);
        }

        if (caseIdParamOptional.isEmpty()) {
            caseIdParamOptional = Optional.ofNullable(request.getHeader(CASE_ID_PARAM));
            checkAuthorisationByCaseId(caseIdParamOptional);
        }

    }

    private void checkAuthorisationByCaseId(Optional<String> caseIdParamOptional) {
        if (caseIdParamOptional.isPresent()) {
            try {
                Integer caseId = Integer.valueOf(caseIdParamOptional.get());
                authorisation.authoriseByCaseId(caseId);
            } catch (NumberFormatException e) {
                log.error("Unable to parse case_id for checkAuthorisation", e);
                throw new DartsApiException(BAD_CASE_ID_REQUEST);
            }
        }
    }

    private void checkAuthorisationByHearingId(HttpServletRequest request) {
        Optional<String> hearingIdParamOptional = Optional.empty();

        Matcher matcher = HEARINGS_ID_PATH_PATTERN.matcher(request.getRequestURI());
        if (matcher.find()) {
            hearingIdParamOptional = Optional.ofNullable(matcher.group(0));
            checkAuthorisationByHearingId(hearingIdParamOptional);
        }

        if (hearingIdParamOptional.isEmpty()) {
            hearingIdParamOptional = Optional.ofNullable(request.getParameter(HEARING_ID_PARAM));
            checkAuthorisationByHearingId(hearingIdParamOptional);
        }

        if (hearingIdParamOptional.isEmpty()) {
            hearingIdParamOptional = Optional.ofNullable(request.getHeader(HEARING_ID_PARAM));
            checkAuthorisationByHearingId(hearingIdParamOptional);
        }

    }

    private void checkAuthorisationByHearingId(Optional<String> hearingIdParamOptional) {
        if (hearingIdParamOptional.isPresent()) {
            try {
                Integer hearingId = Integer.valueOf(hearingIdParamOptional.get());
                authorisation.authoriseByHearingId(hearingId);
            } catch (NumberFormatException e) {
                log.error("Unable to parse hearing_id for checkAuthorisation", e);
                throw new DartsApiException(BAD_HEARING_ID_REQUEST);
            }
        }
    }

}
