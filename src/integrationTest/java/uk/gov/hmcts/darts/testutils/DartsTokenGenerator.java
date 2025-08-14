package uk.gov.hmcts.darts.testutils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import javax.validation.ValidationException;

@Builder
@Slf4j
public class DartsTokenGenerator {

    private String issuer;

    private String audience;

    private String email;

    private boolean useExpiredToken;

    private boolean useGlobalKey;

    private static final int SECONDS = 216_000;

    private static RSAKey globalKey;

    static {
        try {
            globalKey = new RSAKeyGenerator(RSAKeyGenerator.MIN_KEY_SIZE_BITS)
                .keyUse(KeyUse.SIGNATURE)
                .keyID("1234")
                .generate();
        } catch (JOSEException joseException) {
            log.error("Error creating global key" + joseException);
        }
    }

    /**
     * builds an official jwt token that can be used to test the darts api end to end.
     *
     * @return The token as well as the jwks key payload to validate the token.
     * @throws JOSEException Any problems fetching a token.
     */
    @SuppressWarnings("PMD.ReplaceJavaUtilDate")
    public DartsTokenAndJwksKey fetchToken() throws JOSEException {
        if (issuer == null || email == null || audience == null) {
            throw new ValidationException("Required inputs not supplied");
        }

        RSAKey key;
        if (useGlobalKey) {
            key = globalKey;
        } else {
            // generate an rsa signature
            key = new RSAKeyGenerator(RSAKeyGenerator.MIN_KEY_SIZE_BITS)
                .keyUse(KeyUse.SIGNATURE)
                .keyID("123")
                .generate();
        }

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(JOSEObjectType.JWT)
            .keyID(key.getKeyID())
            .build();

        // conditionally expire the token for test purposes
        Date expirationDate;
        if (useExpiredToken) {
            expirationDate = Date.from(Instant.now().minusSeconds(SECONDS));
        } else {
            expirationDate = Date.from(Instant.now().plusSeconds(SECONDS));
        }

        // setup our claims
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .audience(audience)
            .expirationTime(expirationDate)
            .claim("emails", List.of(email))
            .claim("iat", OffsetDateTime.now().toEpochSecond())
            .claim("sub", "subject")
            .build();

        // create a signed token using the private key
        SignedJWT signedJwt = new SignedJWT(header, claimsSet);
        signedJwt.sign(new RSASSASigner(key.toRSAPrivateKey()));

        // return the token and the jwks validating public key for this token
        DartsTokenAndJwksKey token = new DartsTokenAndJwksKey();
        token.setToken(signedJwt.serialize());
        token.setJwksKey(key.toPublicJWK().toJSONString());

        return token;
    }

    public static String getGlobalKey() {
        return globalKey.toPublicJWK().toJSONString();
    }

    public DartsTokenAndJwksKey fetchTokenWithGlobalUser() throws JOSEException {
        email = "darts.global.user@hmcts.net";
        return fetchToken();
    }
}