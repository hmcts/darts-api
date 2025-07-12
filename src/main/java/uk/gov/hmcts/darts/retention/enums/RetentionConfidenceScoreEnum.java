package uk.gov.hmcts.darts.retention.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RetentionConfidenceScoreEnum {

    CASE_PERFECTLY_CLOSED(1), CASE_NOT_PERFECTLY_CLOSED(0);

    private final Integer id;
}