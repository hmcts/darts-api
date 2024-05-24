package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResult;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TranscriptionDocumentRepository extends JpaRepository<TranscriptionDocumentEntity, Integer> {

    @Query("""
            SELECT new uk.gov.hmcts.darts.transcriptions.model.TranscriptionDocumentResult(tmd.id, t.id,
            courtCase.id,
            courtCase.caseNumber,
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
                 LEFT JOIN t.courtroom courtroom
                 LEFT JOIN courtroom.courthouse courthouse
                 LEFT JOIN hearings.courtroom.courthouse hearingcourthouse
                 LEFT JOIN t.transcriptionWorkflowEntities wf
                 LEFT JOIN wf.workflowActor wfa
             WHERE
                (:caseNumber IS NULL OR (:caseNumber IS NOT NULL AND (courtCase.caseNumber=:caseNumber)))AND
                (:courtHouseDisplayName IS NULL OR (:courtHouseDisplayName IS NOT NULL 
                AND (courthouse.displayName ILIKE CONCAT('%', :courtHouseDisplayName, '%') 
                OR (hearingcourthouse.displayName ILIKE CONCAT('%', :courtHouseDisplayName, '%'))))) AND
                (:hearingDate IS NULL OR (:hearingDate IS NOT NULL AND hearings.hearingDate=:hearingDate ))AND
                (:isManualTranscription IS NULL OR (:isManualTranscription IS NOT NULL AND t.isManualTranscription=:isManualTranscription)) AND
                (:requestedBy IS NULL OR (:requestedBy IS NOT NULL AND t.createdBy.userFullName ILIKE CONCAT('%', :requestedBy, '%')))AND
                ((cast(:requestedAtFrom as TIMESTAMP)) IS NULL OR (:requestedAtFrom IS NOT NULL AND t.createdDateTime >= :requestedAtFrom)) AND
                (:owner IS NULL OR (:owner IS NOT NULL AND wfa.userFullName ILIKE CONCAT('%', :owner, '%'))) AND
                ((cast(:requestedAtTo as TIMESTAMP)) IS NULL OR (:requestedAtTo IS NOT NULL AND t.createdDateTime <= :requestedAtTo))
           """)
    List<TranscriptionDocumentResult> findTranscriptionMedia(String caseNumber,
                                                             String courtHouseDisplayName,
                                                             LocalDate hearingDate,
                                                             String requestedBy,
                                                             OffsetDateTime requestedAtFrom,
                                                             OffsetDateTime requestedAtTo,
                                                             Boolean isManualTranscription,
                                                             String owner);
}