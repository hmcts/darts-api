package uk.gov.hmcts.darts.arm.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "darts.storage.arm-api.authentication-url")
@Getter
@Setter
@Validated
public class ArmApiAuthenticationUrlConfigurationProperties {

    @NotEmpty
    private String availableEntitlementProfilesPath;
    @NotEmpty
    private String selectEntitlementProfilePath;

}
