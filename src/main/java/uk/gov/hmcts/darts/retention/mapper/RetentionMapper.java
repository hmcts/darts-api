package uk.gov.hmcts.darts.retention.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.retentions.model.GetCaseRetentionsResponse;


@Component
@RequiredArgsConstructor
public class RetentionMapper {

    public GetCaseRetentionsResponse mapToCaseRetention(CaseRetentionEntity caseRetentionEntity) {
        GetCaseRetentionsResponse caseRetention = new GetCaseRetentionsResponse();
        caseRetention.setRetentionLastChangedDate(caseRetentionEntity.getLastModifiedDateTime());
        caseRetention.setRetentionDate(caseRetentionEntity.getRetainUntil().toLocalDate());
        caseRetention.setAmendedBy(caseRetentionEntity.getSubmittedBy().getUserName());
        caseRetention.setRetentionPolicyApplied(caseRetentionEntity.getRetentionPolicyType().getDisplayName());
        caseRetention.setComments(caseRetentionEntity.getComments());
        caseRetention.setStatus(caseRetentionEntity.getCurrentState());
        return caseRetention;
    }
}
