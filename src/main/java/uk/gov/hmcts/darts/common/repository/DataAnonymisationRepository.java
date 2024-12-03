package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.DataAnonymisationEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;

import java.util.List;

@Repository
public interface DataAnonymisationRepository extends JpaRepository<DataAnonymisationEntity, Integer> {

    List<DataAnonymisationEntity> findByEvent(EventEntity eventEntity);

    List<DataAnonymisationEntity> findByTranscriptionComment(TranscriptionCommentEntity transcriptionCommentEntity);
}
