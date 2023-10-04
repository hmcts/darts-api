package uk.gov.hmcts.darts.cases.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity_;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TempService {

    private final EntityManager entityManager;

    public List<CourthouseEntity> getAuthorisedCourthouses(String userEmailAddress) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<CourthouseEntity> query = criteriaBuilder.createQuery(CourthouseEntity.class);

        Root<SecurityGroupEntity> securityGroupRoot = query.from(SecurityGroupEntity.class);
        Join<SecurityGroupEntity, CourthouseEntity> courthouseJoin = securityGroupRoot.join(SecurityGroupEntity_.COURTHOUSE_ENTITIES);

        query.select(courthouseJoin);

        Root<UserAccountEntity> userAccountRoot = query.from(UserAccountEntity.class);
        query.where(criteriaBuilder.equal(userAccountRoot.get("emailAddress"), userEmailAddress));

        return entityManager.createQuery(query)
            .getResultList();
    }

}
