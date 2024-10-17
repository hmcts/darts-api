package uk.gov.hmcts.darts.audit.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AuditActivityRepository;
import uk.gov.hmcts.darts.common.repository.AuditRepository;

import java.util.Optional;

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
    public void recordAudit(AuditActivity activity, UserAccountEntity userAccountEntity, Optional<CourtCaseEntity> courtCase, Optional<String> additionalData) {
        AuditEntity auditEntity = new AuditEntity();
        courtCase.ifPresent(auditEntity::setCourtCase);
        auditEntity.setAuditActivity(auditActivityRepository.getReferenceById(activity.getId()));
        auditEntity.setUser(userAccountEntity);
        additionalData.ifPresent(auditEntity::setAdditionalData);
        auditEntity.setCreatedBy(userAccountEntity);
        auditEntity.setLastModifiedBy(userAccountEntity);
        auditRepository.saveAndFlush(auditEntity);
    }
}