package uk.gov.hmcts.darts.retention.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RetentionConfidenceReasonEnum {

    CASE_CLOSED, MANUAL_OVERRIDE, AGED_CASE;
}