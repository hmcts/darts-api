package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;

@Repository
public interface ArmRpoStateRepository extends JpaRepository<ArmRpoStateEntity, Integer> {
}
