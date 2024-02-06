package uk.gov.hmcts.darts.authentication.config;

import jakarta.servlet.http.HttpServletRequest;

@FunctionalInterface
public interface AuthConfigFallback {

    AuthenticationConfigurationPropertiesStrategy getFallbackStrategy(HttpServletRequest request);
}
