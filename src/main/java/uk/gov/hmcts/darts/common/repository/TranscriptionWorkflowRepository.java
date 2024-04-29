package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;

import java.util.List;

@Repository
public interface TranscriptionWorkflowRepository extends JpaRepository<TranscriptionWorkflowEntity, Integer> {

    List<TranscriptionWorkflowEntity> findByTranscriptionOrderByWorkflowTimestampDesc(TranscriptionEntity transcription);
}
