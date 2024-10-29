package uk.gov.hmcts.darts.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SystemUsersEnum {
    DEFAULT(0),
    EVENT_PROCESSOR(-1),
    DAILY_LIST_PROCESSOR(-2),
    ADD_AUDIO_PROCESSOR(-3),
    ADD_CASE_PROCESSOR(-4),
    AUDIO_LINKING_AUTOMATED_TASK(-5);

    private final int id;

}
