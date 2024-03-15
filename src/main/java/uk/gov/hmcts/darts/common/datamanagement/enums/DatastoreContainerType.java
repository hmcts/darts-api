package uk.gov.hmcts.darts.common.datamanagement.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;

import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum DatastoreContainerType {
    INBOUND(Optional.of(ExternalLocationTypeEnum.INBOUND)),
    OUTBOUND(Optional.empty()),
    UNSTRUCTURED(Optional.of(ExternalLocationTypeEnum.UNSTRUCTURED)),
    ARM(Optional.of(ExternalLocationTypeEnum.ARM)),
    DETS(Optional.of(ExternalLocationTypeEnum.DETS));

    private final Optional<ExternalLocationTypeEnum> externalLocationTypeEnum;
}