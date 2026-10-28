package uk.gov.hmcts.darts.cases.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseLinkImportStatus {
    RECEIVED(1),
    PROCESSING(2),
    COMPLETED(3),
    COMPLETED_WITH_ERRORS(4),
    REJECTED(5),
    FAILED(6);

    private final Integer id;

    public static CaseLinkImportStatus fromId(Integer id) {
        for (var status : values()) {
            if (status.id.equals(id)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown case link import status ID: " + id);
    }
}
