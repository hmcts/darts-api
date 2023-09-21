package uk.gov.hmcts.darts.transcriptions.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TranscriptionUrgencyEnum {
    STANDARD(1),
    OVERNIGHT(2),
    OTHER(3);

    private final Integer id;

}
