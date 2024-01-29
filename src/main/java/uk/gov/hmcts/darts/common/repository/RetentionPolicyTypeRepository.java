package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface RetentionPolicyTypeRepository extends JpaRepository<RetentionPolicyTypeEntity, Integer> {

    @Query("""
        SELECT r
        FROM RetentionPolicyTypeEntity r
        WHERE fixedPolicyKey = :fixedPolicyKey
        and policyStart <= :currentTime
        and (policyEnd is null or policyEnd > :currentTime)
        """
    )
    Optional<RetentionPolicyTypeEntity> findCurrentWithFixedPolicyKey(String fixedPolicyKey, OffsetDateTime currentTime);

}
