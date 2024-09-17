package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ObjectAdminActionRepository extends JpaRepository<ObjectAdminActionEntity, Integer> {
    List<ObjectAdminActionEntity> findByTranscriptionDocument_Id(Integer transcriptionDocumentId);

    List<ObjectAdminActionEntity> findByMedia_Id(Integer transcriptionDocumentId);

    @Query("""
        SELECT objectAdminActionEntity
        FROM ObjectAdminActionEntity objectAdminActionEntity
        JOIN objectAdminActionEntity.objectHiddenReason reason
        WHERE objectAdminActionEntity.markedForManualDeletion = false
        AND reason.markedForDeletion = true
        AND objectAdminActionEntity.media IS NOT null
        """)
    List<ObjectAdminActionEntity> findAllMediaActionsWithAnyDeletionReason();


    @Query("""
        SELECT oaa FROM ObjectAdminActionEntity oaa WHERE oaa.transcriptionDocument.id = :transcriptionDocumentId
        AND oaa.objectHiddenReason IS NOT NULL AND oaa.objectHiddenReason.markedForDeletion = true
        """)
    Optional<ObjectAdminActionEntity> findByTranscriptionDocumentIdHiddenAndMarkedForDeletion(
        @Param("transcriptionDocumentId") Integer transcriptionDocumentId);

}