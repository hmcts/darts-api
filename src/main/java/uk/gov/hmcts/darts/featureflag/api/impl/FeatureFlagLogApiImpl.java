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
        logIfEnabled(featureFlagLoggingConfig.isArmRpoFeatureFlagLogsEnabled(), logMessage);
    }

    @Override
    public void logDetsToArmPush(String logMessage) {
        logIfEnabled(featureFlagLoggingConfig.isDetsToArmPushFeatureFlagLogsEnabled(), logMessage);
    }

    @Override
    public void logArmPull(String logMessage) {
        logIfEnabled(featureFlagLoggingConfig.isArmPullFeatureFlagLogsEnabled(), logMessage);
    }

    private void logIfEnabled(boolean isEnabled, String logMessage) {
        if (isEnabled) {
            log.info(logMessage);
        }
    }
}
