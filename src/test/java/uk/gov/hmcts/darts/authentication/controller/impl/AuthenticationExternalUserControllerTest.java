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
import uk.gov.hmcts.darts.authentication.model.SecurityToken;
import uk.gov.hmcts.darts.authentication.service.AuthenticationService;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.model.Role;
import uk.gov.hmcts.darts.authorisation.model.UserState;

import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.entity.SecurityRoleEnum.TRANSCRIPTION_COMPANY;

@ExtendWith(MockitoExtension.class)
class AuthenticationExternalUserControllerTest {

    private static final URI DUMMY_AUTHORIZATION_URI = URI.create("https://www.example.com/authorization?param=value");
    private static final URI DUMMY_LOGOUT_URI = URI.create("https://www.example.com/logout?param=value");

    @InjectMocks
    private AuthenticationExternalUserController controller;

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private AuthorisationApi authorisationApi;

    @Test
    void loginAndRefreshShouldReturnLoginPageAsRedirectWhenAuthHeaderIsNotSet() {
        when(authenticationService.loginOrRefresh(null))
            .thenReturn(DUMMY_AUTHORIZATION_URI);

        ModelAndView modelAndView = controller.loginOrRefresh(null);

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/authorization?param=value", modelAndView.getViewName());
    }

    @Test
    void handleOauthCodeFromAzureWhenCodeIsReturned() throws JOSEException {
        when(authenticationService.handleOauthCode(anyString()))
            .thenReturn(createDummyAccessToken());

        when(authorisationApi.getAuthorisation(anyString())).thenReturn(
            UserState.builder()
                .userId(-1)
                .userName("Test User")
                .roles(Set.of(Role.builder()
                                  .roleId(TRANSCRIPTION_COMPANY.getId())
                                  .roleName(TRANSCRIPTION_COMPANY.toString())
                                  .permissions(new HashSet<>())
                                  .build()))
                .build()
        );

        SecurityToken securityToken = controller.handleOauthCode("code");
        assertNotNull(securityToken);
        assertNotNull(securityToken.getAccessToken());
        assertNotNull(securityToken.getUserState());

        verify(authenticationService).handleOauthCode("code");
        verify(authorisationApi).getAuthorisation("test.user@example.com");
    }

    @Test
    void logoutShouldReturnLogoutPageUriAsRedirectWhenTokenExistsInSession() {
        when(authenticationService.logout(any()))
            .thenReturn(DUMMY_LOGOUT_URI);

        ModelAndView modelAndView = controller.logout(anyString());

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/logout?param=value", modelAndView.getViewName());
    }

    @Test
    void resetPasswordShouldReturnResetPageAsRedirect() {
        when(authenticationService.resetPassword())
            .thenReturn(DUMMY_AUTHORIZATION_URI);

        ModelAndView modelAndView = controller.resetPassword();

        assertNotNull(modelAndView);
        assertEquals("redirect:https://www.example.com/authorization?param=value", modelAndView.getViewName());
    }

    @SuppressWarnings("PMD.UseUnderscoresInNumericLiterals")
    private String createDummyAccessToken() throws JOSEException {
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
            .claim("emails", List.of("test.user@example.com"))
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
