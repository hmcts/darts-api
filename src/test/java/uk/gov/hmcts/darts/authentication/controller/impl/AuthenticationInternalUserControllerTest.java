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
import uk.gov.hmcts.darts.authentication.config.internal.InternalAuthConfigurationProperties;
import uk.gov.hmcts.darts.authentication.config.internal.InternalAuthConfigurationPropertiesStrategy;
import uk.gov.hmcts.darts.authentication.config.internal.InternalAuthProviderConfigurationProperties;
import uk.gov.hmcts.darts.authentication.model.SecurityToken;
import uk.gov.hmcts.darts.authentication.model.TokenResponse;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.model.UserStateRole;
import uk.gov.hmcts.darts.common.service.UserAccountService;

import java.net.URI;
import java.util.Date;
import java.util.HashSet;
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
class AuthenticationInternalUserControllerTest {

    private static final URI DUMMY_AUTHORIZATION_URI = URI.create("https://www.example.com/authorization?param=value");
    private static final URI DUMMY_LOGOUT_URI = URI.create("https://www.example.com/logout?param=value");
    private static final String DUMMY_CODE = "code";
    private static final String DUMMY_TOKEN = "token";

    @InjectMocks
    private AuthenticationInternalUserController controller;

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private AuthorisationApi authorisationApi;
    @Mock
    private AuthStrategySelector locator;
    @Mock
    private UserAccountService userAccountService;
    @Mock
    private InternalAuthConfigurationProperties internalAuthConfigurationProperties;

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
        when(authenticationService.handleOauthCode(anyString(), isNull()))
            .thenReturn(new TokenResponse(createDummyAccessToken(emailAddress), null));
        when(locator.locateAuthenticationConfiguration()).thenReturn(new InternalAuthConfigurationPropertiesStrategy(
            internalAuthConfigurationProperties, new InternalAuthProviderConfigurationProperties()));
        when(internalAuthConfigurationProperties.getClaims()).thenReturn("preferred_username");

        when(authorisationApi.getAuthorisation(anyString())).thenReturn(
            Optional.ofNullable(UserState.builder()
                                    .userId(-1)
                                    .userName("Test User")
                                    .isActive(true)
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
    void handleOauthCodeFromAzureWhenCodeIsReturnedWithAccessTokenAndNoUserState() throws JOSEException {
        final String emailAddress = "test.missing@example.com";
        when(authenticationService.handleOauthCode(anyString(), isNull()))
            .thenReturn(new TokenResponse(createDummyAccessToken(emailAddress), null));
        when(locator.locateAuthenticationConfiguration()).thenReturn(new InternalAuthConfigurationPropertiesStrategy(
            internalAuthConfigurationProperties, new InternalAuthProviderConfigurationProperties()));
        when(internalAuthConfigurationProperties.getClaims()).thenReturn("preferred_username");

        SecurityToken securityToken = controller.handleOauthCode(DUMMY_CODE, null);
        assertNotNull(securityToken);
        assertNotNull(securityToken.getAccessToken());
        assertNull(securityToken.getUserState());

        verify(authenticationService).handleOauthCode(DUMMY_CODE, null);
        verify(authorisationApi).getAuthorisation(emailAddress);
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
    void handleOauthCodeFromAzureWhenCodeIsReturnedWithoutClaim() throws JOSEException {
        when(authenticationService.handleOauthCode(anyString(), isNull()))
            .thenReturn(new TokenResponse(createDummyAccessToken("test.missing@example.com"), null));
        when(locator.locateAuthenticationConfiguration()).thenReturn(new InternalAuthConfigurationPropertiesStrategy(
            internalAuthConfigurationProperties, new InternalAuthProviderConfigurationProperties()));

        SecurityToken securityToken = controller.handleOauthCode(DUMMY_CODE, null);
        assertNotNull(securityToken);
        assertNotNull(securityToken.getAccessToken());
        assertNull(securityToken.getUserState());

        verify(authenticationService).handleOauthCode(DUMMY_CODE, null);
        verifyNoMoreInteractions(authenticationService, authorisationApi, userAccountService);
    }

    @SuppressWarnings("PMD.UseUnderscoresInNumericLiterals")
    private String createDummyAccessToken(String emails) throws JOSEException {
        RSAKey rsaKey = new RSAKeyGenerator(2048)
            .keyID("123")
            .generate();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .claim("ver", "1.0")
            .issuer(String.format("https://<tenant-name>.b2clogin.com/%s/v2.0/", UUID.randomUUID()))
            .subject(UUID.randomUUID().toString())
            .audience(UUID.randomUUID().toString())
            .expirationTime(new Date(1690973493))
            .claim("nonce", "defaultNonce")
            .issueTime(new Date(1690969893))
            .claim("auth_time", new Date(1690969893))
            .claim("preferred_username", emails)
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