package uk.gov.hmcts.darts.arm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;

@Configuration
@EnableConfigurationProperties(ArmApiConfigurationProperties.class)
@ConfigurationProperties(prefix = "darts.storage.arm.batch-response-cleanup")
@Getter
@Setter
public class ArmBatchCleanupConfiguration extends StorageConfiguration {


    private Integer bufferMinutes;
    private String manifestFileSuffix;

}