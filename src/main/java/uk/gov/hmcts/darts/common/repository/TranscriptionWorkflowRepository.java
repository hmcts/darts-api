package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;

import java.util.List;

@Repository
public interface TranscriptionWorkflowRepository extends JpaRepository<TranscriptionWorkflowEntity, Integer> {

    @Query("""
            SELECT new uk.gov.hmcts.darts.common.repository.TranscriptionIdsAndLatestWorkflowTs(tw.transcription.id, MAX(tw.workflowTimestamp))
            FROM TranscriptionWorkflowEntity tw
            JOIN tw.workflowActor ua
            WHERE ua.userFullName LIKE CONCAT('%', :owner, '%')
            GROUP BY tw.transcription.id
            """)
    List<TranscriptionIdsAndLatestWorkflowTs> findWorkflowOwnedBy(String owner);
}
