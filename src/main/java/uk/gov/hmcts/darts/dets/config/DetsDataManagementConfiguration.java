package uk.gov.hmcts.darts.dets.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;

@Configuration
@EnableConfigurationProperties(ArmApiConfigurationProperties.class)
@ConfigurationProperties(prefix = "darts.storage.dets")
@Getter
@Setter
public class DetsDataManagementConfiguration extends StorageConfiguration {
    private String containerName;
    private String connectionString;
    private boolean fetchFromDetsEnabled;
}