package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;

@Repository
public interface ArmRpoExecutionDetailRepository extends JpaRepository<ArmRpoExecutionDetailEntity, Integer> {

    @Query("""
        SELECT ared
        FROM ArmRpoExecutionDetailEntity ared
        ORDER BY ared.createdDateTime DESC
        LIMIT 1
        """)
    ArmRpoExecutionDetailEntity findLatestByCreatedDateTimeDesc();

    @Query("""
        SELECT ared
        FROM ArmRpoExecutionDetailEntity ared
        WHERE ared.armRpoState = :armRpoStateEntity
        AND ared.armRpoStatus = :armRpoStatusEntity
        ORDER BY ared.createdDateTime DESC
        LIMIT 1
        """)
    ArmRpoExecutionDetailEntity findLatestByCreatedDateTimeDescWithStateAndStatus(ArmRpoStateEntity armRpoStateEntity, ArmRpoStatusEntity armRpoStatusEntity);

}
