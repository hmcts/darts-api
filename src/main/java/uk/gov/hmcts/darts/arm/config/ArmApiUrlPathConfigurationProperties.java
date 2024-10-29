package uk.gov.hmcts.darts.arm.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "darts.storage.arm-api.api-url")
@Getter
@Setter
@Validated
public class ArmApiUrlPathConfigurationProperties {

    @NotEmpty
    private String updateMetadataPath;
    @NotEmpty
    private String downloadDataPath;

}
