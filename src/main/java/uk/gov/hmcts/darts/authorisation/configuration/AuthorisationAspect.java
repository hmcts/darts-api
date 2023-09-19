package uk.gov.hmcts.darts.authorisation.configuration;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Regex to capture any path param which may not have a trailing slash e.g. /cases/{case_id}
     */
    private static final String PATH_PARAM_REGEX = "(?<=\\/%s\\/)(\\d+?)(?=\\/|$)";

    private static final String CASES_PATH = "cases";
    private static final String CASE_ID_PARAM = "case_id";

    private static final String HEARINGS_PATH = "hearings";
    private static final String HEARING_ID_PARAM = "hearing_id";

    private static final String AUDIO_REQUESTS_PATH = "audio-requests";
    private static final String AUDIO_REQUEST_ID_PARAM = "media_request_id";

    private static final String AUDIOS_PATH = "audio/preview"; //FIXME should probably be "audios"
    private static final String AUDIO_ID_PARAM = "media_id";

    private static final String TRANSCRIPTIONS_PATH = "transcriptions";
    private static final String TRANSCRIPTION_ID_PARAM = "transcription_id";

    private static final Pattern CASES_ID_PATH_PATTERN = Pattern.compile(
        String.format(PATH_PARAM_REGEX, CASES_PATH));

    private static final Pattern HEARINGS_ID_PATH_PATTERN = Pattern.compile(
        String.format(PATH_PARAM_REGEX, HEARINGS_PATH));

    private static final Pattern AUDIO_REQUESTS_ID_PATH_PATTERN = Pattern.compile(
        String.format(PATH_PARAM_REGEX, AUDIO_REQUESTS_PATH));

    private static final Pattern AUDIOS_ID_PATH_PATTERN = Pattern.compile(
        String.format(PATH_PARAM_REGEX, AUDIOS_PATH));

    private static final Pattern TRANSCRIPTIONS_ID_PATH_PATTERN = Pattern.compile(
        String.format(PATH_PARAM_REGEX, TRANSCRIPTIONS_PATH));

    private final Authorisation authorisation;

    @Before("@annotation(uk.gov.hmcts.darts.authorisation.annotation.Authorisation)")
    public void authorisation(JoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        uk.gov.hmcts.darts.authorisation.annotation.Authorisation authorisationAnnotation = ((MethodSignature) joinPoint.getSignature()).getMethod()
            .getAnnotation(uk.gov.hmcts.darts.authorisation.annotation.Authorisation.class);

        ContextIdEnum contextId = authorisationAnnotation.contextId();
        Set<SecurityRoleEnum> roles = Set.of(authorisationAnnotation.securityRoles());

        switch (contextId) {
            case CASE_ID:
                checkAuthorisationByCaseId(request, roles);
                break;
            case HEARING_ID:
                checkAuthorisationByHearingId(request, roles);
                break;
            case MEDIA_REQUEST_ID:
                checkAuthorisationByMediaRequestId(request, roles);
                break;
            case MEDIA_ID:
                checkAuthorisationByMediaId(request, roles);
                break;
            case TRANSCRIPTION_ID:
                checkAuthorisationByTranscriptionId(request, roles);
                break;
            default:
                log.warn("Unrecognised contextId");
                break;
        }
    }

    private void checkAuthorisationByCaseId(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> caseIdParamOptional = Optional.empty();

        Matcher matcher = CASES_ID_PATH_PATTERN.matcher(request.getRequestURI());
        if (matcher.find()) {
            caseIdParamOptional = Optional.ofNullable(matcher.group(0));
            checkAuthorisationByCaseId(caseIdParamOptional, roles);
        }

        if (caseIdParamOptional.isEmpty()) {
            caseIdParamOptional = Optional.ofNullable(request.getParameter(CASE_ID_PARAM));
            checkAuthorisationByCaseId(caseIdParamOptional, roles);
        }

        if (caseIdParamOptional.isEmpty()) {
            caseIdParamOptional = Optional.ofNullable(request.getHeader(CASE_ID_PARAM));
            checkAuthorisationByCaseId(caseIdParamOptional, roles);
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
        Optional<String> hearingIdParamOptional = Optional.empty();

        Matcher matcher = HEARINGS_ID_PATH_PATTERN.matcher(request.getRequestURI());
        if (matcher.find()) {
            hearingIdParamOptional = Optional.ofNullable(matcher.group(0));
            checkAuthorisationByHearingId(hearingIdParamOptional, roles);
        }

        if (hearingIdParamOptional.isEmpty()) {
            hearingIdParamOptional = Optional.ofNullable(request.getParameter(HEARING_ID_PARAM));
            checkAuthorisationByHearingId(hearingIdParamOptional, roles);
        }

        if (hearingIdParamOptional.isEmpty()) {
            hearingIdParamOptional = Optional.ofNullable(request.getHeader(HEARING_ID_PARAM));
            checkAuthorisationByHearingId(hearingIdParamOptional, roles);
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
        Optional<String> mediaRequestIdParamOptional = Optional.empty();

        Matcher matcher = AUDIO_REQUESTS_ID_PATH_PATTERN.matcher(request.getRequestURI());
        if (matcher.find()) {
            mediaRequestIdParamOptional = Optional.ofNullable(matcher.group(0));
            checkAuthorisationByMediaRequestId(mediaRequestIdParamOptional, roles);
        }

        if (mediaRequestIdParamOptional.isEmpty()) {
            mediaRequestIdParamOptional = Optional.ofNullable(request.getParameter(AUDIO_REQUEST_ID_PARAM));
            checkAuthorisationByMediaRequestId(mediaRequestIdParamOptional, roles);
        }

        if (mediaRequestIdParamOptional.isEmpty()) {
            mediaRequestIdParamOptional = Optional.ofNullable(request.getHeader(AUDIO_REQUEST_ID_PARAM));
            checkAuthorisationByMediaRequestId(mediaRequestIdParamOptional, roles);
        }
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
        Optional<String> mediaIdParamOptional = Optional.empty();

        Matcher matcher = AUDIOS_ID_PATH_PATTERN.matcher(request.getRequestURI());
        if (matcher.find()) {
            mediaIdParamOptional = Optional.ofNullable(matcher.group(0));
            checkAuthorisationByMediaId(mediaIdParamOptional, roles);
        }

        if (mediaIdParamOptional.isEmpty()) {
            mediaIdParamOptional = Optional.ofNullable(request.getParameter(AUDIO_ID_PARAM));
            checkAuthorisationByMediaId(mediaIdParamOptional, roles);
        }

        if (mediaIdParamOptional.isEmpty()) {
            mediaIdParamOptional = Optional.ofNullable(request.getHeader(AUDIO_ID_PARAM));
            checkAuthorisationByMediaId(mediaIdParamOptional, roles);
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
        Optional<String> transcriptionIdParamOptional = Optional.empty();

        Matcher matcher = TRANSCRIPTIONS_ID_PATH_PATTERN.matcher(request.getRequestURI());
        if (matcher.find()) {
            transcriptionIdParamOptional = Optional.ofNullable(matcher.group(0));
            checkAuthorisationByTranscriptionId(transcriptionIdParamOptional, roles);
        }

        if (transcriptionIdParamOptional.isEmpty()) {
            transcriptionIdParamOptional = Optional.ofNullable(request.getParameter(TRANSCRIPTION_ID_PARAM));
            checkAuthorisationByTranscriptionId(transcriptionIdParamOptional, roles);
        }

        if (transcriptionIdParamOptional.isEmpty()) {
            transcriptionIdParamOptional = Optional.ofNullable(request.getHeader(TRANSCRIPTION_ID_PARAM));
            checkAuthorisationByTranscriptionId(transcriptionIdParamOptional, roles);
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

}
