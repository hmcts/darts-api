package uk.gov.hmcts.darts.arm.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URL;

@ConfigurationProperties(prefix = "darts.storage.arm-api")
@Getter
@Setter
@Validated
public class ArmApiConfigurationProperties {

    @NotEmpty
    private String armUsername;
    @NotEmpty
    private String armPassword;
    @NotNull
    private URL url;
    @NotEmpty
    private String cabinetId;
    @NotEmpty
    private String armServiceProfile;
    @NotEmpty
    private String armServiceEntitlement;
    @NotEmpty
    private String armStorageAccountName;

}
