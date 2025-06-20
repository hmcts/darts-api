package uk.gov.hmcts.darts.usermanagement.component.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity_;
import uk.gov.hmcts.darts.usermanagement.component.UserSearchQuery;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@RequiredArgsConstructor
public class UserSearchQueryImpl implements UserSearchQuery {

    private static final String LIKE_CONTAINS_STRING_VALUE = "%%%s%%";

    private final EntityManager em;

    @Override
    public List<UserAccountEntity> getUsers(boolean includeSystemUsers, String fullName, String emailAddress, Boolean active) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<UserAccountEntity> criteriaQuery = criteriaBuilder.createQuery(UserAccountEntity.class);

        Root<UserAccountEntity> root = criteriaQuery.from(UserAccountEntity.class);
        criteriaQuery.select(root);

        List<Predicate> wherePredicates = new ArrayList<>();
        if (!includeSystemUsers) {
            wherePredicates.add(criteriaBuilder.isFalse(root.get(UserAccountEntity_.isSystemUser)));
        }
        ParameterExpression<String> paramEmailAddress = criteriaBuilder.parameter(String.class);
        ParameterExpression<String> paramFullName = criteriaBuilder.parameter(String.class);
        ParameterExpression<Boolean> paramActive = criteriaBuilder.parameter(Boolean.class);

        boolean isNotBlankEmailAddress = isNotBlank(emailAddress);
        if (isNotBlankEmailAddress) {
            wherePredicates.add(criteriaBuilder.like(
                criteriaBuilder.upper(root.get(UserAccountEntity_.emailAddress)),
                criteriaBuilder.upper(paramEmailAddress)
            ));
        }

        boolean isNotBlankFullName = isNotBlank(fullName);
        if (isNotBlankFullName) {
            wherePredicates.add(criteriaBuilder.like(
                criteriaBuilder.upper(root.get(UserAccountEntity_.userFullName)),
                criteriaBuilder.upper(paramFullName)
            ));
        }

        boolean activeNonNull = nonNull(active);
        if (activeNonNull) {
            Path<Boolean> path = root.get(UserAccountEntity_.active);
            Predicate activePredicate = criteriaBuilder.equal(paramActive, path);
            wherePredicates.add(activePredicate);
        }

        Predicate finalWherePredicate = criteriaBuilder.and(wherePredicates.toArray(Predicate[]::new));
        criteriaQuery.where(finalWherePredicate);
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get(UserAccountEntity_.userFullName)));

        TypedQuery<UserAccountEntity> query = em.createQuery(criteriaQuery);
        if (isNotBlankEmailAddress) {
            query.setParameter(paramEmailAddress, String.format(LIKE_CONTAINS_STRING_VALUE, emailAddress));
        }
        if (isNotBlankFullName) {
            query.setParameter(paramFullName, String.format(LIKE_CONTAINS_STRING_VALUE, fullName));
        }
        if (activeNonNull) {
            query.setParameter(paramActive, active);

        }
        return query.getResultList();
    }

}
