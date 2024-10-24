package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ArmRpoStatusEnum {

    IN_PROGRESS(1),
    COMPLETED(2),
    FAILED(3);

    private final Integer id;

}
