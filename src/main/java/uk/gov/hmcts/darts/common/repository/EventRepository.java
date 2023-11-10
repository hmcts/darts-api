package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.EventEntity;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Integer> {

    @Query("""
           SELECT ee
           FROM EventEntity ee
           JOIN ee.hearingEntities he
           WHERE he.id = :hearingId
           ORDER by ee.timestamp
        """)
    List<EventEntity> findAllByHearingId(Integer hearingId);
}
