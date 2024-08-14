package uk.gov.hmcts.darts.audit.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AuditActivityRepository;
import uk.gov.hmcts.darts.common.repository.AuditRepository;

@RequiredArgsConstructor
@Service
@Slf4j
public class AuditServiceImpl implements AuditService {
    private final AuditActivityRepository auditActivityRepository;
    private final AuditRepository auditRepository;

    @Value("${darts.audit.application-server}")
    private String applicationServer;

    @Transactional
    @Override
    public void recordAudit(AuditActivity activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase) {
        AuditEntity auditEntity = new AuditEntity();
        auditEntity.setCourtCase(courtCase);
        auditEntity.setAuditActivity(auditActivityRepository.getReferenceById(activity.getId()));
        auditEntity.setUser(userAccountEntity);
        auditEntity.setAdditionalData(null);
        auditEntity.setCreatedBy(userAccountEntity);
        auditEntity.setLastModifiedBy(userAccountEntity);
        auditRepository.saveAndFlush(auditEntity);
    }


}