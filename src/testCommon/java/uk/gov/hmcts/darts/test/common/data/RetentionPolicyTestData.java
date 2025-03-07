package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;

import java.time.OffsetDateTime;

public final class RetentionPolicyTestData {

    private RetentionPolicyTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public static RetentionPolicyTypeEntity minimalRetentionPolicy() {
        var minimalRetentionPolicy = new RetentionPolicyTypeEntity();
        minimalRetentionPolicy.setFixedPolicyKey("some-fixed-policy-key");
        minimalRetentionPolicy.setPolicyName("some-policy-name");
        minimalRetentionPolicy.setDuration("some-duration");
        minimalRetentionPolicy.setDisplayName("some-display-name");
        minimalRetentionPolicy.setPolicyStart(OffsetDateTime.now());
        minimalRetentionPolicy.setCreatedById(0);
        minimalRetentionPolicy.setLastModifiedById(0);
        minimalRetentionPolicy.setDescription("some-description");

        return minimalRetentionPolicy;
    }
}