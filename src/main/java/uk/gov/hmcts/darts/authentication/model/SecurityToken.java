package uk.gov.hmcts.darts.authentication.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.darts.authorisation.model.UserState;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class SecurityToken {

    private String accessToken;
    private UserState userState;

}
