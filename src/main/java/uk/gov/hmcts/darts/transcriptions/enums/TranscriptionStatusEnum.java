package uk.gov.hmcts.darts.transcriptions.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.BAD_REQUEST_TRANSCRIPTION_STATUS;

@AllArgsConstructor
@Getter
public enum TranscriptionStatusEnum {

    REQUESTED(1),
    AWAITING_AUTHORISATION(2),
    APPROVED(3),
    REJECTED(4),
    WITH_TRANSCRIBER(5),
    COMPLETE(6),
    CLOSED(7),
    UNFULFILLED(8);

    private final Integer id;

    public static TranscriptionStatusEnum fromId(Integer id) {
        for (TranscriptionStatusEnum b : values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_STATUS);
    }

}
