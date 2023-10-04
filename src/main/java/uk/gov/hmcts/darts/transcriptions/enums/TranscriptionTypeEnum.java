package uk.gov.hmcts.darts.transcriptions.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.BAD_REQUEST_TRANSCRIPTION_TYPE;

@AllArgsConstructor
@Getter
public enum TranscriptionTypeEnum {
    SENTENCING_REMARKS(1),
    INCLUDING_VERDICT(2),
    ANTECEDENTS(3),
    ARGUMENT_AND_SUBMISSION_OF_RULING(4),
    COURT_LOG(5),
    MITIGATION(6),
    PROCEEDINGS_AFTER_VERDICT(7),
    PROSECUTION_OPENING_OF_FACTS(8),
    SPECIFIED_TIMES(9),
    OTHER(999);

    private final Integer id;

    public static TranscriptionTypeEnum fromId(Integer id) {
        for (TranscriptionTypeEnum b : TranscriptionTypeEnum.values()) {
            if (b.id.equals(id)) {
                return b;
            }
        }
        throw new DartsApiException(BAD_REQUEST_TRANSCRIPTION_TYPE);
    }

}
