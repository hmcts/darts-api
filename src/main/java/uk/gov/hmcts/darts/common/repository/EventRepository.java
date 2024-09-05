package uk.gov.hmcts.darts.common.repository;

import jakarta.persistence.Column;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.EventSearchResult;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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
            e.createdDateTime,
            et.eventName,
            e.eventText,
            e.chronicleId,
            e.antecedentId,
            ch.id,
            ch.displayName,
            c.id,
            c.name)
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
        """)
    Page<EventSearchResult> searchEventsFilteringOn(
        List<Integer> courthouseIds,
        String caseNumber,
        String courtroomName,
        LocalDate hearingStartDate,
        LocalDate hearingEndDate,
        Pageable pageable);

    @Query(value = """
        SELECT event_id
        FROM  darts.event
        WHERE is_current=true
        AND event_id <> 0
        AND event_id IS NOT NULL
        GROUP BY event_id
        HAVING count(event_id) > 1
        """, nativeQuery = true)
    List<Integer> getCurrentEventIdsToBeProcessed(Pageable pageable);

    @Query(value = """
         SELECT e.eve_id, event_id, string_agg(he.hea_id::varchar, ',' order by he.hea_id) as hearing_ids FROM darts.event e
         left join darts.hearing_event_ae he
         on he.eve_id = e.eve_id
         WHERE e.event_id=:eventId
         group by e.eve_id, event_id
         ORDER BY created_ts desc
         LIMIT 1
        """, nativeQuery = true)
    EventIdAndHearingIds getTheLatestCreatedEventPrimaryKeyForTheEventId(Integer eventId);

    /**
     *  string_agg(he.hea_id::varchar, ',' order by he.hea_id) to ensure we only update the events that have the same hearing ids
     *  This is done by reading the hearing ids from the latest event created and only updating the events that have the same hearing ids
     * @param eventIdsPrimaryKey the primary key of the event that is the latest created
     * @param eventId the event id that we want to close old events for
     * @param hearingIds the hearing ids that we want to close old events for (Should match hearing ids of the latest event created)
     */
    @Transactional
    @Modifying
    @Query(value = """
        UPDATE darts.event e
            SET is_current = false
        FROM (
           select string_agg(he.hea_id::varchar, ',' order by he.hea_id) as hearing_ids FROM darts.event e
           left join darts.hearing_event_ae he
            on he.eve_id = e.eve_id
            where event_id=:eventId
        ) h WHERE e.eve_id != :eventIdsPrimaryKey
                AND e.event_id = :eventId
                and h.hearing_ids = :hearingIds
        """, nativeQuery = true)
    void updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
        Integer eventIdsPrimaryKey, Integer eventId, String hearingIds);

    @Query("""
        SELECT ee
        FROM EventEntity ee
        WHERE ee.timestamp >= :startDateTime
        AND ee.timestamp <= :endDateTime
        """)
    List<EventEntity> findAllBetweenDateTimesInclusive(OffsetDateTime startDateTime, OffsetDateTime endDateTime);

    interface EventIdAndHearingIds {

        @Column(name = "eve_id")
        Integer getEveId();

        @Column(name = "event_id")
        Integer getEventId();

        @Column(name = "hearing_ids")
        String getHearingIds();
    }
}