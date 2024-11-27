package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.task.runner.SoftDeleteRepository;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResult;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TranscriptionDocumentRepository extends JpaRepository<TranscriptionDocumentEntity, Integer>,
    SoftDeleteRepository<TranscriptionDocumentEntity, Integer> {

    @Query("""
         SELECT distinct new uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResult(tmd.id, t.id,
         courtCase.id,
         courtCase.caseNumber,
         hearingCase.id,
         hearingCase.caseNumber,
         courthouse.id,
         courthouse.displayName,
         hearingcourthouse.id,
         hearingcourthouse.displayName,
         hearings.id,
         hearings.hearingDate,
         t.isManualTranscription,
         tmd.isHidden)
              FROM TranscriptionDocumentEntity tmd
              JOIN tmd.transcription t
              LEFT JOIN t.hearings hearings
              LEFT JOIN t.courtCases courtCase
              LEFT JOIN hearings.courtCase hearingCase                 
              LEFT JOIN courtCase.courthouse courthouse
              LEFT JOIN hearings.courtroom.courthouse hearingcourthouse
              LEFT JOIN t.transcriptionWorkflowEntities wf
              LEFT JOIN wf.workflowActor wfa
          WHERE
             (:caseNumber IS NULL OR ((courtCase.caseNumber=cast(:caseNumber as text) OR hearingCase.caseNumber=cast(:caseNumber as text)))) AND
             (:courtHouseDisplayName IS NULL OR ((courthouse.displayName ILIKE CONCAT('%', cast(:courtHouseDisplayName as text), '%')
             OR (hearingcourthouse.displayName ILIKE CONCAT('%', cast(:courtHouseDisplayName as text), '%'))))) AND
             ((cast(:hearingDate AS LocalDate)) IS NULL OR (hearings.hearingDate=:hearingDate ))AND
             (:isManualTranscription IS NULL OR t.isManualTranscription=:isManualTranscription) AND
             (:requestedBy IS NULL OR (t.requestedBy.userFullName ILIKE CONCAT('%', cast(:requestedBy as text), '%')))AND
             ((cast(:requestedAtFrom as TIMESTAMP)) IS NULL OR (t.createdDateTime >= :requestedAtFrom)) AND
             (:owner IS NULL OR (wfa.userFullName ILIKE CONCAT('%', cast(:owner as text), '%'))) AND
             ((cast(:requestedAtTo as TIMESTAMP)) IS NULL OR t.createdDateTime <= :requestedAtTo)
        """)
    List<TranscriptionDocumentResult> findTranscriptionMedia(String caseNumber,
                                                             String courtHouseDisplayName,
                                                             LocalDate hearingDate,
                                                             String requestedBy,
                                                             OffsetDateTime requestedAtFrom,
                                                             OffsetDateTime requestedAtTo,
                                                             Boolean isManualTranscription,
                                                             String owner);


    @Query("""
               SELECT t
               FROM TranscriptionDocumentEntity t
               JOIN t.adminActions ae
               JOIN ae.objectHiddenReason hr
               WHERE ae.markedForManualDeletion = false AND hr.markedForDeletion = true
        """)
    List<TranscriptionDocumentEntity> getMarkedForDeletion();

    // native query to bypass @SQLRestriction
    @Query(value = "SELECT trd.* FROM darts.transcription_document trd WHERE trd.tra_id = :trdId AND trd.is_hidden = true", nativeQuery = true)
    List<TranscriptionDocumentEntity> findByTranscriptionIdAndHiddenTrueIncludeDeleted(Integer trdId);
}