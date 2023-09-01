package uk.gov.hmcts.darts.authentication.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfiguration;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class AuthConfigurationLocator {

    private final List<AuthConfiguration<?>> configMatchers;

    private final HttpServletRequest request;

    private final ExternalAuthConfiguration externalConfiguration;

    public Function<HttpServletRequest, AuthConfiguration<?>> getDefaultExternalConfig()
    {
        return e -> externalConfiguration;
    }

    public AuthConfiguration<?> locateAuthenticationConfiguration(Function<HttpServletRequest, AuthConfiguration<?>> defaultConfig)
    {
        Optional<AuthConfiguration<?>> configuration
            = configMatchers.stream().filter(e -> e.doesMatch(request)).findFirst();

        if (configuration.isPresent())
        {
            return defaultConfig.apply(request);
        }

        return configuration.get();
    }

    public AuthConfiguration<?> locateAuthenticationConfigurationWithExternalDefault()
    {
        return locateAuthenticationConfiguration(getDefaultExternalConfig());
    }
}
