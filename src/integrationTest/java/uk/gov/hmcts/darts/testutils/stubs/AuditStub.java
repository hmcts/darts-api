package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.AuditActivityEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AuditActivityRepository;
import uk.gov.hmcts.darts.common.repository.AuditRepository;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuditStub {
    private final AuditActivityRepository auditActivityRepository;
    private final AuditRepository auditRepository;
    private final UserAccountStub userAccountStub;

    public AuditActivityEntity getAnyAuditActivity() {
        Optional<AuditActivityEntity> found = auditActivityRepository.findById(1);
        return found.orElseGet(this::createTestAuditActivityEntity);
    }

    private AuditActivityEntity createTestAuditActivityEntity() {
        AuditActivityEntity auditActivity = new AuditActivityEntity();
        auditActivity.setDescription("Test AuditActivityEntity description");
        auditActivity.setCreatedBy(userAccountStub.getDefaultUser());
        auditActivity.setLastModifiedBy(userAccountStub.getDefaultUser());
        return auditActivity;
    }

    public AuditEntity createAuditEntity(CourtCaseEntity courtCase, AuditActivityEntity auditActivity, UserAccountEntity userAccount, String applicationServer, String additionalData) {
        AuditEntity auditEntity = new AuditEntity();
        auditEntity.setCaseId(courtCase.getId());
        auditEntity.setAuditActivity(auditActivity.getId());
        auditEntity.setUserId(userAccount.getId());
        auditEntity.setApplicationServer(applicationServer);
        auditEntity.setAdditionalData(additionalData);
        auditEntity.setCreatedBy(userAccount);
        auditEntity.setLastModifiedBy(userAccount);
        auditRepository.saveAndFlush(auditEntity);
        return auditEntity;
    }
}
