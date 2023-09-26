package uk.gov.hmcts.darts.transcriptions.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TranscriptionStatusEnum {
    REQUESTED(1),
    AWAITING_AUTHORISATION(2),
    APPROVED(3),
    REJECTED(4),
    WITH_TRANSCRIBER(5),
    COMPLETE(6),
    CLOSED(7);

    private final Integer id;

}
