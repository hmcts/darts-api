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
    COMPLETE_TRANSCRIPTION(12);

    private final Integer id;
}
