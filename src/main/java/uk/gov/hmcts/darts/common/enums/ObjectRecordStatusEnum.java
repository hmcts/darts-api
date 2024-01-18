package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
    DELETED(11),
    ARM_INGESTION(12),
    ARM_DROP_ZONE(13),
    FAILURE_ARM_RAW_DATA_FAILED(14),
    FAILURE_ARM_MANIFEST_FILE_FAILED(15),
    ARM_PROCESSING_RESPONSE_FILES(16),
    ARM_RESPONSE_PROCESSING_FAILED(17);

    private final Integer id;

}
