package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MediaLinkedCaseSourceType {
    ADD_AUDIO_METADATA(1),
    ADD_AUDIO_EVENT_LINKING(2),
    AUDIO_LINKING_TASK(3);

    private final Integer id;

}