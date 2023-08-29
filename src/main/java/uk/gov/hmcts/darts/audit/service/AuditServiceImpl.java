package uk.gov.hmcts.darts.audit.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.model.AuditSearchQuery;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity_;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
@Slf4j
public class AuditServiceImpl implements AuditService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<AuditEntity> search(AuditSearchQuery auditSearchQuery) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditEntity> criteriaQuery = criteriaBuilder.createQuery(AuditEntity.class);
        Root<AuditEntity> root = criteriaQuery.from(AuditEntity.class);

        List<Predicate> predicates = getPredicates(auditSearchQuery, criteriaBuilder, root);
        Predicate finalAndPredicate = criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        criteriaQuery.where(finalAndPredicate);
        TypedQuery<AuditEntity> query = entityManager.createQuery(criteriaQuery);

        return query.getResultList();

    }

    private List<Predicate> getPredicates(AuditSearchQuery auditSearchQuery, CriteriaBuilder criteriaBuilder, Root<AuditEntity> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (auditSearchQuery.getCaseId() != null) {

            predicates.add(criteriaBuilder.equal(root.get(AuditEntity_.caseId), auditSearchQuery.getCaseId()));
        }

        if (auditSearchQuery.getFromDate() != null && auditSearchQuery.getToDate() != null) {
            predicates.add(criteriaBuilder.between(
                root.get(AuditEntity_.createdDateTime),
                auditSearchQuery.getFromDate(),
                auditSearchQuery.getToDate()
            ));
        }

        if (auditSearchQuery.getEventId() != null) {
            predicates.add(criteriaBuilder.equal(root.get(AuditEntity_.auditActivity), auditSearchQuery.getEventId()));
        }
        return predicates;
    }
}
