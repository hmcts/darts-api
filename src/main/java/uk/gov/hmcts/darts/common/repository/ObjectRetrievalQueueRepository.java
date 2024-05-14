package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRetrievalQueueEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;

import java.util.Optional;

public interface ObjectRetrievalQueueRepository extends JpaRepository<ObjectRetrievalQueueEntity, Integer> {

    @Query(
        """
            SELECT orq FROM ObjectRetrievalQueueEntity orq
            WHERE orq.parentObjectId = :parentObjectId
            AND orq.contentObjectId = :contentObjectId
            AND orq.clipId = :clipId
            AND (orq.media = :media
            or orq.transcriptionDocument = :transcriptionDocument)
        """
    )
    Optional<ObjectRetrievalQueueEntity> findMatchingObjectRetrievalQueuedItems(MediaEntity media,
                                                                                   TranscriptionDocumentEntity transcriptionDocument,
                                                                                   String parentObjectId,
                                                                                   String contentObjectId,
                                                                                   String clipId);
}
