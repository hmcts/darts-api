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

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.DOWNLOAD_HEARING_ID_TRANSCRIBER;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.AvoidReassigningParameters"})
class AudioRequestsControllerAuthorisationImpl extends BaseControllerAuthorisation
    implements ControllerAuthorisation {

    static final String HEARING_ID_PARAM = "hearing_id";
    public static final String REQUEST_TYPE = "request_type";
    public static final String DOWNLOAD_REQUEST_TYPE = "DOWNLOAD";

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
            roles = setDownloadRequestSecurityRoles(roles, requestTypeParamOptional.get());
        }

        hearingIdControllerAuthorisation.checkAuthorisation(request, roles);
    }

    @Override
    public void checkAuthorisation(Supplier<Optional<String>> idToAuthorise, Set<SecurityRoleEnum> roles) {
        Optional<String> optionalId = idToAuthorise.get();
        if (optionalId.isPresent()) {
            roles = setDownloadRequestSecurityRoles(roles, optionalId.get());
        }

        hearingIdControllerAuthorisation.checkAuthorisation(idToAuthorise, roles);
    }

    @Override
    public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
        String requestType = jsonNode.path(REQUEST_TYPE).textValue();
        roles = setDownloadRequestSecurityRoles(roles, requestType);
        authorisation.authoriseByHearingId(jsonNode.path(HEARING_ID_PARAM).intValue(), roles);
    }

    private static Set<SecurityRoleEnum> setDownloadRequestSecurityRoles(Set<SecurityRoleEnum> roles, String requestType) {
        if (DOWNLOAD_REQUEST_TYPE.equals(requestType)) {
            roles = EnumSet.noneOf(SecurityRoleEnum.class);
            roles.add(SecurityRoleEnum.TRANSCRIBER);
        }
        return roles;
    }

}