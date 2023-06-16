package uk.gov.hmcts.darts.event.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum DarNotifyType {

    START_RECORDING("1"),
    STOP_RECORDING("2"),
    CASE_UPDATE("3");

    private final String notificationType;

}
