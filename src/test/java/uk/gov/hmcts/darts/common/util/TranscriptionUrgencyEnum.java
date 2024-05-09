package uk.gov.hmcts.darts.common.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.BAD_REQUEST_TRANSCRIPTION_URGENCY;

@AllArgsConstructor
@Getter
public enum TranscriptionUrgencyEnum {
    STANDARD(1, 999, "Standard"),
    OVERNIGHT(2, 1, "Overnight"),
    OTHER(3, 6, "Other"),
    WORKING_DAYS_3(4, 3, "Up to 3 working days"),
    WORKING_DAYS_7(5, 4,"Up to 7 working days"),
    WORKING_DAYS_12(6, 5, "Up to 12 working days"),
    WORKING_DAYS_2(7, 2, "Up to 2 working days");

    private final Integer id;
    private final Integer priorityOrderId;
    private final String  description;

    public static TranscriptionUrgencyEnum fromId(Integer id) {
        for (TranscriptionUrgencyEnum b : values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_URGENCY);
    }

}