package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ObjectAdminActionRepository extends JpaRepository<ObjectAdminActionEntity, Integer> {
    List<ObjectAdminActionEntity> findByTranscriptionDocumentId(Long transcriptionDocumentId);

    List<ObjectAdminActionEntity> findByMediaId(Long mediaId);

    List<ObjectAdminActionEntity> findByMediaIdAndMarkedForManualDeletionTrue(Long mediaId);

    @Query("""
        SELECT objectAdminActionEntity
        FROM ObjectAdminActionEntity objectAdminActionEntity
        JOIN objectAdminActionEntity.objectHiddenReason reason
        WHERE objectAdminActionEntity.markedForManualDeletion = false
        AND reason.markedForDeletion = true
        AND objectAdminActionEntity.media IS NOT null
        """)
    List<ObjectAdminActionEntity> findAllMediaActionsWithAnyDeletionReason();

    Optional<ObjectAdminActionEntity> findByTranscriptionDocumentIdAndObjectHiddenReasonIsNotNullAndObjectHiddenReasonMarkedForDeletionTrue(
        Long transcriptionDocumentId);

    @Query("""
        SELECT o.id FROM ObjectAdminActionEntity o
                LEFT JOIN o.media m
                LEFT JOIN o.transcriptionDocument t
                WHERE o.markedForManualDelDateTime < :deletionThreshold
                AND ((m is not null and m.isDeleted = false)
                         OR (t is not null AND t.isDeleted = false))
        """)
    List<Integer> findObjectAdminActionsIdsForManualDeletion(OffsetDateTime deletionThreshold, Limit limit);
}