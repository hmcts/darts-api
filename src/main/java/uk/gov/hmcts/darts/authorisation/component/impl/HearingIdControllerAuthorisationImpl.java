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

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.BAD_REQUEST_HEARING_ID;

@Component
@RequiredArgsConstructor
@Slf4j
class HearingIdControllerAuthorisationImpl extends BaseControllerAuthorisation
    implements ControllerAuthorisation {

    static final String HEARING_ID_PARAM = "hearing_id";

    private final Authorisation authorisation;

    @Override
    public ContextIdEnum getContextId() {
        return HEARING_ID;
    }

    public String getEntityIdParam() {
        return HEARING_ID_PARAM;
    }

    @Override
    public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> hearingIdParamOptional = getEntityParamOptional(request, HEARING_ID_PARAM);
        checkAuthorisationByHearingId(hearingIdParamOptional, roles);

        if (hearingIdParamOptional.isEmpty()) {
            log.error(String.format(
                BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE,
                HEARING_ID_PARAM,
                request.getRequestURI()
            ));
            throw new DartsApiException(BAD_REQUEST_HEARING_ID);
        }
    }

    @Override
    public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
        authorisation.authoriseByHearingId(jsonNode.path(HEARING_ID_PARAM).intValue(), roles);
    }

    void checkAuthorisationByHearingId(Optional<String> hearingIdParamOptional, Set<SecurityRoleEnum> roles) {
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

}
