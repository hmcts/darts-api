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
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

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
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
//@AutoConfigureWireMock
@SuppressWarnings("PMD.ExcessiveImports")
class HandleOAuthCodeIntTest extends IntegrationBase {

    private static final String EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE =
        "/external-user/handle-oauth-code?code=abc";
    private static final String KEY_ID_VALUE = "dummy_key_id";
    private static final String CONFIGURED_ISSUER_VALUE = "dummy_issuer_uri";
    private static final String CONFIGURED_AUDIENCE_VALUE = "dummy_client_id";
    private static final String OAUTH_TOKEN_ENDPOINT = "/oauth2/v2.0/token";
    private static final String OAUTH_KEYS_ENDPOINT = "/discovery/v2.0/keys";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void handOAuthCodeShouldReturnAccessTokenWhenValidAuthTokenIsObtainedForProvidedAuthCode() throws Exception {
        KeyPair keyPair = setTokenStub();
        setKeyStoreStub(keyPair);

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post(EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        String token = response.getResponse().getContentAsString();
        assertThat(token).isNotNull();

    }

    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void handleOAuthCodeShouldReturnErrorResponseWhenDownstreamCallToAzureFails() throws Exception {
        stubFor(
            WireMock.post(OAUTH_TOKEN_ENDPOINT)
                .willReturn(
                    aResponse().withStatus(500)
                )
        );

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post(EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE))
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
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void handleOAuthCodeShouldReturnErrorResponseWhenTokenValidationFails() throws Exception {
        setTokenStub();

        MvcResult response = mockMvc.perform(MockMvcRequestBuilders.post(EXTERNAL_USER_HANDLE_OAUTH_CODE_ENDPOINT_WITH_CODE))
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
    private KeyPair setTokenStub() {
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
            .audience(CONFIGURED_AUDIENCE_VALUE)
            .issuer(CONFIGURED_ISSUER_VALUE)
            .expirationTime(createDateInFuture())
            .build();

        KeyPair keyPair = createKeys();

        String signedJwt = createSignedJwt(jwtClaimsSet, keyPair).serialize();

        stubFor(
            WireMock.post(OAUTH_TOKEN_ENDPOINT)
                .willReturn(
                    aResponse().withStatus(200).withBody("{\"id_token\":\"" + signedJwt + "\"}")
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
