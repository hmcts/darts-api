package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.EventEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface CourtLogEventRepository extends JpaRepository<EventEntity, Integer> {

    @Query("""
           SELECT ee FROM EventEntity ee, CourtroomEntity cr, CourthouseEntity ch, CourtCaseEntity ce, HearingEntity he
           JOIN ee.hearingEntities
           WHERE upper(ch.courthouseName) = upper(:courtHouse)
           AND upper(ce.caseNumber) = upper(:caseNumber)
           AND ee.eventName = upper('LOG')
           AND ee.timestamp between :start AND :end
           AND cr.courthouse = ch
           AND he.courtroom = cr
           AND he.courtCase = ce
        """)
    List<EventEntity> findByCourthouseAndCaseNumberBetweenStartAndEnd(String courtHouse, String caseNumber, OffsetDateTime start, OffsetDateTime end);

}
