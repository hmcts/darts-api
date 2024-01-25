package uk.gov.hmcts.darts.arm.client.model;

import org.apache.commons.lang3.Validate;

public record ArmTokenRequest(String username, String password, String grantType) {

    public ArmTokenRequest {
        Validate.notBlank(username, "username must not be blank");
        Validate.notBlank(password, "password must not be blank");
        Validate.notBlank(grantType, "grant type must not be blank");
    }

}
