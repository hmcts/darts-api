package uk.gov.hmcts.darts.dets.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;

@Configuration
@ConfigurationProperties(prefix = "darts.storage.dets")
@Getter
@Setter
public class DetsDataManagementConfiguration extends StorageConfiguration {
    private String containerName;
    private String sasEndpoint;
    private String detsManifestFilePrefix;
    private int deleteTimeout;
    private String armDropzoneSasEndpoint;
    private String armContainerName;

}