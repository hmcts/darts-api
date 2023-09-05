package uk.gov.hmcts.darts.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TranscriptionStatusTypeEnum {
    REQUESTED(1),
    AWAITING_AUTHORISATION(2),
    WITH_TRANSCRIBER(3),
    COMPLETE(4),
    REJECTED(5),
    CLOSED(6);

    private final Integer statusType;

}
