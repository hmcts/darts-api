package uk.gov.hmcts.darts.authentication.controller.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.model.UserState;
import uk.gov.hmcts.darts.authorisation.model.UserStateRole;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@AutoConfigureMockMvc
class HandleOAuthCodeIntTest extends IntegrationBaseWithWiremock {

    private static final String EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE =
        "/external-user/handle-oauth-code?code=abc";
    private static final String EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE_AND_OVERRIDE =
        "/external-user/handle-oauth-code?code=abc&redirect_uri=https://darts-portal.com/auth/callback";
    private static final String KEY_ID_VALUE = "dummy_key_id";
    private static final String CONFIGURED_ISSUER_VALUE = "dummy_issuer_uri";
    private static final String CONFIGURED_AUDIENCE_VALUE = "dummy_client_id";
    private static final String OAUTH_TOKEN_ENDPOINT = "/oauth2/v2.0/token";
    private static final String OAUTH_KEYS_ENDPOINT = "/discovery/v2.0/keys";
    private static final String VALID_SUBJECT_VALUE = "VALID SUBJECT VALUE";
    private static final String VALID_EMAIL_VALUE = "test.user@example.com";
    private static final String EMAILS_CLAIM_NAME = "emails";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthorisationApi mockAuthorisationApi;

    @ParameterizedTest
    @ValueSource(strings = {EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE, EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE_AND_OVERRIDE})
    void handleOAuthCodeShouldReturnAccessTokenWhenValidAuthTokenIsObtainedForProvidedAuthCode(String endpoint) throws Exception {
        when(mockAuthorisationApi.getAuthorisation(VALID_EMAIL_VALUE))
            .thenReturn(Optional.ofNullable(UserState.builder()
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

        KeyPair keyPair = setTokenStub(List.of(VALID_EMAIL_VALUE));
        setKeyStoreStub(keyPair);

        mockMvc.perform(MockMvcRequestBuilders.post(endpoint))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.userState").exists());

        verify(mockAuthorisationApi).getAuthorisation(VALID_EMAIL_VALUE);

        verifySecurityTokenWithNoUserStateWhenNoUserAccount();
    }


    // DetachedTestCase - workaround for BadJWSException: Signed JWT rejected: Invalid signature
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")

    private void verifySecurityTokenWithNoUserStateWhenNoUserAccount() throws Exception {
        reset(mockAuthorisationApi);

        when(mockAuthorisationApi.getAuthorisation(VALID_EMAIL_VALUE))
            .thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.post(
                EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.userState").doesNotExist());

        verify(mockAuthorisationApi).getAuthorisation(VALID_EMAIL_VALUE);
    }

    @Test
    void handleOAuthCodeShouldReturnErrorResponseWhenAccessTokenMissingEmailsClaim() throws Exception {
        KeyPair keyPair = setTokenStub(Collections.emptyList());
        setKeyStoreStub(keyPair);

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post(
                EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE))
            .andExpect(status().isInternalServerError())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();

        String expectedResponseBody = """
            {
                "type":"AUTHENTICATION_101",
                "title":"Failed to validate access token",
                "status":500
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);

        verifyNoInteractions(mockAuthorisationApi);
    }

    @Test
    void handleOAuthCodeShouldReturnErrorResponseWhenDownstreamCallToAzureFails() throws Exception {
        stubFor(
            WireMock.post(OAUTH_TOKEN_ENDPOINT)
                .willReturn(
                    aResponse().withStatus(500)
                )
        );

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post(
                EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE))
            .andExpect(status().isInternalServerError())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();

        String expectedResponseBody = """
            {
                "type":"AUTHENTICATION_100",
                "title":"Failed to obtain access token",
                "status":500
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void handleOAuthCodeShouldReturnErrorResponseWhenTokenValidationFails() throws Exception {
        setTokenStub(List.of(VALID_EMAIL_VALUE));

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post(
                EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE))
            .andExpect(status().isInternalServerError())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();

        String expectedResponseBody = """
            {
                "type":"AUTHENTICATION_101",
                "title":"Failed to validate access token",
                "status":500
            }
            """;

        JSONAssert.assertEquals(expectedResponseBody, actualResponseBody, JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * To test the access token validation aspects of the /handle-oauth-code flow, we must ensure the Azure /token stub
     * provides a token whose signature can be verified against a public key provided by the configured Remote JWKS.
     * This setup method generates these private and public RSA keys and sets the stub responses accordingly.
     */
    private KeyPair setTokenStub(List<String> emails) {
        JWTClaimsSet.Builder claimBuilder = new JWTClaimsSet.Builder()
            .audience(CONFIGURED_AUDIENCE_VALUE)
            .issuer(CONFIGURED_ISSUER_VALUE)
            .expirationTime(createDateInFuture())
            .issueTime(Date.from(Instant.now()))
            .subject(VALID_SUBJECT_VALUE);

        if (!emails.isEmpty()) {
            claimBuilder = claimBuilder.claim(EMAILS_CLAIM_NAME, emails);
        }
        JWTClaimsSet jwtClaimsSet = claimBuilder.build();

        KeyPair keyPair = createKeys();

        String signedJwt = createSignedJwt(jwtClaimsSet, keyPair).serialize();

        stubFor(
            WireMock.post(OAUTH_TOKEN_ENDPOINT)
                .willReturn(
                    aResponse().withHeader("Content-Type", "application/json").withStatus(200).withBody(
                        "{\"id_token\":\"" + signedJwt + "\"}")
                )
        );

        return keyPair;
    }

    @SneakyThrows(JsonProcessingException.class)
    private void setKeyStoreStub(KeyPair keyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        Encoder encoder = Base64.getEncoder();
        JwksKey jwksKey = new JwksKey(
            "RSA",
            "sig",
            KEY_ID_VALUE,
            encoder.encodeToString(publicKey.getModulus().toByteArray()),
            encoder.encodeToString(publicKey.getPublicExponent().toByteArray()),
            CONFIGURED_ISSUER_VALUE
        );
        JwksKeySet jwksKeySet = new JwksKeySet(Collections.singletonList(jwksKey));

        String jwksKeySetJson = new ObjectMapper().writeValueAsString(jwksKeySet);

        stubFor(
            WireMock.get(OAUTH_KEYS_ENDPOINT)
                .willReturn(
                    aResponse().withStatus(200).withBody(jwksKeySetJson)
                )
        );
    }

    @SneakyThrows(NoSuchAlgorithmException.class)
    private KeyPair createKeys() {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        return generator.generateKeyPair();
    }

    @SneakyThrows(JOSEException.class)
    private SignedJWT createSignedJwt(JWTClaimsSet jwtClaimsSet, KeyPair keyPair) {
        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(KEY_ID_VALUE)
            .type(JOSEObjectType.JWT)
            .build();

        SignedJWT signedJwt = new SignedJWT(jwsHeader, jwtClaimsSet);

        RSASSASigner signer = new RSASSASigner(keyPair.getPrivate());
        signedJwt.sign(signer);

        return signedJwt;
    }

    private Date createDateInFuture() {
        return Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
    }

    private record JwksKeySet(@JsonProperty("keys") List<JwksKey> keys) {

    }

    private record JwksKey(@JsonProperty("kty") String algorithm,
                           @JsonProperty("use") String use,
                           @JsonProperty("kid") String keyId,
                           @JsonProperty("n") String modulus,
                           @JsonProperty("e") String exponent,
                           @JsonProperty("issuer") String issuer) {

    }

}