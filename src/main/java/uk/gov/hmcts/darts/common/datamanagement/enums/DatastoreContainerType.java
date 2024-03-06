package uk.gov.hmcts.darts.common.datamanagement.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum DatastoreContainerType {
    INBOUND(1, Optional.of(ExternalLocationTypeEnum.INBOUND)),
    OUTBOUND(-1, Optional.ofNullable(null)),
    UNSTRUCTURED(2, Optional.of(ExternalLocationTypeEnum.UNSTRUCTURED)),
    ARM(3, Optional.of(ExternalLocationTypeEnum.ARM)),
    DETS(4, Optional.of(ExternalLocationTypeEnum.DETS));

    private final Integer id;
    private final Optional<ExternalLocationTypeEnum> externalLocationTypeEnum;
}