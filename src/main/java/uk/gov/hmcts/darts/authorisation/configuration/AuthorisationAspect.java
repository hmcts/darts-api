package uk.gov.hmcts.darts.authorisation.configuration;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_CASE_ID_REQUEST;

@Aspect
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuthorisationAspect {

    private static final String CASE_ID_PARAM = "case_id";

    private final Authorisation authorisation;

    /**
     * Regex to capture any {case_id} path param which may not have a trailing slash e.g. /cases/{case_id}
     */
    private static final Pattern CASES_ID_PATH_PATTERN = Pattern.compile("(?<=\\/cases\\/)(.*?)(?=\\/|$)");

    @Before("@annotation(uk.gov.hmcts.darts.authorisation.annotation.Authorisation)")
    public void before() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        Optional<String> caseIdParamOptional = Optional.empty();

        Matcher matcher = CASES_ID_PATH_PATTERN.matcher(request.getRequestURI());
        if (matcher.find()) {
            caseIdParamOptional = Optional.ofNullable(matcher.group(0));
            checkAuthorisation(caseIdParamOptional);
            return;
        }

        if (caseIdParamOptional.isEmpty()) {
            caseIdParamOptional = Optional.ofNullable(request.getParameter(CASE_ID_PARAM));
            checkAuthorisation(caseIdParamOptional);
            return;
        }

        if (caseIdParamOptional.isEmpty()) {
            caseIdParamOptional = Optional.ofNullable(request.getHeader(CASE_ID_PARAM));
            checkAuthorisation(caseIdParamOptional);
        }

    }

    private void checkAuthorisation(Optional<String> caseIdParamOptional) {
        if (caseIdParamOptional.isPresent()) {
            try {
                Integer caseId = Integer.valueOf(caseIdParamOptional.get());
                authorisation.authorise(caseId);
            } catch (NumberFormatException e) {
                log.error("Unable to parse case_id for checkAuthorisation", e);
                throw new DartsApiException(BAD_CASE_ID_REQUEST);
            }
        }
    }

}
