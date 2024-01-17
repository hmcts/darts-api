package uk.gov.hmcts.darts.retention.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Policy {
    PERMANENT("DARTS Permanent Retention v3"),
    STANDARD("DARTS Standard Retention v3"),
    NOT_GUILTY("DARTS Not Guilty Policy"),
    CUSTODIAL("DARTS Custodial Policy"),
    NON_CUSTODIAL("DARTS Non Custodial Policy"),
    LIFE("DARTS Life Policy");

    private final String policyName;

    public static Policy getForPolicyName(String policyName) {
        for (Policy policy: Policy.values()) {
            if (policy.getPolicyName().equals(policyName)) {
                return policy;
            }
        }
        return null;
    }

}
