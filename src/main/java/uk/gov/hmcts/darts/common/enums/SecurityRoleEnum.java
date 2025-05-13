package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public enum SecurityRoleEnum {

    JUDICIARY(1),
    REQUESTER(2),
    APPROVER(3),
    TRANSCRIBER(4),
    TRANSLATION_QA(5),
    RCJ_APPEALS(6),
    SUPER_USER(7),
    SUPER_ADMIN(8),
    MEDIA_ACCESSOR(9),
    DARTS(10),
    XHIBIT(11),
    CPP(12),
    DAR_PC(13),
    MID_TIER(14),
    MEDIA_IN_PERPETUITY(15);

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
