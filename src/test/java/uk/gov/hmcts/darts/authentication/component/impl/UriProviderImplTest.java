package uk.gov.hmcts.darts.authentication.component.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authentication.config.AuthenticationConfiguration;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.LinguisticNaming")
class UriProviderImplTest {

    @Mock
    private AuthenticationConfiguration authConfig;

    @InjectMocks
    private UriProviderImpl uriProvider;

    @Test
    void getAuthorizationUriShouldReturnExpectedUri() {
        mockStubsForAuthorization();

        URI authUrl = uriProvider.getAuthorizationUri();

        assertEquals("AuthUrl?client_id=ClientId&response_type=ResponseType&redirect_uri=RedirectId"
                         + "&response_mode=ResponseMode&scope=Scope&prompt=Prompt",
                     authUrl.toString());
    }

    @Test
    void getLandingPageUriShouldReturnExpectedUri() {
        URI landingPageUri = uriProvider.getLandingPageUri();

        assertEquals("/", landingPageUri.toString());
    }

    @Test
    void getLogoutPageUriShouldReturnExpectedUri() {
        URI logoutPageUri = uriProvider.getLogoutPageUri();

        assertEquals("/", logoutPageUri.toString());
    }

    private void mockStubsForAuthorization() {
        when(authConfig.getExternalADauthorizationUri()).thenReturn("AuthUrl");
        when(authConfig.getExternalADclientId()).thenReturn("ClientId");
        when(authConfig.getExternalADresponseType()).thenReturn("ResponseType");
        when(authConfig.getExternalADredirectUri()).thenReturn("RedirectId");
        when(authConfig.getExternalADresponseMode()).thenReturn("ResponseMode");
        when(authConfig.getExternalADscope()).thenReturn("Scope");
        when(authConfig.getExternalADprompt()).thenReturn("Prompt");
    }

}
