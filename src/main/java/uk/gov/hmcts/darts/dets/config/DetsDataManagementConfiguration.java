package uk.gov.hmcts.darts.dets.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.arm.config.ArmApiConfigurationProperties;

@Configuration
@EnableConfigurationProperties(ArmApiConfigurationProperties.class)
@ConfigurationProperties(prefix = "darts.storage.dets")
@Getter
@Setter
public class DetsDataManagementConfiguration {
    private String containerName;
    private String connectionString;
    private boolean fetchFromDetsEnabled;
}