package uk.gov.hmcts.darts.retention.service;

import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;

public interface RetentionPostService {

    void postRetention(PostRetentionRequest postRetentionRequest);
}
