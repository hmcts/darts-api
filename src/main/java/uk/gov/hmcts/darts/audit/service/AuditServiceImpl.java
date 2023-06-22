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
import uk.gov.hmcts.darts.common.entity.Audit;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Service
@Slf4j
public class AuditServiceImpl implements AuditService {

    public static final String CASE_ID = "caseId";
    public static final String CREATED_TS = "createdAt";
    public static final String EVENT_ID = "eventId";
    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public List<Audit> search(AuditSearchQuery auditSearchQuery) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Audit> criteriaQuery = criteriaBuilder.createQuery(Audit.class);
        Root<Audit> root = criteriaQuery.from(Audit.class);

        List<Predicate> predicates = getPredicates(auditSearchQuery, criteriaBuilder, root);
        Predicate finalAndPredicate = criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        criteriaQuery.where(finalAndPredicate);
        TypedQuery<Audit> query = entityManager.createQuery(criteriaQuery);

        return query.getResultList();

    }

    private List<Predicate> getPredicates(AuditSearchQuery auditSearchQuery, CriteriaBuilder criteriaBuilder, Root<Audit> root) {
        List<Predicate> predicates = new ArrayList<>();
        if (auditSearchQuery.getCaseId() != null) {
            predicates.add(criteriaBuilder.equal(root.get(CASE_ID), auditSearchQuery.getCaseId()));
        }

        if (auditSearchQuery.getFromDate() != null && auditSearchQuery.getToDate() != null) {
            predicates.add(criteriaBuilder.between(root.get(CREATED_TS), auditSearchQuery.getFromDate(),
                                                   auditSearchQuery.getToDate()));
        }

        if (auditSearchQuery.getEventId() != null) {
            predicates.add(criteriaBuilder.equal(root.get(EVENT_ID), auditSearchQuery.getEventId()));
        }
        return predicates;
    }
}
