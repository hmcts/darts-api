package uk.gov.hmcts.darts.authentication.controller.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.hmcts.darts.authentication.config.AuthStrategySelector;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthConfigurationPropertiesStrategy;
import uk.gov.hmcts.darts.authentication.config.external.ExternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.model.SecurityToken;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.model.UserStateRole;
import uk.gov.hmcts.darts.common.service.UserAccountService;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class AuthenticationExternalUserControllerTest {

    private static final URI DUMMY_AUTHORIZATION_URI = URI.create("https://www.example.com/authorization?param=value");
    private static final URI DUMMY_LOGOUT_URI = URI.create("https://www.example.com/logout?param=value");
    private static final String DUMMY_CODE = "code";
    private static final String DUMMY_TOKEN = "token";

    @InjectMocks
    private AuthenticationExternalUserController controller;

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private AuthorisationApi authorisationApi;
    @Mock
    private AuthStrategySelector locator;
    @Mock
    private UserAccountService userAccountService;

    @Mock
    private ExternalAuthConfigurationProperties externalAuthConfigurationProperties;

    @Test
    void loginAndRefreshShouldReturnLoginPageAsRedirectWhenAuthHeaderIsNotSet() {
        when(authenticationService.loginOrRefresh(null, null))
            .thenReturn(DUMMY_AUTHORIZATION_URI);

        ModelAndView modelAndView = controller.loginOrRefresh(null, null);

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/authorization?param=value", modelAndView.getViewName());
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturnedWithAccessTokenAndUserState() throws JOSEException {
        final String emailAddress = "test.user@example.com";
        when(authenticationService.handleOauthCode(anyString()))
            .thenReturn(createDummyAccessToken(List.of(emailAddress)));
        when(locator.locateAuthenticationConfiguration()).thenReturn(new ExternalAuthConfigurationPropertiesStrategy(
            externalAuthConfigurationProperties, new ExternalAuthProviderConfigurationProperties()));
        when(externalAuthConfigurationProperties.getClaims()).thenReturn("emails");

        when(authorisationApi.getAuthorisation(anyString())).thenReturn(
            Optional.ofNullable(UserState.builder()
                                    .userId(-1)
                                    .userName("Test User")
                                    .roles(Set.of(UserStateRole.builder()
                                                      .roleId(TRANSCRIBER.getId())
                                                      .roleName(TRANSCRIBER.toString())
                                                      .globalAccess(false)
                                                      .permissions(new HashSet<>())
                                                      .build()))
                                    .build())
        );
        doNothing().when(userAccountService).updateLastLoginTime(-1);

        SecurityToken securityToken = controller.handleOauthCode(DUMMY_CODE);
        assertNotNull(securityToken);
        assertNotNull(securityToken.getAccessToken());
        assertNotNull(securityToken.getUserState());

        verify(authenticationService).handleOauthCode(DUMMY_CODE);
        verify(authorisationApi).getAuthorisation(emailAddress);
        verify(userAccountService).updateLastLoginTime(-1);
        verifyNoMoreInteractions(authenticationService, authorisationApi, userAccountService);
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturnedWithAccessTokenAndNoUserState() throws JOSEException {
        String accessToken = createDummyAccessToken(List.of("test.missing@example.com"));
        when(authenticationService.handleOauthCode(anyString()))
            .thenReturn(accessToken);

        when(authorisationApi.getAuthorisation(anyString())).thenReturn(Optional.empty());
        when(locator.locateAuthenticationConfiguration()).thenReturn(new ExternalAuthConfigurationPropertiesStrategy(
            externalAuthConfigurationProperties, new ExternalAuthProviderConfigurationProperties()));
        when(externalAuthConfigurationProperties.getClaims()).thenReturn("emails");

        SecurityToken securityToken = controller.handleOauthCode(DUMMY_CODE);
        assertNotNull(securityToken);
        assertNotNull(securityToken.getAccessToken());
        assertNull(securityToken.getUserState());

        verify(authenticationService).handleOauthCode(DUMMY_CODE);
        verify(authorisationApi).getAuthorisation("test.missing@example.com");
        verifyNoMoreInteractions(authenticationService, authorisationApi, userAccountService);
    }

    @Test
    void logoutShouldReturnLogoutPageUriAsRedirectWhenTokenExistsInSession() {
        when(authenticationService.logout(DUMMY_TOKEN, null))
            .thenReturn(DUMMY_LOGOUT_URI);

        ModelAndView modelAndView = controller.logout("Bearer " + DUMMY_TOKEN, null);

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/logout?param=value", modelAndView.getViewName());
    }

    @Test
    void resetPasswordShouldReturnResetPageAsRedirect() {
        when(authenticationService.resetPassword(any()))
            .thenReturn(DUMMY_AUTHORIZATION_URI);

        ModelAndView modelAndView = controller.resetPassword(null);

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/authorization?param=value", modelAndView.getViewName());
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturnedWithNullClaim() throws JOSEException {
        when(authenticationService.handleOauthCode(anyString()))
            .thenReturn(createDummyAccessToken(List.of("test.user@example.com")));
        when(locator.locateAuthenticationConfiguration()).thenReturn(new ExternalAuthConfigurationPropertiesStrategy(
            externalAuthConfigurationProperties, new ExternalAuthProviderConfigurationProperties()));

        SecurityToken securityToken = controller.handleOauthCode(DUMMY_CODE);
        assertNotNull(securityToken);
        assertNotNull(securityToken.getAccessToken());
        assertNull(securityToken.getUserState());

        verify(authenticationService).handleOauthCode(DUMMY_CODE);
        verifyNoMoreInteractions(authenticationService, authorisationApi, userAccountService);
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturnedWithEmptyClaim() throws JOSEException {
        when(authenticationService.handleOauthCode(anyString()))
            .thenReturn(createDummyAccessToken(new ArrayList<>()));
        when(locator.locateAuthenticationConfiguration()).thenReturn(new ExternalAuthConfigurationPropertiesStrategy(
            externalAuthConfigurationProperties, new ExternalAuthProviderConfigurationProperties()));
        when(externalAuthConfigurationProperties.getClaims()).thenReturn("emails");

        SecurityToken securityToken = controller.handleOauthCode(DUMMY_CODE);
        assertNotNull(securityToken);
        assertNotNull(securityToken.getAccessToken());
        assertNull(securityToken.getUserState());

        verify(authenticationService).handleOauthCode(DUMMY_CODE);
        verifyNoMoreInteractions(authenticationService, authorisationApi, userAccountService);
    }

    @SuppressWarnings("PMD.UseUnderscoresInNumericLiterals")
    private String createDummyAccessToken(List<String> emails) throws JOSEException {
        RSAKey rsaKey = new RSAKeyGenerator(2048)
            .keyID("123")
            .generate();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .claim("ver", "1.0")
            .issuer(String.format("https://<tenant-name>.b2clogin.com/%s/v2.0/", UUID.randomUUID().toString()))
            .subject(UUID.randomUUID().toString())
            .audience(UUID.randomUUID().toString())
            .expirationTime(new Date(1690973493))
            .claim("nonce", "defaultNonce")
            .issueTime(new Date(1690969893))
            .claim("auth_time", new Date(1690969893))
            .claim("emails", emails)
            .claim("name", "Test User")
            .claim("given_name", "Test")
            .claim("family_name", "User")
            .claim("tfp", "policy_name")
            .claim("nbf", new Date(1690969893))
            .build();

        SignedJWT signedJwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
            claimsSet
        );

        signedJwt.sign(new RSASSASigner(rsaKey));

        return signedJwt.serialize();
    }
}
