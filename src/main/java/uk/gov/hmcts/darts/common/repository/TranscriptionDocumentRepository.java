package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TranscriptionDocumentRepository extends JpaRepository<TranscriptionDocumentEntity, Integer> {

    @Query("""
        SELECT new uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResponse(tmd.id, t.id,
        courtCase.id,
        courtCase.caseNumber,
        courthouse.id,
        courthouse.displayName,
        hearing.id,
        hearing.hearingDate,
        t.isManualTranscription,
        tmd.isHidden)
            FROM TranscriptionDocumentEntity tmd,
            JOIN tmd.transcription t
            JOIN t.hearings hearing
            JOIN t.courtCase courtCase
            JOIN t.courtroom courtroom
            JOIN courtroom.courthouse courthouse
        WHERE
           (:mediaId IS NULL OR (:mediaId IS NOT NULL AND media.id=:mediaId)) AND
           (:caseNumber IS NULL  OR (:caseNumber IS NOT NULL AND courtCase.caseNumber=:caseNumber)) AND
           (:courtHouseDisplayName IS NULL OR (:courtHouseDisplayName IS NOT NULL AND courthouse.displayName ILIKE CONCAT('%', :courtHouseDisplayName, '%'))) AND
           (:hearingDate IS NULL OR (:hearingDate IS NOT NULL AND hearing.hearingDate=:hearingDate )) AND
           (:isManualTranscription IS NULL OR (:isManualTranscription IS NOT NULL AND tisManualTranscription=:isManualTranscription)) AND
           (:owner IS NULL OR (:owner IS NOT NULL AND media.currentOwner.userFullName ILIKE CONCAT('%', :owner, '%'))) AND
           (:requestedBy IS NULL OR (:requestedBy IS NOT NULL AND tm.createdBy.userFullName ILIKE CONCAT('%', :requestedBy, '%'))) AND
           ((cast(:requestedAtFrom as TIMESTAMP)) IS NULL OR (:requestedAtFrom IS NOT NULL AND media.createdDateTime >= :requestedAtFrom)) AND
           ((cast(:requestedAtTo as TIMESTAMP)) IS NULL OR (:requestedAtTo IS NOT NULL AND media.createdDateTime <= :requestedAtTo))
           """)
    List<TranscriptionDocumentEntity> findTranscriptionMedia(String caseNumber,
                                                           String courtHouseDisplayName,
                                                           LocalDate hearingDate,
                                                           String owner,
                                                           String requestedBy,
                                                           OffsetDateTime requestedAtFrom,
                                                           OffsetDateTime requestedAtTo,
                                                           Boolean isManualTranscription);
}