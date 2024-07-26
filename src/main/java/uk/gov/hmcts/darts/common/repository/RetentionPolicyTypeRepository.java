package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetentionPolicyTypeRepository extends
    RevisionRepository<RetentionPolicyTypeEntity, Integer, Long>,
    JpaRepository<RetentionPolicyTypeEntity, Integer> {

    @Query("""
        SELECT r
        FROM RetentionPolicyTypeEntity r
        WHERE fixedPolicyKey = :fixedPolicyKey
        and policyStart <= :currentTime
        and (policyEnd is null or policyEnd > :currentTime)
        """
    )
    List<RetentionPolicyTypeEntity> findCurrentWithFixedPolicyKey(String fixedPolicyKey, OffsetDateTime currentTime);

    Optional<RetentionPolicyTypeEntity> findFirstByFixedPolicyKeyOrderByPolicyStartDesc(String fixedPolicyKey);

    List<RetentionPolicyTypeEntity> findByFixedPolicyKeyOrderByPolicyStartDesc(String fixedPolicyKey);

    Optional<RetentionPolicyTypeEntity> findFirstByPolicyNameAndFixedPolicyKeyNot(String policyName, String excludingFixedPolicyKey);

    Optional<RetentionPolicyTypeEntity> findFirstByDisplayNameAndFixedPolicyKeyNot(String displayName, String excludingFixedPolicyKey);

    Optional<RetentionPolicyTypeEntity> findByFixedPolicyKeyAndPolicyEndIsNull(String fixedPolicyKey);

    List<RetentionPolicyTypeEntity> findByPolicyNameNotIn(List<String> names);

    List<RetentionPolicyTypeEntity> findByIdGreaterThanEqual(Integer threshold);
}
