package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;

import java.util.List;

@Repository
public interface CaseManagementRetentionRepository extends JpaRepository<CaseManagementRetentionEntity, Integer> {

    @Transactional
    @Modifying
    @Query("DELETE FROM CaseManagementRetentionEntity c WHERE c.eventEntity.id IN :eventsIds")
    void deleteAllByEventEntityIn(List<Long> eventsIds);

    @Query("SELECT c.id FROM CaseManagementRetentionEntity c WHERE c.eventEntity.id IN :eventsIds")
    List<Integer> getIdsForEvents(@Param("eventsIds") List<Long> eventsIds);
}
