package uk.gov.hmcts.darts.retention.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.retentions.model.CaseRetention;


@Component
@RequiredArgsConstructor
public class RetentionMapper {

    public CaseRetention mapToCaseRetention(CaseRetentionEntity caseRetentionEntity) {
        CaseRetention caseRetention = new CaseRetention();
        caseRetention.setRetentionDate(caseRetentionEntity.getRetainUntil());
        //Is this right?? No amended_by field
        caseRetention.setAmendedBy(caseRetentionEntity.getCreatedBy().getEmailAddress());
        caseRetention.setRetentionPolicyApplied(caseRetentionEntity.getRetentionPolicyType().getPolicyName());
        caseRetention.setComments(caseRetentionEntity.getComments());
        caseRetention.setStatus(caseRetentionEntity.getCurrentState());
        return caseRetention;
    }
}
