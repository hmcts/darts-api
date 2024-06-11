package uk.gov.hmcts.darts.audit.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuditActivity {

    MOVE_COURTROOM(1),
    EXPORT_AUDIO(2),
    REQUEST_AUDIO(3),
    AUDIO_PLAYBACK(4),
    APPLY_RETENTION(5),
    REQUEST_TRANSCRIPTION(6),
    IMPORT_TRANSCRIPTION(7),
    DOWNLOAD_TRANSCRIPTION(8),
    AUTHORISE_TRANSCRIPTION(9),
    REJECT_TRANSCRIPTION(10),
    ACCEPT_TRANSCRIPTION(11),
    COMPLETE_TRANSCRIPTION(12),
    IMPORT_ANNOTATION(13),
    CREATE_COURTHOUSE(14),
    UPDATE_COURTHOUSE_GROUP(15),
    UPDATE_COURTHOUSE(16),
    CREATE_USER(17),
    DEACTIVATE_USER(18),
    REACTIVATE_USER(19),
    UPDATE_USER(20),
    CREATE_GROUP(21),
    UPDATE_GROUP(22),
    UPDATE_USERS_GROUP(23),
    ENABLE_DISABLE_JOB(24),
    RUN_JOB_MANUALLY(25);

    private final Integer id;
}