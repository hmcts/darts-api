package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TranscriptionRepository extends RevisionRepository<TranscriptionEntity, Integer, Long>, JpaRepository<TranscriptionEntity, Integer> {

    @Query(value = """
        SELECT *
        FROM darts.transcription tr
        WHERE tr.tra_id IN (
            SELECT ht.tra_id
            FROM darts.hearing h
            JOIN darts.hearing_transcription_ae ht ON ht.hea_id = h.hea_id
            WHERE h.cas_id = :caseId
            UNION
            SELECT ct.tra_id
            FROM darts.court_case cc
            JOIN darts.case_transcription_ae ct ON ct.cas_id = cc.cas_id
            WHERE cc.cas_id = :caseId
        )
        AND (
            tr.is_manual_transcription = true OR tr.transcription_object_id IS NOT NULL
        )
        AND (
            :includeHidden = true
            OR
            EXISTS (
                SELECT 1
                FROM darts.transcription_document trd
                WHERE trd.tra_id = tr.tra_id
                AND trd.is_hidden = false
            )
            OR
            NOT EXISTS (
                SELECT 1
                FROM darts.transcription_document trd
                WHERE trd.tra_id = tr.tra_id
            )
        )
        """, nativeQuery = true
    )
    List<TranscriptionEntity> findByCaseIdManualOrLegacy(Integer caseId, Boolean includeHidden);

    @Query("""
           SELECT te.id
           FROM TranscriptionEntity te
           WHERE te.transcriptionStatus NOT IN (:transcriptionStatuses)
           and te.createdDateTime <= :createdDateTime
        """)
    List<Integer> findAllByTranscriptionStatusNotInWithCreatedDateTimeBefore(
        List<TranscriptionStatusEntity> transcriptionStatuses, OffsetDateTime createdDateTime, Limit limit);

    // native query to bypass @SQLRestriction on TranscriptionDocumentEntity in NOT EXISTS sub-query
    @Query(value = """
        SELECT t.*
        FROM darts.transcription t
        JOIN darts.hearing_transcription_ae ht ON ht.tra_id = t.tra_id
        JOIN darts.hearing h ON h.hea_id = ht.hea_id
        WHERE h.hea_id = :hearingId
        AND (t.is_manual_transcription = true OR t.transcription_object_id IS NOT NULL)
        AND (EXISTS (
            SELECT 1
            FROM darts.transcription_document td
            WHERE td.tra_id = t.tra_id
            AND td.is_hidden = false
        ) OR NOT EXISTS (
            SELECT 1
            FROM darts.transcription_document td
            WHERE td.tra_id = t.tra_id
        ))
        ORDER BY t.created_ts desc
        """, nativeQuery = true
    )
    List<TranscriptionEntity> findByHearingIdManualOrLegacyIncludeDeletedTranscriptionDocuments(Integer hearingId);

    @Query("""
        SELECT t
        FROM TranscriptionEntity t
        JOIN t.hearings hearing
        WHERE hearing.id = :hearingId
        AND t.transcriptionType = :transcriptionType
        AND (t.startTime IS NULL OR t.startTime = :startTime)
        AND (t.endTime IS NULL OR t.endTime = :endTime)
        AND t.isManualTranscription = :isManual
        AND t.transcriptionStatus NOT IN (:ignoreStatuses)
        """)
    List<TranscriptionEntity> findByHearingIdTypeStartAndEndAndIsManualAndNotStatus(
        Integer hearingId,
        TranscriptionTypeEntity transcriptionType,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Boolean isManual,
        List<TranscriptionStatusEntity> ignoreStatuses
    );

    List<TranscriptionEntity> findByIdIn(List<Integer> transcriptionIds);

    @Query("""
         SELECT new uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult(
             t.id,
             cc.caseNumber,
             cth.id,
             h.hearingDate,
             t.createdDateTime,
             ts.id,
             t.isManualTranscription,
             (SELECT MAX(w.workflowTimestamp) FROM TranscriptionWorkflowEntity w WHERE w.transcription = t AND 
              w.transcriptionStatus = :transcriptionStatus) as approvedAt)
         FROM TranscriptionEntity t
         JOIN t.hearings h
         JOIN h.courtCase cc
         JOIN h.courtroom cr
         JOIN cr.courthouse cth
         JOIN t.transcriptionStatus ts
         JOIN t.createdBy ua
         WHERE (:ids IS NULL OR t.id IN :ids)
             AND (:caseNumber IS NULL OR cc.caseNumber = :caseNumber)
             AND (cth.displayName ILIKE CONCAT('%', :courthouseDisplayNamePattern, '%') OR :courthouseDisplayNamePattern IS NULL)
             AND (cast(:hearingDate as LocalDate) IS NULL OR :hearingDate = h.hearingDate)
             AND (cast(:createdFrom as TIMESTAMP) IS NULL OR t.createdDateTime >= :createdFrom)
             AND (cast(:createdTo as TIMESTAMP) IS NULL OR t.createdDateTime <= :createdTo)
             AND (:isManual IS NULL OR t.isManualTranscription = :isManual)
             AND (ua.userFullName ILIKE CONCAT('%', :requestedBy, '%') OR :requestedBy IS NULL)
             AND t.isCurrent = true
        """)
    List<TranscriptionSearchResult> searchModernisedTranscriptionsFilteringOn(
        List<Integer> ids,
        String caseNumber,
        String courthouseDisplayNamePattern,
        LocalDate hearingDate,
        OffsetDateTime createdFrom,
        OffsetDateTime createdTo,
        Boolean isManual,
        String requestedBy,
        TranscriptionStatusEntity transcriptionStatus);


    @Query("""
         SELECT new uk.gov.hmcts.darts.transcriptions.model.TranscriptionSearchResult(
             t.id,
             cc.caseNumber,
             cth.id,
             t.hearingDate,
             t.createdDateTime,
             ts.id,
             t.isManualTranscription,
             (SELECT MAX(w.workflowTimestamp) FROM TranscriptionWorkflowEntity w WHERE w.transcription = t AND 
              w.transcriptionStatus = :transcriptionStatus) as approvedAt)
         FROM TranscriptionEntity t
         JOIN t.transcriptionStatus ts
         JOIN t.createdBy ua
         LEFT JOIN t.courtCases cc
         LEFT JOIN cc.courthouse cth
         WHERE (:ids IS NULL OR t.id IN :ids)
             AND (:caseNumber IS NULL OR cc.caseNumber = :caseNumber)
             AND (cth.displayName ILIKE CONCAT('%', :courthouseDisplayNamePattern, '%') OR :courthouseDisplayNamePattern IS NULL)
             AND (cast(:hearingDate as LocalDate) IS NULL OR :hearingDate = t.hearingDate)
             AND (cast(:createdFrom as TIMESTAMP) IS NULL OR t.createdDateTime >= :createdFrom)
             AND (cast(:createdTo as TIMESTAMP) IS NULL OR t.createdDateTime <= :createdTo)
             AND (:isManual IS NULL OR t.isManualTranscription = :isManual)
             AND (ua.userFullName ILIKE CONCAT('%', :requestedBy, '%') OR :requestedBy IS NULL)
             AND t.isCurrent = true
        """)
    List<TranscriptionSearchResult> searchMigratedTranscriptionsFilteringOn(
        List<Integer> ids,
        String caseNumber,
        String courthouseDisplayNamePattern,
        LocalDate hearingDate,
        OffsetDateTime createdFrom,
        OffsetDateTime createdTo,
        Boolean isManual,
        String requestedBy,
        TranscriptionStatusEntity transcriptionStatus);

    @Query("""
        SELECT distinct t
        FROM TranscriptionEntity t
        WHERE t.requestedBy.id = :userId
        AND ((cast(:onOrAfterCreatedDate as TIMESTAMP)) IS NULL OR t.createdDateTime >= :onOrAfterCreatedDate)
        UNION
        SELECT distinct trans 
        FROM TranscriptionWorkflowEntity twfe
        JOIN twfe.transcription trans
        JOIN twfe.workflowActor user
        WHERE user.id = :userId
        AND ((cast(:onOrAfterCreatedDate as TIMESTAMP)) IS NULL OR trans.createdDateTime >= :onOrAfterCreatedDate)
        """)
    List<TranscriptionEntity> findTranscriptionForUserOnOrAfterDate(Integer userId, OffsetDateTime onOrAfterCreatedDate);
}