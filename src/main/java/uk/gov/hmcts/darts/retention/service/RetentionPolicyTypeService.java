package uk.gov.hmcts.darts.retention.service;

import uk.gov.hmcts.darts.retentions.model.AdminPatchRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.AdminPostRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.RetentionPolicyType;

import java.util.List;

public interface RetentionPolicyTypeService {

    List<RetentionPolicyType> getRetentionPolicyTypes();

    RetentionPolicyType getRetentionPolicyType(Integer id);

    RetentionPolicyType createOrReviseRetentionPolicyType(AdminPostRetentionRequest adminPostRetentionRequest, Boolean isRevision);

    RetentionPolicyType editRetentionPolicyType(Integer id, AdminPatchRetentionRequest adminPatchRetentionRequest);

}
