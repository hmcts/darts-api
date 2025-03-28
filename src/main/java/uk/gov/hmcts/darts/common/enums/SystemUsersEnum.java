package uk.gov.hmcts.darts.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SystemUsersEnum {
    DEFAULT(0),
    DAILY_LIST_HOUSEKEEPING_AUTOMATED_TASK(-1),
    DAILY_LIST_PROCESSOR(-2),
    ADD_AUDIO_PROCESSOR(-3),
    ADD_CASE_PROCESSOR(-4),
    CLOSE_OLD_UNFINISHED_TRANSCRIPTIONS(-7),
    OUTBOUND_AUDIO_DELETER_AUTOMATED_TASK(-8),
    INBOUND_AUDIO_DELETER_AUTOMATED_TASK(-9),
    EXTERNAL_DATA_STORE_DELETER_AUTOMATED_TASK(-10),
    INBOUND_TO_UNSTRUCTURED_DATA_STORE_AUTOMATED_TASK(-11),
    UNSTRUCTURED_AUDIO_DELETER_AUTOMATED_TASK(-12),
    CLOSE_OLD_CASES_AUTOMATED_TASK(-17),
    APPLY_RETENTION_AUTOMATED_TASK(-16),
    CLEANUP_ARM_RESPONSE_FILES_AUTOMATED_TASK(-15),
    PROCESS_ARM_RESPONSE_FILES_AUTOMATED_TASK(-14),
    UNSTRUCTURED_TO_ARM_DATA_STORE_AUTOMATED_TASK(-13),
    PROCESS_ARM_RPO_PENDING_AUTOMATED_TASK(-36),
    ARM_RPO_REPLAY_AUTOMATED_TASK(-35),
    ARM_RPO_POLLING_AUTOMATED_TASK(-34),
    PROCESS_E2E_ARM_RPO_PENDING_AUTOMATED_TASK(-33),
    AUDIO_LINKING_AUTOMATED_TASK(-32),
    DETS_TO_ARM_AUTOMATED_TASK(-31),
    MANUAL_DELETION_AUTOMATED_TASK(-30),
    PROCESS_DETS_TO_ARM_RESPONSE_AUTOMATED_TASK(-29),
    ASSOCIATED_OBJECT_DATA_EXPIRY_DELETION_AUTOMATED_TASK(-28),
    CASE_EXPIRY_DELETION_AUTOMATED_TASK(-27),
    GENERATE_CASE_DOCUMENT_FOR_RETENTION_DATE_AUTOMATED_TASK(-26),
    UNSTRUCTURED_TRANSCRIPTION_ANNOTATION_DELETER_AUTOMATED_TASK(-25),
    INBOUND_TRANSCRIPTION_ANNOTATION_DELETER_AUTOMATED_TASK(-24),
    REMOVE_DUPLICATED_EVENTS_AUTOMATED_TASK(-23),
    GENERATE_CASE_DOCUMENT_AUTOMATED_TASK(-22),
    BATCH_CLEANUP_ARM_RESPONSE_FILES_AUTOMATED_TASK(-21),
    APPLY_RETENTION_CASE_ASSOCIATED_OBJECTS_AUTOMATED_TASK(-20),
    ARM_RETENTION_EVENT_DATE_CALCULATOR_AUTOMATED_TASK(-19);

    private final int id;

}
