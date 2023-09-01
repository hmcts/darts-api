package uk.gov.hmcts.darts.common.util;

import javax.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface RequestMatcher {

    String urlMatcher = "/internal-user";
    RequestMatcher URL_MAPPER_INTERNAL = (req) ->
    {
        if (req.getRequestURL().toString().contains(urlMatcher)) {
            return true;
        }

        return false;
    };

     RequestMatcher URL_MAPPER_EXTERNAL = (req) ->
    {
        if (!req.getRequestURL().toString().contains(urlMatcher)) {
            return true;
        }

        return false;
    };

     boolean doesMatch(HttpServletRequest req);
}
