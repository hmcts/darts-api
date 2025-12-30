package uk.gov.hmcts.darts.featureflag.api;

public interface FeatureFlagLogApi {

    void logArmRpo(String logMessage);

    void logDetsToArmPush(String logMessage);

    void logDetsToArmPull(String logMessage);
}
