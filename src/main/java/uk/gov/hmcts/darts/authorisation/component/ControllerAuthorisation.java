package uk.gov.hmcts.darts.authorisation.component;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Set;

public interface ControllerAuthorisation {

    ContextIdEnum getContextId();

    void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles);

    void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles);

}
