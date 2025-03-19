package uk.gov.hmcts.darts.authentication.model;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.darts.authorisation.model.UserState;

@Builder
@Value
public class SecurityToken {

    private String accessToken;
    private String refreshToken;
    private UserState userState;

}
