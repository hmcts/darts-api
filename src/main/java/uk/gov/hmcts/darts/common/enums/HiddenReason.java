package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public enum HiddenReason {
    PUBLIC_INTEREST_IMMUNITY(1),
    CLASSIFIED(2),
    OTHER_DELETE(3),
    OTHER_HIDE(4);

    private final Integer id;

    private static final Map<Integer, HiddenReason> BY_ID = new ConcurrentHashMap<>();

    static {
        for (HiddenReason e : values()) {
            BY_ID.put(e.id, e);
        }
    }

    public static HiddenReason valueOfId(Integer id) {
        return BY_ID.get(id);
    }
}