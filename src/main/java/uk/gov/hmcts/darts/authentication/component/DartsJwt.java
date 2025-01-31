package uk.gov.hmcts.darts.authentication.component;

import lombok.Getter;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;

@Getter
public class DartsJwt extends Jwt {

    private final Integer userId;

    public DartsJwt(Jwt jwt, Integer userId) {
        this(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getHeaders(), jwt.getClaims(), userId);
    }

    public DartsJwt(String tokenValue, Instant issuedAt, Instant expiresAt, Map<String, Object> headers,
                    Map<String, Object> claims,
                    Integer userId) {
        super(tokenValue, issuedAt, expiresAt, headers, claims);
        this.userId = userId;
    }
}
