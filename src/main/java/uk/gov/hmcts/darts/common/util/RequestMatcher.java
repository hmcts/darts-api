package uk.gov.hmcts.darts.common.util;

import jakarta.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface RequestMatcher {

    String INTERNAL_URL_MATCHER = "/internal-user";

    String EXTERNAL_URL_MATCHER = "/external-user";

    RequestMatcher URL_MAPPER_INTERNAL = (req) -> req.getRequestURL().toString().contains(INTERNAL_URL_MATCHER);


    RequestMatcher URL_MAPPER_EXTERNAL = (req) -> req.getRequestURL().toString().contains(EXTERNAL_URL_MATCHER);

    boolean doesMatch(HttpServletRequest req);
}
