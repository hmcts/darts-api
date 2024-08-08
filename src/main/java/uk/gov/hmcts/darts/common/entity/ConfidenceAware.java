package uk.gov.hmcts.darts.common.entity;

public interface ConfidenceAware  {
    Integer getRetConfScore();

    String getRetConfReason();
}