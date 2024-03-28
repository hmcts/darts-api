package uk.gov.hmcts.darts.retention.service;

import uk.gov.hmcts.darts.retentions.model.AdminPostRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.RetentionPolicy;

import java.util.List;

public interface RetentionPolicyService {

    List<RetentionPolicy> getRetentionPolicyTypes();

    RetentionPolicy getRetentionPolicyType(Integer id);

    RetentionPolicy createOrReviseRetentionPolicyType(AdminPostRetentionRequest adminPostRetentionRequest, Boolean isRevision);

}
