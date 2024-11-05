package uk.gov.hmcts.darts.arm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "darts.storage.arm-api.rpo-url")
@Getter
@Setter
@Validated
public class ArmRpoConfigurationProperties {

    private String getRecordManagementMatterPath;
    
}
