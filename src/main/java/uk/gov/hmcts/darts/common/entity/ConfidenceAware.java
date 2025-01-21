package uk.gov.hmcts.darts.common.entity;

import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

public interface ConfidenceAware  {
    RetentionConfidenceScoreEnum getRetConfScore();

    String getRetConfReason();
}