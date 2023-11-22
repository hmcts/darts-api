package uk.gov.hmcts.darts.usermanagement.component.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity_;
import uk.gov.hmcts.darts.usermanagement.component.UserSearchQuery;

import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
@RequiredArgsConstructor
public class UserSearchQueryImpl implements UserSearchQuery {

    private static final String LIKE_CONTAINS_STRING_VALUE = "%%%s%%";

    private final EntityManager em;

    public List<UserAccountEntity> getUsers(String fullName, String emailAddress) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<UserAccountEntity> criteriaQuery = criteriaBuilder.createQuery(UserAccountEntity.class);

        Root<UserAccountEntity> root = criteriaQuery.from(UserAccountEntity.class);
        criteriaQuery.select(root);

        List<Predicate> wherePredicates = new ArrayList<>();
        ParameterExpression<String> paramEmailAddress = criteriaBuilder.parameter(String.class);
        ParameterExpression<String> paramFullName = criteriaBuilder.parameter(String.class);

        boolean isNotBlankEmailAddress = isNotBlank(emailAddress);
        if (isNotBlankEmailAddress) {
            wherePredicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(root.get(UserAccountEntity_.emailAddress)),
                criteriaBuilder.lower(paramEmailAddress)
            ));
        }

        boolean isNotBlankFullName = isNotBlank(fullName);
        if (isNotBlankFullName) {
            wherePredicates.add(criteriaBuilder.like(
                criteriaBuilder.lower(root.get(UserAccountEntity_.userName)),
                criteriaBuilder.lower(paramFullName)
            ));
        }

        Predicate finalWherePredicate = criteriaBuilder.and(wherePredicates.toArray(Predicate[]::new));
        criteriaQuery.where(finalWherePredicate);
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get(UserAccountEntity_.id)));

        TypedQuery<UserAccountEntity> query = em.createQuery(criteriaQuery);
        if (isNotBlankEmailAddress) {
            query.setParameter(paramEmailAddress, String.format(LIKE_CONTAINS_STRING_VALUE, emailAddress));
        }
        if (isNotBlankFullName) {
            query.setParameter(paramFullName, String.format(LIKE_CONTAINS_STRING_VALUE, fullName));
        }
        return query.getResultList();
    }

}
