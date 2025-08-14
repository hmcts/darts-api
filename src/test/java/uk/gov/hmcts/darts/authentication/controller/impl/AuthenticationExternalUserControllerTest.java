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
import uk.gov.hmcts.darts.authentication.model.TokenResponse;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.ReplaceJavaUtilDate")
class AuthenticationExternalUserControllerTest {

    private static final URI DUMMY_AUTHORIZATION_URI = URI.create("https://www.example.com/authorization?param=value");
    private static final URI DUMMY_LOGOUT_URI = URI.create("https://www.example.com/logout?param=value");
    private static final String DUMMY_CODE = "code";
    private static final String DUMMY_TOKEN = "token";
    private static final String TEST_USER_EMAIL = "test.user@example.com";

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
    void loginAndRefresh_ShouldReturnLoginPageAsRedirectWhenAuthHeaderIsNotSet() {
        when(authenticationService.loginOrRefresh(null, null))
            .thenReturn(DUMMY_AUTHORIZATION_URI);

        ModelAndView modelAndView = controller.loginOrRefresh(null, null);

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/authorization?param=value", modelAndView.getViewName());
    }

    @Test
    void handleOauthCode_FromAzureWhenCodeIsReturnedWithAccessTokenAndUserState() throws JOSEException {
        final String emailAddress = TEST_USER_EMAIL;
        when(authenticationService.handleOauthCode(anyString(), isNull()))
            .thenReturn(new TokenResponse(createDummyAccessToken(List.of(emailAddress)), null));
        when(locator.locateAuthenticationConfiguration()).thenReturn(new ExternalAuthConfigurationPropertiesStrategy(
            externalAuthConfigurationProperties, new ExternalAuthProviderConfigurationProperties()));
        when(externalAuthConfigurationProperties.getClaims()).thenReturn("emails");

        when(authorisationApi.getAuthorisation(anyString())).thenReturn(
            Optional.ofNullable(UserState.builder()
                                    .userId(-1)
                                    .isActive(true)
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

        SecurityToken securityToken = controller.handleOauthCode(DUMMY_CODE, null);
        assertNotNull(securityToken);
        assertNotNull(securityToken.getAccessToken());
        assertNotNull(securityToken.getUserState());

        verify(authenticationService).handleOauthCode(DUMMY_CODE, null);
        verify(authorisationApi).getAuthorisation(emailAddress);
        verify(userAccountService).updateLastLoginTime(-1);
        verifyNoMoreInteractions(authenticationService, authorisationApi, userAccountService);
    }

    @Test
    void handleOauthCode_FromAzureWhenCodeIsReturnedWithAccessTokenAndNoUserState() throws JOSEException {
        String accessToken = createDummyAccessToken(List.of("test.missing@example.com"));
        when(authenticationService.handleOauthCode(anyString(), isNull()))
            .thenReturn(new TokenResponse(accessToken, null));

        when(authorisationApi.getAuthorisation(anyString())).thenReturn(Optional.empty());
        when(locator.locateAuthenticationConfiguration()).thenReturn(new ExternalAuthConfigurationPropertiesStrategy(
            externalAuthConfigurationProperties, new ExternalAuthProviderConfigurationProperties()));
        when(externalAuthConfigurationProperties.getClaims()).thenReturn("emails");

        SecurityToken securityToken = controller.handleOauthCode(DUMMY_CODE, null);
        assertNotNull(securityToken);
        assertNotNull(securityToken.getAccessToken());
        assertNull(securityToken.getUserState());

        verify(authenticationService).handleOauthCode(DUMMY_CODE, null);
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
    void resetPassword_ShouldReturnResetPageAsRedirect() {
        when(authenticationService.resetPassword(any()))
            .thenReturn(DUMMY_AUTHORIZATION_URI);

        ModelAndView modelAndView = controller.resetPassword(null);

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/authorization?param=value", modelAndView.getViewName());
    }

    @Test
    void handleOauthCode_FromAzureWhenCodeIsReturnedWithNullClaim() throws JOSEException {
        when(authenticationService.handleOauthCode(anyString(), isNull()))
            .thenReturn(new TokenResponse(createDummyAccessToken(List.of(TEST_USER_EMAIL)), null));
        when(locator.locateAuthenticationConfiguration()).thenReturn(new ExternalAuthConfigurationPropertiesStrategy(
            externalAuthConfigurationProperties, new ExternalAuthProviderConfigurationProperties()));

        SecurityToken securityToken = controller.handleOauthCode(DUMMY_CODE, null);
        assertNotNull(securityToken);
        assertNotNull(securityToken.getAccessToken());
        assertNull(securityToken.getUserState());

        verify(authenticationService).handleOauthCode(DUMMY_CODE, null);
        verifyNoMoreInteractions(authenticationService, authorisationApi, userAccountService);
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturnedWithEmptyClaim() throws JOSEException {
        when(authenticationService.handleOauthCode(anyString(), isNull()))
            .thenReturn(new TokenResponse(createDummyAccessToken(new ArrayList<>()), null));
        when(locator.locateAuthenticationConfiguration()).thenReturn(new ExternalAuthConfigurationPropertiesStrategy(
            externalAuthConfigurationProperties, new ExternalAuthProviderConfigurationProperties()));
        when(externalAuthConfigurationProperties.getClaims()).thenReturn("emails");

        SecurityToken securityToken = controller.handleOauthCode(DUMMY_CODE, null);
        assertNotNull(securityToken);
        assertNotNull(securityToken.getAccessToken());
        assertNull(securityToken.getUserState());

        verify(authenticationService).handleOauthCode(DUMMY_CODE, null);
        verifyNoMoreInteractions(authenticationService, authorisationApi, userAccountService);
    }

    @Test
    void refreshAccessToken_ShouldReturnSecurityTokenWithUserState() throws JOSEException {
        // given
        String refreshToken = "dummyRefreshToken";
        String accessToken = createDummyAccessToken(List.of(TEST_USER_EMAIL));
        UserState userState = Optional.ofNullable(UserState.builder()
                                                      .userId(-1)
                                                      .isActive(true)
                                                      .userName("Test User")
                                                      .roles(Set.of(UserStateRole.builder()
                                                                        .roleId(TRANSCRIBER.getId())
                                                                        .roleName(TRANSCRIBER.toString())
                                                                        .globalAccess(false)
                                                                        .permissions(new HashSet<>())
                                                                        .build()))
                                                      .build()).get();

        when(authenticationService.refreshAccessToken(refreshToken)).thenReturn(accessToken);
        when(locator.locateAuthenticationConfiguration()).thenReturn(new ExternalAuthConfigurationPropertiesStrategy(
            externalAuthConfigurationProperties, new ExternalAuthProviderConfigurationProperties()));
        when(externalAuthConfigurationProperties.getClaims()).thenReturn("emails");
        when(authorisationApi.getAuthorisation(TEST_USER_EMAIL)).thenReturn(Optional.of(userState));

        // when
        SecurityToken securityToken = controller.refreshAccessToken(refreshToken);

        // then
        assertEquals(accessToken, securityToken.getAccessToken());
        assertEquals(refreshToken, securityToken.getRefreshToken());
        assertEquals(userState, securityToken.getUserState());
    }

    @Test
    void refreshAccessToken_ShouldReturnSecurityTokenWithoutUserStateWhenEmailNotPresent() throws JOSEException {
        // given
        String refreshToken = "dummyRefreshToken";
        String accessToken = createDummyAccessToken(List.of(TEST_USER_EMAIL));

        when(authenticationService.refreshAccessToken(refreshToken)).thenReturn(accessToken);
        when(locator.locateAuthenticationConfiguration()).thenReturn(new ExternalAuthConfigurationPropertiesStrategy(
            externalAuthConfigurationProperties, new ExternalAuthProviderConfigurationProperties()));
        when(externalAuthConfigurationProperties.getClaims()).thenReturn("emails");

        // when
        SecurityToken securityToken = controller.refreshAccessToken(refreshToken);

        // then
        assertEquals(accessToken, securityToken.getAccessToken());
        assertEquals(refreshToken, securityToken.getRefreshToken());
        assertEquals(null, securityToken.getUserState());
    }

    private String createDummyAccessToken(List<String> emails) throws JOSEException {
        RSAKey rsaKey = new RSAKeyGenerator(2_048)
            .keyID("123")
            .generate();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .claim("ver", "1.0")
            .issuer(String.format("https://<tenant-name>.b2clogin.com/%s/v2.0/", UUID.randomUUID().toString()))
            .subject(UUID.randomUUID().toString())
            .audience(UUID.randomUUID().toString())
            .expirationTime(new Date(1_690_973_493))
            .claim("nonce", "defaultNonce")
            .issueTime(new Date(1_690_969_893))
            .claim("auth_time", new Date(1_690_969_893))
            .claim("emails", emails)
            .claim("name", "Test User")
            .claim("given_name", "Test")
            .claim("family_name", "User")
            .claim("tfp", "policy_name")
            .claim("nbf", new Date(1_690_969_893))
            .build();

        SignedJWT signedJwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
            claimsSet
        );

        signedJwt.sign(new RSASSASigner(rsaKey));

        return signedJwt.serialize();
    }
}