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
           SELECT ee
           FROM EventEntity ee, CourtroomEntity cr, CourthouseEntity ch, CourtCaseEntity ce
           JOIN ee.hearingEntities hearing
           WHERE ch.courthouseName = upper(trim(:courtHouse))
           AND ce.caseNumber = :caseNumber
           AND ee.isLogEntry = true
           AND ee.timestamp between :start AND :end
           AND cr.courthouse = ch
           AND hearing.courtroom = cr
           AND hearing.courtCase = ce
        """)
    List<EventEntity> findByCourthouseAndCaseNumberBetweenStartAndEnd(String courtHouse, String caseNumber, OffsetDateTime start, OffsetDateTime end);

}