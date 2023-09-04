package uk.gov.hmcts.darts.authentication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

@Configuration
public class AuthenticationConfiguration {

    @Bean
    public AuthConfigFallback getNoAuthConfigurationFallback(DefaultAuthConfigurationPropertiesStrategy strategy) {
        return (req) -> {
            throw new DartsApiException(AuthenticationError.FAILED_TO_OBTAIN_AUTHENTICATION_CONFIG);
        };
    }
}
