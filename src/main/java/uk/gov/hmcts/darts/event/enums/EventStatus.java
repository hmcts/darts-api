package uk.gov.hmcts.darts.event.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum EventStatus {

    HERITAGE("1"),
    MODERNISED("2");

    private final String statusNumber;

}
