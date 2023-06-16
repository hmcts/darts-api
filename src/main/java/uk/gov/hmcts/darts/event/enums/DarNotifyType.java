package uk.gov.hmcts.darts.event.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum DarNotifyType {

    START_RECORDING("1"),
    STOP_RECORDING("2"),
    CASE_UPDATE("3");

    private final String notificationType;

}
