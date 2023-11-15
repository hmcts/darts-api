package uk.gov.hmcts.darts.audit.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.model.AuditSearchQuery;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.common.entity.AuditActivityEntity_;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity_;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity_;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AuditActivityRepository;
import uk.gov.hmcts.darts.common.repository.AuditRepository;

import java.time.OffsetDateTime;
import java.util.List;

@AllArgsConstructor
@Service
@Slf4j
public class AuditServiceImpl implements AuditService {
    private AuditActivityRepository auditActivityRepository;
    private AuditRepository auditRepository;

    @Transactional
    @Override
    public void recordAudit(AuditActivity activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase) {
        AuditEntity auditEntity = new AuditEntity();
        auditEntity.setCourtCase(courtCase);
        auditEntity.setAuditActivity(auditActivityRepository.getReferenceById(activity.getId()));
        auditEntity.setUser(userAccountEntity);
        auditEntity.setApplicationServer("not available");
        auditEntity.setAdditionalData(null);
        auditEntity.setCreatedBy(userAccountEntity);
        auditEntity.setLastModifiedBy(userAccountEntity);
        auditRepository.saveAndFlush(auditEntity);
    }

    @Override
    public List<AuditEntity> search(AuditSearchQuery auditSearchQuery) {
        Specification<AuditEntity> specification = isCourtCase(auditSearchQuery.getCaseId())
            .and(isWithInDates(auditSearchQuery.getFromDate(), auditSearchQuery.getToDate()))
            .and(isAuditActivity(auditSearchQuery.getAuditActivityId()));

        return auditRepository.findAll(specification);
    }

    private Specification<AuditEntity> isCourtCase(Integer courtCaseId) {
        return (root, query, criteriaBuilder) -> {
            if (courtCaseId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(AuditEntity_.courtCase).get(CourtCaseEntity_.id), courtCaseId);
        };

    }

    private Specification<AuditEntity> isWithInDates(OffsetDateTime fromDate, OffsetDateTime toDate) {
        return (root, query, criteriaBuilder) -> {
            if (fromDate == null || toDate == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.between(
                root.get(AuditEntity_.createdDateTime),
                fromDate,
                toDate
            );
        };

    }

    private Specification<AuditEntity> isAuditActivity(Integer auditActivityId) {
        return (root, query, criteriaBuilder) -> {
            if (auditActivityId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(
                root.get(AuditEntity_.auditActivity).get(AuditActivityEntity_.id),
                auditActivityId
            );
        };

    }
}
