package uk.gov.hmcts.darts.common.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArmRpoExecutionDetailRepository extends JpaRepository<ArmRpoExecutionDetailEntity, Integer> {

    @Query("""
        SELECT ared
        FROM ArmRpoExecutionDetailEntity ared
        ORDER BY ared.createdDateTime DESC
        LIMIT 1
        """)
    Optional<ArmRpoExecutionDetailEntity> findLatestByCreatedDateTimeDesc();

    @Query("""
        SELECT ared
        FROM ArmRpoExecutionDetailEntity ared
        WHERE ared.armRpoState = :armRpoStateEntity
        AND ared.armRpoStatus = :armRpoStatusEntity
        ORDER BY ared.createdDateTime DESC
        LIMIT 1
        """)
    Optional<ArmRpoExecutionDetailEntity> findLatestByCreatedDateTimeDescWithStateAndStatus(ArmRpoStateEntity armRpoStateEntity,
                                                                                            ArmRpoStatusEntity armRpoStatusEntity);
    
    @Query("""
        SELECT ared.id
        FROM ArmRpoExecutionDetailEntity ared
        WHERE ared.armRpoStatus = :armRpoStatusEntity
        AND ared.lastModifiedDateTime < :cutoffDateTime
        """)
    List<Integer> findIdsByStatusAndLastModifiedDateTimeAfter(ArmRpoStatusEntity armRpoStatusEntity, OffsetDateTime cutoffDateTime);

    @Modifying
    @Transactional
    @Query("""
        UPDATE ArmRpoExecutionDetailEntity ared
        SET ared.lastModifiedDateTime = :newLastModifiedDateTime
        WHERE ared.id = :id
        """)
    void updateLastModifiedDateTimeById(Integer id, OffsetDateTime newLastModifiedDateTime);
}
