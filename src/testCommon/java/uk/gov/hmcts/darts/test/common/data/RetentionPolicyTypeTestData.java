package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;

import java.time.OffsetDateTime;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;


public class RetentionPolicyTypeTestData {

    private RetentionPolicyTypeTestData() {

    }

    public static RetentionPolicyTypeEntity someMinimalRetentionPolicyType() {
        var postfix = random(10, false, true);
        var retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setFixedPolicyKey("some-fixed-policy-key-" + postfix);
        retentionPolicyType.setPolicyName("some-policy-name-" + postfix);
        retentionPolicyType.setDuration("some-duration");
        retentionPolicyType.setPolicyStart(OffsetDateTime.now());
        retentionPolicyType.setCreatedBy(minimalUserAccount());
        retentionPolicyType.setLastModifiedBy(minimalUserAccount());
        retentionPolicyType.setDescription("some-description-" + postfix);

        return retentionPolicyType;
    }
}