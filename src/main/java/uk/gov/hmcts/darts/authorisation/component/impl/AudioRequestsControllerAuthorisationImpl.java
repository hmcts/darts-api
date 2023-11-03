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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.DOWNLOAD_HEARING_ID_TRANSCRIBER;

@Component
@RequiredArgsConstructor
@Slf4j
class AudioRequestsControllerAuthorisationImpl extends BaseControllerAuthorisation
    implements ControllerAuthorisation {

    static final String HEARING_ID_PARAM = "hearing_id";
    public static final String REQUEST_TYPE = "request_type";

    private final Authorisation authorisation;

    private final HearingIdControllerAuthorisationImpl hearingIdControllerAuthorisation;

    @Override
    public ContextIdEnum getContextId() {
        return DOWNLOAD_HEARING_ID_TRANSCRIBER;
    }

    @Override
    public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
        Optional<String> requestTypeParamOptional = getEntityParamOptional(request, REQUEST_TYPE);
        if (requestTypeParamOptional.isPresent()) {
            roles = new HashSet<>();
            roles.add(SecurityRoleEnum.TRANSCRIBER);
        }

        hearingIdControllerAuthorisation.checkAuthorisation(request, roles);
    }

    @Override
    public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
        String requestType = jsonNode.path(REQUEST_TYPE).textValue();
        if ("DOWNLOAD".equals(requestType)) {
            roles = new HashSet<>();
            roles.add(SecurityRoleEnum.TRANSCRIBER);
        }
        authorisation.authoriseByHearingId(jsonNode.path(HEARING_ID_PARAM).intValue(), roles);
    }

}
