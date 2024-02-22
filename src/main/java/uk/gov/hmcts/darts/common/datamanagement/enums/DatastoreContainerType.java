package uk.gov.hmcts.darts.common.datamanagement.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DatastoreContainerType {
    INBOUND(1),
    OUTBOUND(-1),
    UNSTRUCTURED(2),
    ARM(3),
    DETS(4);

    private final Integer id;
}