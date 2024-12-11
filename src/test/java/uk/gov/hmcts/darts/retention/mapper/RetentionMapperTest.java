package uk.gov.hmcts.darts.retention.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retentions.model.GetCaseRetentionsResponse;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetentionMapperTest {

    @Test
    void testMapping() {
        CaseRetentionEntity caseRetentionEntity = new CaseRetentionEntity();

        OffsetDateTime lastModifiedBy = OffsetDateTime.now();
        caseRetentionEntity.setLastModifiedDateTime(lastModifiedBy);

        OffsetDateTime retentionDate = OffsetDateTime.now().plusYears(7);
        caseRetentionEntity.setRetainUntil(retentionDate);

        UserAccountEntity submittedBy = new UserAccountEntity();
        submittedBy.setEmailAddress("submit@email.com");
        submittedBy.setUserFullName("The User");
        caseRetentionEntity.setSubmittedBy(submittedBy);
        caseRetentionEntity.setId(1);

        RetentionPolicyTypeEntity retentionPolicyTypeEntity = new RetentionPolicyTypeEntity();
        retentionPolicyTypeEntity.setDisplayName("Standard");
        caseRetentionEntity.setRetentionPolicyType(retentionPolicyTypeEntity);

        caseRetentionEntity.setComments("Some comments");
        caseRetentionEntity.setCurrentState("A State");

        RetentionMapper retentionMapper = new RetentionMapper();
        GetCaseRetentionsResponse caseRetention = retentionMapper.mapToCaseRetention(caseRetentionEntity);

        assertEquals(caseRetentionEntity.getLastModifiedDateTime(), caseRetention.getRetentionLastChangedDate());

        assertEquals(caseRetentionEntity.getRetainUntil().toLocalDate(), caseRetention.getRetentionDate());
        assertEquals(caseRetentionEntity.getSubmittedBy().getUserFullName(), caseRetention.getAmendedBy());
        assertEquals(caseRetentionEntity.getRetentionPolicyType().getDisplayName(), caseRetention.getRetentionPolicyApplied());
        assertEquals(caseRetentionEntity.getComments(), caseRetention.getComments());
        assertEquals(caseRetentionEntity.getCurrentState(), caseRetention.getStatus());
    }
}