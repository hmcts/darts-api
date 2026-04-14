package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;

import java.util.List;
import java.util.Optional;

public interface ObjectStateRecordRepository extends JpaRepository<ObjectStateRecordEntity, Long> {
    Optional<ObjectStateRecordEntity> findByArmEodId(long armEodId);

    @Modifying
    @Query("UPDATE ObjectStateRecordEntity o SET o.flagFileDetsCleanupStatus = true where o.uuid in :uuids")
    void markDetsCleanupStatusAsComplete(List<Long> uuids);
}
