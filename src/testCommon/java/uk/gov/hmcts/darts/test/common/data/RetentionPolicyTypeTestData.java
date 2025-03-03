package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;

import java.time.OffsetDateTime;

import static org.apache.commons.lang3.RandomStringUtils.random;

public final class RetentionPolicyTypeTestData {

    private RetentionPolicyTypeTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public static RetentionPolicyTypeEntity someMinimalRetentionPolicyType() {
        var postfix = random(10, false, true);
        var retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setFixedPolicyKey("some-fixed-policy-key-" + postfix);
        retentionPolicyType.setPolicyName("some-policy-name-" + postfix);
        retentionPolicyType.setDisplayName("some-display-name-" + postfix);
        retentionPolicyType.setDuration("some-duration");
        retentionPolicyType.setPolicyStart(OffsetDateTime.now());
        retentionPolicyType.setCreatedById(0);
        retentionPolicyType.setLastModifiedById(0);
        retentionPolicyType.setDescription("some-description-" + postfix);

        return retentionPolicyType;
    }
}