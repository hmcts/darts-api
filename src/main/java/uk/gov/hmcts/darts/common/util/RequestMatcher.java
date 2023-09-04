package uk.gov.hmcts.darts.common.util;

import jakarta.servlet.http.HttpServletRequest;
@FunctionalInterface
public interface RequestMatcher {

    String internalUrlMatcher = "/internal-user";

    String externalUrlMatcher = "/external-user";

    RequestMatcher URL_MAPPER_INTERNAL = (req) ->
    {
        if (req.getRequestURL().toString().contains(internalUrlMatcher)) {
            return true;
        }

        return false;
    };

     RequestMatcher URL_MAPPER_EXTERNAL = (req) ->
    {
        if (req.getRequestURL().toString().contains(externalUrlMatcher)) {
            return true;
        }

        return false;
    };

     boolean doesMatch(HttpServletRequest req);
}
