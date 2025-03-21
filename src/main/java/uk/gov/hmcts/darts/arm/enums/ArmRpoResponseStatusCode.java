package uk.gov.hmcts.darts.arm.enums;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ArmRpoResponseStatusCode {
    IN_PROGRESS_STATUS(1),
    READY_STATUS(4);

    @Getter
    private final int statusCode;

    private static final Map<Integer, ArmRpoResponseStatusCode> BY_STATUS_CODE = new ConcurrentHashMap<>();

    static {
        for (ArmRpoResponseStatusCode armRpoResponseStatusCode : values()) {
            BY_STATUS_CODE.put(armRpoResponseStatusCode.statusCode, armRpoResponseStatusCode);
        }
    }

    ArmRpoResponseStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public static ArmRpoResponseStatusCode valueOfId(Integer id) {
        return BY_STATUS_CODE.get(id);
    }

}
