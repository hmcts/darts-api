package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExternalLocationTypeEnum {

    INBOUND(1),
    UNSTRUCTURED(2),
    ARM(3),
    TEMPSTORE(4),
    VODAFONE(5);

    private final Integer id;

}
