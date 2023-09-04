package uk.gov.hmcts.darts.authentication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationException;

@Configuration
public class AuthenticationConfiguration {

    @Bean
    public AuthConfigFallback getDefaultNoAuthConfiguration() {
        return (req) -> {
            throw new AuthenticationException("No authentication configuration found");
        };
    }
}
