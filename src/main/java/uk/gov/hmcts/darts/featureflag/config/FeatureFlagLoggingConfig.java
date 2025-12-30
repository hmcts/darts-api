package uk.gov.hmcts.darts.featureflag.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "darts.feature-flag.logging")
@Getter
@Setter
@Configuration
public class FeatureFlagLoggingConfig {

    private boolean enableArmRpoFeatureFlagLogs;
    private boolean enableDetsToArmPushFeatureFlagLogs;
    private boolean enableDetsToArmPullFeatureFlagLogs;
    
}
