package uk.gov.hmcts.darts.retention.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RetentionConfidenceCategoryEnum {

    CASE_CLOSED(1), MANUAL_OVERRIDE(2), AGED_CASE(3);

    private final Integer id;
}