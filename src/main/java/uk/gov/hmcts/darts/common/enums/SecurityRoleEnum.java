package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public enum SecurityRoleEnum {

    APPROVER(1),
    REQUESTER(2),
    JUDGE(3),
    TRANSCRIBER(4),
    TRANSLATION_QA(5),
    RCJ_APPEALS(6),
    XHIBIT(7),
    CPP(8),
    DAR_PC(9),
    MID_TIER(10),
    SUPER_ADMIN(11),
    SUPER_USER(12);

    private final Integer id;

    private static final Map<Integer, SecurityRoleEnum> BY_ID = new ConcurrentHashMap<>();

    static {
        for (SecurityRoleEnum e : values()) {
            BY_ID.put(e.id, e);
        }
    }

    public static SecurityRoleEnum valueOfId(Integer id) {
        return BY_ID.get(id);
    }


}
