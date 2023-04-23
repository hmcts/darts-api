package uk.gov.hmcts.darts.authentication.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OAuthProviderRawResponse {

    @JsonProperty("id_token")
    private String accessToken;

    @JsonProperty("id_token_expires_in")
    private long expiresIn;
}
