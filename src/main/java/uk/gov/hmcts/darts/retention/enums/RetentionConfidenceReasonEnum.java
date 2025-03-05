package uk.gov.hmcts.darts.retention.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RetentionConfidenceReasonEnum {

    // Modernised values
    CASE_CLOSED,
    MANUAL_OVERRIDE,
    AGED_CASE,
    MAX_HEARING_CLOSED,

    // Legacy values
    MAX_EVENT_CLOSED,
    MAX_MEDIA_CLOSED,
    CASE_CREATION_CLOSED;
}