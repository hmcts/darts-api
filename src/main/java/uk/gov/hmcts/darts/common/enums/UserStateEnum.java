package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStateEnum {

    ENABLED(0),
    DISABLED(1);

    private final Integer id;

}
