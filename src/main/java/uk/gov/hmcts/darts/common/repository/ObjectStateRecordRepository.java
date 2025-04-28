package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;

import java.util.Optional;

public interface ObjectStateRecordRepository extends JpaRepository<ObjectStateRecordEntity, Long> {
    Optional<ObjectStateRecordEntity> findByArmEodId(long armEodId);
}
