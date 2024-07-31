package uk.gov.hmcts.darts.common.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;

import java.util.List;

@Repository
public interface CaseManagementRetentionRepository extends JpaRepository<CaseManagementRetentionEntity, Integer> {

    @Transactional
    void deleteAllByEventEntityIn(List<EventEntity> events);

    @Query("SELECT c.id FROM CaseManagementRetentionEntity c WHERE c.eventEntity IN :events")
    List<Integer> getIdsForEvents(@Param("events") List<EventEntity> events);
}
