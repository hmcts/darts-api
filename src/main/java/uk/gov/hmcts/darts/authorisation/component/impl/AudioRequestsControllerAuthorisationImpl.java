package uk.gov.hmcts.darts.authorisation.component.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.HashSet;
import java.util.Set;

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.HEARING_ID_MEDIA_REQUEST;

@Component
@Slf4j
class AudioRequestsControllerAuthorisationImpl extends HearingIdControllerAuthorisationImpl {

    static final String HEARING_ID_PARAM = "hearing_id";

    private final Authorisation authorisation;

    public AudioRequestsControllerAuthorisationImpl(Authorisation authorisation, Authorisation authorisation1) {
        super(authorisation);
        this.authorisation = authorisation1;
    }

    @Override
    public ContextIdEnum getContextId() {
        return HEARING_ID_MEDIA_REQUEST;
    }

    @Override
    public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
        String requestType = jsonNode.path("request_type").textValue();
        if ("DOWNLOAD".equals(requestType)) {
            roles = new HashSet<>();
            roles.add(SecurityRoleEnum.TRANSCRIBER);
        }
        authorisation.authoriseByHearingId(jsonNode.path(HEARING_ID_PARAM).intValue(), roles);
    }
    
}
