package uk.gov.hmcts.darts.retention.service;

import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.PostRetentionResponse;

public interface RetentionPostService {

    PostRetentionResponse postRetention(Boolean validateOnly, PostRetentionRequest postRetentionRequest);
}
