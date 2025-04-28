package uk.gov.hmcts.darts.common.repository;

import jakarta.persistence.Column;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.model.Event;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.EventSearchResult;

import java.time.LocalDate;
import java.util.List;

@Repository
@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    @Query("""
           SELECT ee
           FROM EventEntity ee
           JOIN ee.hearingEntities he
           WHERE he.id = :hearingId
           ORDER by ee.timestamp desc
        """)
    List<EventEntity> findAllByHearingId(Integer hearingId);

    List<EventEntity> findAllByEventId(Integer eventId);

    @Query("""
           SELECT ee
           FROM EventEntity ee, CourtCaseEntity ce
           JOIN ee.hearingEntities he
           WHERE ce.id = :caseId
           AND he.courtCase = ce
          ORDER BY he.hearingDate DESC, ee.timestamp DESC
        """)
    List<EventEntity> findAllByCaseId(Integer caseId);


    @Query("""
           SELECT new uk.gov.hmcts.darts.cases.model.Event(
                  ee.id,
                  he.id,
                  he.hearingDate,
                  ee.timestamp,
                  ee.eventType.eventName,
                  ee.isDataAnonymised,
                  ee.eventText,
                  ee.courtroom.name
           )
           FROM EventEntity ee
           JOIN ee.hearingEntities he
           LEFT JOIN ee.eventType et        
           WHERE he.courtCase.id = :caseId
        """)
    Page<Event> findAllByCaseIdPaginated(Integer caseId, Pageable pageable);


    @Query("""
            SELECT EXISTS (
                SELECT 1
                FROM EventEntity ee, EventHandlerEntity ehe
                WHERE ee.eventType = ehe
                AND ehe.id = :eventHandlerId
            )
        """)
    boolean doesEventHandlerHaveEvents(int eventHandlerId);

    @Query("""
         SELECT new uk.gov.hmcts.darts.event.model.EventSearchResult(
            e.id,
            e.timestamp,
            et.eventName,
            e.eventText,
            e.isDataAnonymised,
            ch.id,
            ch.displayName,
            c.id,
            c.name,
            cc.isDataAnonymised,
            cc.dataAnonymisedTs)
         FROM EventEntity e
         JOIN e.eventType et
         JOIN e.hearingEntities h
         JOIN h.courtroom c
         JOIN h.courtCase cc
         JOIN c.courthouse ch
         WHERE (:courthouseIds IS NULL OR ch.id IN :courthouseIds)
             AND (cc.caseNumber ILIKE CONCAT('%', :caseNumber, '%') OR :caseNumber IS NULL)
             AND (c.name ILIKE CONCAT('%', :courtroomName, '%') OR :courtroomName IS NULL)
             AND (cast(:hearingStartDate as LocalDate) IS NULL OR h.hearingDate >= :hearingStartDate)
             AND (cast(:hearingEndDate as LocalDate) IS NULL OR h.hearingDate <= :hearingEndDate)
             AND e.isCurrent = true
        ORDER BY e.id DESC
        """)
    List<EventSearchResult> searchEventsFilteringOn(
        List<Integer> courthouseIds,
        String caseNumber,
        String courtroomName,
        LocalDate hearingStartDate,
        LocalDate hearingEndDate,
        Limit limit);

    @Query(value = """
        select distinct on (event_id, hearing_ids) e.* from (
            SELECT e.eve_id, event_id, e.created_ts, string_agg(he.hea_id::varchar, ',' order by he.hea_id) as hearing_ids
            FROM darts.event e
            LEFT JOIN darts.hearing_event_ae he ON he.eve_id = e.eve_id
            WHERE e.event_id=:eventId
            GROUP BY e.eve_id, event_id
        ) e ORDER BY event_id, hearing_ids, e.created_ts DESC
        """, nativeQuery = true)
    List<EventIdAndHearingIds> getTheLatestCreatedEventPrimaryKeyForTheEventId(Integer eventId);

    @Query("""
         SELECT ee
         FROM EventEntity ee
         JOIN ee.eventLinkedCaseEntities elc
         WHERE ee.eventId = :eventId
         AND elc.courtCase.id in :courtCaseIds
         AND (ee.eventId <> 0 or ee.id = :eveId)
        """)
    List<EventEntity> findAllByRelatedEvents(Long eveId, Integer eventId, List<Integer> courtCaseIds);

    @Query("""
        SELECT e.id
        FROM EventEntity e
        WHERE e.eventStatus = :statusNumber
        AND e.courtroom.name not in (:courtroomNumbers)
        """
    )
    List<Long> findAllByEventStatusAndNotCourtrooms(Integer statusNumber, List<String> courtroomNumbers, Limit limit);

    @Query("""
                        SELECT e3.id from EventEntity e3
                        JOIN (                        
                            SELECT e.eventId as eventId, e.messageId as messageId, e.eventText as eventText
                            FROM EventEntity e
                            WHERE e.eventId IS NOT NULL and e.messageId IS NOT NULL and e.eventId = :eventId
                            GROUP BY e.eventId, e.messageId, e.eventText
                            HAVING COUNT(e) > 1) e2
                         ON e2.eventId = e3.eventId and e2.messageId = e3.messageId and e2.eventText = e3.eventText
                         WHERE e3.eventId >= :eventId
                         ORDER BY e3.createdDateTime ASC  
        """)
    List<Long> findDuplicateEventIds(Integer eventId);


    @Transactional
    @Modifying
    @Query(value = "DELETE FROM darts.hearing_event_ae WHERE eve_id IN (:eventEntitiesIdsToDelete)", nativeQuery = true)
    void deleteAllAssociatedHearings(List<Long> eventEntitiesIdsToDelete);

    interface EventIdAndHearingIds {

        @Column(name = "eve_id")
        Long getEveId();

        @Column(name = "event_id")
        Integer getEventId();

        @Column(name = "hearing_ids")
        String getHearingIds();
    }
}
