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

    @Query("""
          SELECT t
          FROM TranscriptionEntity t
          join t.courtCases case
          WHERE case.id = :caseId
          ORDER BY t.createdDateTime
          """
    )
    List<TranscriptionEntity> findByCaseId(Integer caseId);

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
          join t.hearings hearing
          WHERE hearing.id = :hearingId
          ORDER BY t.createdDateTime
          """
    )
    List<TranscriptionEntity> findByHearingId(Integer hearingId);

    @Query("""
          SELECT t
          FROM TranscriptionEntity t
          join t.hearings hearing
          WHERE hearing.id = :hearingId
          AND t.transcriptionType = :transcriptionType
          AND t.startTime = :startTime
          AND t.endTime = :endTime
          AND t.isManualTranscription = :isManual
          """)
    List<TranscriptionEntity> findByHearingIdTypeStartAndEndAndIsManual(
          Integer hearingId,
          TranscriptionTypeEntity transcriptionType,
          OffsetDateTime startTime,
          OffsetDateTime endTime,
          Boolean isManual
    );

    List<TranscriptionEntity> findByIdIn(List<Integer> transcriptionIds);
}
