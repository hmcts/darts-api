package uk.gov.hmcts.darts.retentions.service;

import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;

public interface RetentionsService {

    void postRetention(PostRetentionRequest postRetentionRequest);
}
