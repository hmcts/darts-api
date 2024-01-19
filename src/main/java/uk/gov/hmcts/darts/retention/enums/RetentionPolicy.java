package uk.gov.hmcts.darts.retention.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RetentionPolicy {
    PERMANENT(-1),
    STANDARD(-2),
    NOT_GUILTY(1),
    NON_CUSTODIAL(2),
    CUSTODIAL(3),
    LIFE(4);

    private final int policyKey;
}
