package uk.gov.hmcts.darts.transcriptions.validator.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;

@AllArgsConstructor
@Getter
public class StatusTransitionCheck {
    TranscriptionStatusEnum fromStatus;
    TranscriptionStatusEnum toStatus;
    boolean shouldBeAllowed;

}
