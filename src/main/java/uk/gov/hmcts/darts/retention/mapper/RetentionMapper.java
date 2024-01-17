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
        caseRetention.setRetentionLastChangedDate(caseRetentionEntity.getLastModifiedDateTime());
        caseRetention.setRetentionDate(caseRetentionEntity.getRetainUntil());
        caseRetention.setAmendedBy(caseRetentionEntity.getSubmittedBy().getUserName());
        caseRetention.setRetentionPolicyApplied(caseRetentionEntity.getRetentionPolicyType().getPolicyName());
        caseRetention.setComments(caseRetentionEntity.getComments());
        caseRetention.setStatus(caseRetentionEntity.getCurrentState());
        return caseRetention;
    }
}
