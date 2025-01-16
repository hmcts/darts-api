package uk.gov.hmcts.darts.arm.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@Jacksonized
public class ArmTokenRequest {

    @JsonProperty("username")
    public String username;
    @JsonProperty("password")
    public String password;


}
