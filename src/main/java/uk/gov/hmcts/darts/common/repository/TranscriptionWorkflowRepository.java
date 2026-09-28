package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionIdsAndLatestWorkflowTs;

import java.util.List;

@Repository
public interface TranscriptionWorkflowRepository extends
    RevisionRepository<TranscriptionWorkflowEntity, Integer, Long>,
    JpaRepository<TranscriptionWorkflowEntity, Integer> {

    List<TranscriptionWorkflowEntity> findByTranscriptionOrderByWorkflowTimestampDesc(TranscriptionEntity transcription);

    @Query("""
            SELECT new uk.gov.hmcts.darts.transcriptions.model.TranscriptionIdsAndLatestWorkflowTs(tw.transcription.id, MAX(tw.workflowTimestamp))
            FROM TranscriptionWorkflowEntity tw
            JOIN tw.workflowActor ua
            WHERE ua.userFullName ILIKE CONCAT('%', :owner, '%')
            GROUP BY tw.transcription.id
            """)
    List<TranscriptionIdsAndLatestWorkflowTs> findWorkflowOwnedBy(String owner);

    @Query("""
            SELECT distinct tw.transcription
            FROM TranscriptionWorkflowEntity tw
            JOIN tw.transcription trans
            JOIN tw.workflowActor ua
            WHERE trans.transcriptionStatus.id = :statusId
            AND ua.id = :userId
            AND tw.transcriptionStatus.id = :statusId
            AND NOT EXISTS (
                SELECT newerWorkflow.id
                FROM TranscriptionWorkflowEntity newerWorkflow
                WHERE newerWorkflow.transcription = tw.transcription
                AND newerWorkflow.transcriptionStatus.id = :statusId
                AND (
                    newerWorkflow.workflowTimestamp > tw.workflowTimestamp
                    OR (
                        newerWorkflow.workflowTimestamp = tw.workflowTimestamp
                        AND newerWorkflow.id > tw.id
                    )
                )
            )
            """)
    List<TranscriptionEntity> findWorkflowForUserWithTranscriptionState(
        Integer userId,
        Integer statusId
    );
}