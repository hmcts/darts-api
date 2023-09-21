package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SecurityRoleEnum {

    APPROVER(1),
    REQUESTER(2),
    JUDGE(3),
    TRANSCRIBER(4),
    LANGUAGE_SHOP_USER(5),
    RCJ_APPEALS(6);

    private final Integer id;

}
