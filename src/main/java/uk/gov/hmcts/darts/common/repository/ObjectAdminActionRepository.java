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
    List<ObjectAdminActionEntity> findByTranscriptionDocument_Id(Integer transcriptionDocumentId);

    List<ObjectAdminActionEntity> findByMedia_Id(Integer mediaId);

    List<ObjectAdminActionEntity> findByMediaIdAndMarkedForManualDeletionTrue(Integer mediaId);

    @Query("""
        SELECT objectAdminActionEntity
        FROM ObjectAdminActionEntity objectAdminActionEntity
        JOIN objectAdminActionEntity.objectHiddenReason reason
        WHERE objectAdminActionEntity.markedForManualDeletion = false
        AND reason.markedForDeletion = true
        AND objectAdminActionEntity.media IS NOT null
        """)
    List<ObjectAdminActionEntity> findAllMediaActionsWithAnyDeletionReason();

    Optional<ObjectAdminActionEntity> findByTranscriptionDocument_IdAndObjectHiddenReasonIsNotNullAndObjectHiddenReason_MarkedForDeletionTrue(
        Integer transcriptionDocumentId);

    @Query("""
        SELECT o FROM ObjectAdminActionEntity o
                WHERE o.markedForManualDelDateTime < :deletionThreshold
        """)
    List<ObjectAdminActionEntity> findFilesForManualDeletion(OffsetDateTime deletionThreshold, Limit limit);
}