package uk.gov.hmcts.darts.testutils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DartsTokenAndJwksKey {
    private String token;
    private String jwksKey;
}