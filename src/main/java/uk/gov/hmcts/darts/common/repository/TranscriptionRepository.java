package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TranscriptionRepository extends JpaRepository<TranscriptionEntity, Integer> {
    @Query("""
        SELECT t FROM TranscriptionEntity t, CourtCaseEntity case
        WHERE case.id = :caseId
        AND t.courtCase = case
        """
    )
    List<TranscriptionEntity> findByCaseId(Integer caseId);
    @Query("""
           SELECT te
           FROM TranscriptionEntity te
           WHERE te.transcriptionStatus not in (:transcriptionStatuses)
           and te.createdDateTime <= :createdDateTime
        """)
    List<TranscriptionEntity> findAllByTranscriptionStatusNotInWithCreatedDateTimeBefore(
        List<TranscriptionStatusEntity> transcriptionStatuses, OffsetDateTime createdDateTime);
}
