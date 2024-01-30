package uk.gov.hmcts.darts.retention.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RetentionPolicyEnum {
    LEGACY_PERMANENT("-1"),
    LEGACY_STANDARD("-2"),
    NOT_GUILTY("1"),
    NON_CUSTODIAL("2"),
    CUSTODIAL("3"),
    LIFE("4"),
    DEFAULT("0"),
    PERMANENT("PERM"),
    MANUAL("MANUAL");

    private final String policyKey;
}
