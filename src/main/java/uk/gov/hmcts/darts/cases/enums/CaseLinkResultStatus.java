package uk.gov.hmcts.darts.cases.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseLinkResultStatus {
    PENDING(1),
    SUCCESS(2),
    FAILED(3);

    private final Integer id;
}
