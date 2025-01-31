package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@RequiredArgsConstructor
public enum ObjectRecordStatusEnum {

    NEW(1),
    STORED(2),
    FAILURE(3),
    FAILURE_FILE_NOT_FOUND(4),
    FAILURE_FILE_SIZE_CHECK_FAILED(5),
    FAILURE_FILE_TYPE_CHECK_FAILED(6),
    FAILURE_CHECKSUM_FAILED(7),
    FAILURE_ARM_INGESTION_FAILED(8),
    AWAITING_VERIFICATION(9),
    MARKED_FOR_DELETION(10),
    @Deprecated
    DELETED(11),
    ARM_INGESTION(12),
    ARM_DROP_ZONE(13),
    ARM_RAW_DATA_FAILED(14),
    ARM_MANIFEST_FAILED(15),
    ARM_PROCESSING_RESPONSE_FILES(16),
    ARM_RESPONSE_PROCESSING_FAILED(17),
    ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED(18),
    ARM_RESPONSE_MANIFEST_FAILED(19),
    FAILURE_EMPTY_FILE(20),
    ARM_RPO_PENDING(21),
    ARM_REPLAY(22),
    ARM_MISSING_RESPONSE(23),
    ARM_PUSH_POD_RECYCLED(24),
    ARM_PULL_POD_RECYCLED(25);

    private static final Map<Integer, ObjectRecordStatusEnum> BY_ID = new ConcurrentHashMap<>();

    static {
        for (ObjectRecordStatusEnum e : values()) {
            BY_ID.put(e.id, e);
        }
    }

    private final Integer id;

    public static ObjectRecordStatusEnum valueOfId(Integer id) {
        return BY_ID.get(id);
    }

}