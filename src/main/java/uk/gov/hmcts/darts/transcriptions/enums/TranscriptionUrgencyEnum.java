package uk.gov.hmcts.darts.transcriptions.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.BAD_REQUEST_TRANSCRIPTION_URGENCY;

@AllArgsConstructor
@Getter
public enum TranscriptionUrgencyEnum {
    STANDARD(1, 999),
    OVERNIGHT(2, 1),
    OTHER(3, 6),
    WORKING_DAYS_3(4, 3),
    WORKING_DAYS_7(5, 4),
    WORKING_DAYS_12(6, 5),
    WORKING_DAYS_2(7, 2);

    private final Integer id;
    private final Integer priorityOrder;

    public static TranscriptionUrgencyEnum fromId(Integer id) {
        for (TranscriptionUrgencyEnum b : TranscriptionUrgencyEnum.values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_URGENCY);
    }

}
