package uk.gov.hmcts.darts.authentication.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.exception.AuthenticationException;

import java.io.Closeable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthProviderConfigurationPropertiesTest {

    @Test
    void getJwkSource_shouldCreateSource_whenJwkSetUriIsValid() throws Exception {
        var properties = new ExternalAuthProviderConfigurationProperties();
        properties.setJwkSetUri("https://example.com/.well-known/jwks.json");

        var source = properties.getJwkSource();
        assertThat(source).isNotNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"relative/path", "https://example.com/invalid path", "unsupported://example.com/jwks.json"})
    void getJwkSource_shouldThrowAuthenticationException_whenJwkSetUriIsInvalid(String jwkSetUri) {
        var properties = new ExternalAuthProviderConfigurationProperties();
        properties.setJwkSetUri(jwkSetUri);

        assertThatThrownBy(properties::getJwkSource)
            .isInstanceOf(AuthenticationException.class)
            .hasMessage("Sorry authentication jwks URL is incorrect");
    }
}
