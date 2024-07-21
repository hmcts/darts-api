package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
        GROUP BY event_id
        HAVING count(event_id) > 1
        """, nativeQuery = true)
    List<Integer> getCurrentEventIdsToBeProcessed(Pageable pageable);

    @Query(value = """
         SELECT eve_id FROM darts.event e
         WHERE event_id=:eventId
         ORDER BY created_ts desc
         LIMIT 1
        """, nativeQuery = true)
    Integer getTheLatestCreatedEventPrimaryKeyForTheEventId(Integer eventId);

    @Transactional
    @Modifying
    @Query(value = """
        UPDATE EventEntity
                SET isCurrent = false
                WHERE id not in :eventIdsPrimaryKeysLst AND eventId in :eventIdLst
        """)
    void updateAllEventIdEventsToNotCurrentWithTheExclusionOfTheCurrentEventPrimaryKey(
        List<Integer> eventIdsPrimaryKeysLst, List<Integer> eventIdLst);

    @EntityGraph(attributePaths = {"id", "timestamp", "eventId", "messageId", "eventText"})
    @Query("""
        SELECT ee
        FROM EventEntity ee
        WHERE ee.timestamp >= :startDateTime
        AND ee.timestamp <= :endDateTime
        """)
    List<EventEntity> findAllForDuplicateProcessing(OffsetDateTime startDateTime, OffsetDateTime endDateTime);
}