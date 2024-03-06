package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SecurityRoleEnum {

    APPROVER(1),
    REQUESTER(2),//all cases in Swansea(1)
    JUDGE(3),
    TRANSCRIBER(4),
    TRANSLATION_QA(5),//interpreter cases in all courthouses (1,2,3)
    RCJ_APPEALS(6),
    XHIBIT(7),
    CPP(8),
    DAR_PC(9),
    MID_TIER(10),
    SUPER_ADMIN(11),
    SUPER_USER(12);

    private final Integer id;

}
