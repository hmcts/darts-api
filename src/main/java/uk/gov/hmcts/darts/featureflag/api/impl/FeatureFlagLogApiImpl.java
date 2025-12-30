package uk.gov.hmcts.darts.featureflag.api.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.featureflag.api.FeatureFlagLogApi;
import uk.gov.hmcts.darts.featureflag.config.FeatureFlagLoggingConfig;

@Slf4j
@Service
@AllArgsConstructor
public class FeatureFlagLogApiImpl implements FeatureFlagLogApi {

    private FeatureFlagLoggingConfig featureFlagLoggingConfig;

    @Override
    public void logArmRpo(String logMessage) {
        logIfEnabled(featureFlagLoggingConfig.isEnableArmRpoFeatureFlagLogs(), logMessage);
    }

    @Override
    public void logDetsToArmPush(String logMessage) {
        logIfEnabled(featureFlagLoggingConfig.isEnableDetsToArmPushFeatureFlagLogs(), logMessage);
    }

    @Override
    public void logDetsToArmPull(String logMessage) {
        logIfEnabled(featureFlagLoggingConfig.isEnableDetsToArmPullFeatureFlagLogs(), logMessage);
    }

    private void logIfEnabled(boolean isEnabled, String logMessage) {
        if (isEnabled) {
            log.info(logMessage);
        }
    }
}
