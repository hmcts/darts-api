package uk.gov.hmcts.darts.authorisation.component.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Optional;

@Component
@NoArgsConstructor
abstract class BaseControllerAuthorisation {

    public static final String BAD_REQUEST_AUTHORISATION_PARAM_ERROR_MESSAGE =
        "Unable to extract the %s in request path, query or header params for this Authorisation endpoint: %s";

    Optional<String> getPathParamValue(HttpServletRequest request, String pathParam) {
        Map pathVariables = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String pathParamValue = (String) pathVariables.get(pathParam);
        return Optional.ofNullable(pathParamValue);
    }

}
