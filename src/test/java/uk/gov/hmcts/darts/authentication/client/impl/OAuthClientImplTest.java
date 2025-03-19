package uk.gov.hmcts.darts.authentication.client.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.config.AuthProviderConfigurationProperties;

import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:membername")
@ExtendWith(MockitoExtension.class)
class OAuthClientImplTest {

    @Mock
    private AuthProviderConfigurationProperties providerConfigurationProperties;

    private OAuthClientImpl oAuthClientImpl;

    @BeforeEach
    void setUp() {
        oAuthClientImpl = new OAuthClientImpl();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void fetchAccessToken_ShouldThrowExceptionWhenRefreshTokenIsBlankOrNull(String refreshToken) {
        // when
        assertThrows(IllegalArgumentException.class,
                     () -> oAuthClientImpl.fetchAccessToken(providerConfigurationProperties, refreshToken, "CLIENT_ID", "CLIENT_SECRET", "SCOPE"));
    }

    @Test
    void fetchAccessToken_ShouldThrowExceptionWhenInvalidUri() {
        // given
        when(providerConfigurationProperties.getTokenUri()).thenReturn("http://invalid-uri");

        // when
        var exception = assertThrows(UnknownHostException.class,
                                     () -> oAuthClientImpl.fetchAccessToken(providerConfigurationProperties, "REFRESH_TOKEN", "CLIENT_ID", "CLIENT_SECRET",
                                                                            "SCOPE"));

        // then
        assertEquals("invalid-uri", exception.getMessage());
    }

}