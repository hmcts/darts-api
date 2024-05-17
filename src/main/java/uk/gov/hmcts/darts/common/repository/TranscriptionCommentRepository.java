package uk.gov.hmcts.darts.common.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;

import java.util.List;

@Repository
public interface TranscriptionCommentRepository extends JpaRepository<TranscriptionCommentEntity, Integer> {

    List<TranscriptionCommentEntity> getByTranscriptionAndTranscriptionWorkflowIsNull(TranscriptionEntity transcription);
}
