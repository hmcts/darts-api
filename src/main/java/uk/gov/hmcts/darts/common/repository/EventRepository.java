package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.EventEntity;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Integer> {

    @Query("""
           SELECT ee FROM EventEntity ee, HearingEntity he
           JOIN he.eventList
           WHERE he.id = :hearingId
           """)
    List<EventEntity> findAllByHearingId(Integer hearingId);
}
