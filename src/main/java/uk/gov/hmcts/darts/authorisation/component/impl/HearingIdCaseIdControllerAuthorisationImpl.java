package uk.gov.hmcts.darts.authorisation.component.impl;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.ControllerAuthorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID_OR_CASE_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_CASE_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_HEARING_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class HearingIdCaseIdControllerAuthorisationImpl extends BaseControllerAuthorisation
    implements ControllerAuthorisation {

    static final String HEARING_ID_PARAM = "hearing_id";
    static final String CASE_ID_PARAM = "case_id";

    private final Authorisation authorisation;

    @Override
    public ContextIdEnum getContextId() {
        return HEARING_ID_OR_CASE_ID;
    }

    @Override
    public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> hearingIdParamOptional = getPathParamValue(request, HEARING_ID_PARAM);

        if (hearingIdParamOptional.isEmpty()) {
            hearingIdParamOptional = Optional.ofNullable(request.getParameter(HEARING_ID_PARAM));
        } else if (hearingIdParamOptional.isEmpty()) {
            hearingIdParamOptional = Optional.ofNullable(request.getHeader(HEARING_ID_PARAM));
        }

        Optional<String> caseIdParamOptional = getPathParamValue(request, CASE_ID_PARAM);

        if (caseIdParamOptional.isEmpty()) {
            caseIdParamOptional = Optional.ofNullable(request.getParameter(CASE_ID_PARAM));
        } else if (caseIdParamOptional.isEmpty()) {
            caseIdParamOptional = Optional.ofNullable(request.getHeader(CASE_ID_PARAM));
        }

        if (hearingIdParamOptional.isEmpty() && caseIdParamOptional.isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                HEARING_ID_PARAM,
                request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_HEARING_ID);
        }
        if (!hearingIdParamOptional.isEmpty()) {
            checkAuthorisationByHearingId(hearingIdParamOptional, roles);
        } else if (!caseIdParamOptional.isEmpty()) {
            checkAuthorisationByCaseId(caseIdParamOptional, roles);
        }
    }

    @Override
    public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
        authorisation.authoriseByHearingIdOrCaseId(jsonNode.path(HEARING_ID_PARAM).intValue(),
                                                   jsonNode.path(CASE_ID_PARAM).intValue(),
                                                   roles);
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
}
