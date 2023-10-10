package uk.gov.hmcts.darts.audit.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuditActivityEnum {

    MOVE_COURTROOM(1),
    EXPORT_AUDIO(2),
    REQUEST_AUDIO(3),
    AUDIO_PLAYBACK(4),
    APPLY_RETENTION(5),
    REQUEST_TRANSCRIPTION(6),
    IMPORT_TRANSCRIPTION(7);

    private final Integer id;
}
