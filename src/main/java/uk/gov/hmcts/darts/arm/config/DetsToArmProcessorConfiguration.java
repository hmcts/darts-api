package uk.gov.hmcts.darts.arm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "darts.automated.task.dets-to-arm-batch-processor")
@Getter
@Setter
@Configuration
public class DetsToArmProcessorConfiguration {

    private int maxArmManifestItems;
    private String manifestFilePrefix;
}
