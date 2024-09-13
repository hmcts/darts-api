package uk.gov.hmcts.darts.event.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum EventStatus {

    AUDIO_LINK_NOT_DONE_HERITAGE(1),
    AUDIO_LINK_NOT_DONE_MODERNISED(2),
    AUDIO_LINKED(3);

    private final Integer statusNumber;

}
