package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TranscriptionRepository extends JpaRepository<TranscriptionEntity, Integer> {

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
        """, nativeQuery = true
    )
    List<TranscriptionEntity> findByCaseIdManualOrLegacy(Integer caseId);

    @Query("""
           SELECT te
           FROM TranscriptionEntity te
           WHERE te.transcriptionStatus NOT IN (:transcriptionStatuses)
           and te.createdDateTime <= :createdDateTime
        """)
    List<TranscriptionEntity> findAllByTranscriptionStatusNotInWithCreatedDateTimeBefore(
        List<TranscriptionStatusEntity> transcriptionStatuses, OffsetDateTime createdDateTime);

    @Query("""
        SELECT t
        FROM TranscriptionEntity t
        JOIN t.hearings h
        WHERE h.id = :hearingId
        AND (t.isManualTranscription = true OR t.legacyObjectId IS NOT NULL)
        ORDER BY t.createdDateTime
        """
    )
    List<TranscriptionEntity> findByHearingIdManualOrLegacy(Integer hearingId);

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
}
