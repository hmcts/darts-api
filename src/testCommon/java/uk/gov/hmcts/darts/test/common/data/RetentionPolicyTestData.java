package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class RetentionPolicyTestData {

    public static RetentionPolicyTypeEntity minimalRetentionPolicy() {
        var minimalRetentionPolicy = new RetentionPolicyTypeEntity();
        minimalRetentionPolicy.setFixedPolicyKey("some-fixed-policy-key");
        minimalRetentionPolicy.setPolicyName("some-policy-name");
        minimalRetentionPolicy.setDuration("some-duration");
        minimalRetentionPolicy.setPolicyStart(OffsetDateTime.now());
        minimalRetentionPolicy.setCreatedBy(minimalUserAccount());
        minimalRetentionPolicy.setLastModifiedBy(minimalUserAccount());
        minimalRetentionPolicy.setDescription("some-description");
        minimalRetentionPolicy.setDisplayName("some-name");
        return minimalRetentionPolicy;
    }
}