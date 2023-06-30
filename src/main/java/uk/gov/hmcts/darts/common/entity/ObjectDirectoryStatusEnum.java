package uk.gov.hmcts.darts.common.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ObjectDirectoryStatusEnum {

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
    DELETED(11);

    private final Integer id;

}
