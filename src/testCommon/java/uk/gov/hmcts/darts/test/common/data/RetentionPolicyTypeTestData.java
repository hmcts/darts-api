package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;

import java.time.OffsetDateTime;

import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;


@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class RetentionPolicyTypeTestData {

    public static RetentionPolicyTypeEntity someMinimalRetentionPolicyType() {
        var retentionPolicyType = new RetentionPolicyTypeEntity();
        retentionPolicyType.setFixedPolicyKey("some-fixed-policy-key");
        retentionPolicyType.setPolicyName("some-policy-name");
        retentionPolicyType.setDuration("some-duration");
        retentionPolicyType.setPolicyStart(OffsetDateTime.now());
        retentionPolicyType.setCreatedBy(minimalUserAccount());
        retentionPolicyType.setLastModifiedBy(minimalUserAccount());
        retentionPolicyType.setDescription("some-description");

        return retentionPolicyType;
    }
}
