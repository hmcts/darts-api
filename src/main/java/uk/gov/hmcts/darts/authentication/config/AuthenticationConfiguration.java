package uk.gov.hmcts.darts.authentication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationError;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.Optional;

@Configuration
public class AuthenticationConfiguration {

    @Bean
    public AuthConfigFallback getNoAuthConfigurationFallback(DefaultAuthConfigurationPropertiesStrategy strategy) {
        return (req) -> {
            throw new DartsApiException(AuthenticationError.FAILED_TO_OBTAIN_AUTHENTICATION_CONFIG);
        };
    }

    @Bean
    public AuditorAware<UserAccountEntity> auditorAware(UserIdentity userIdentity) {
        return () -> {
            try {
                return Optional.ofNullable(userIdentity.getUserAccount());
            } catch (Exception e) {
                return Optional.empty();
            }
        };
    }
}
