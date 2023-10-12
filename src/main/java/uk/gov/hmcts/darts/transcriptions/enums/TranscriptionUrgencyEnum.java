package uk.gov.hmcts.darts.transcriptions.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.BAD_REQUEST_TRANSCRIPTION_URGENCY;

@AllArgsConstructor
@Getter
public enum TranscriptionUrgencyEnum {
    STANDARD(1),
    OVERNIGHT(2),
    OTHER(3);

    private final Integer id;

    public static TranscriptionUrgencyEnum fromId(Integer id) {
        for (TranscriptionUrgencyEnum b : TranscriptionUrgencyEnum.values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_URGENCY);
    }

}
