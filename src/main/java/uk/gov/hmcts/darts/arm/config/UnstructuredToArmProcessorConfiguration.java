package uk.gov.hmcts.darts.arm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import uk.gov.hmcts.darts.task.config.AsyncTaskConfig;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "darts.automated.task.unstructured-to-arm-batch-processor")
@Getter
@Setter
@Configuration
public class UnstructuredToArmProcessorConfiguration implements AsyncTaskConfig {

    private int maxArmManifestItems;
    private int maxArmSingleModeItems;
    private int threads;
    private Duration asyncTimeout;

}