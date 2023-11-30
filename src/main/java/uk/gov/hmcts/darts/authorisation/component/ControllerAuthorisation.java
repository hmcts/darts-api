package uk.gov.hmcts.darts.authorisation.component;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public interface ControllerAuthorisation {

    ContextIdEnum getContextId();

    void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles);

    void checkAuthorisation(Supplier<Optional<String>> idToAuthorise, Set<SecurityRoleEnum> roles);

    void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles);

}
