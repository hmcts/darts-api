package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalObjectDirectoryRepository extends JpaRepository<ExternalObjectDirectoryEntity, Integer> {

    @Query(
        """
                SELECT eod FROM ExternalObjectDirectoryEntity eod
                WHERE eod.media = :media
                AND eod.status = :status
                AND eod.externalLocationType = :externalLocationType
            """
    )
    List<ExternalObjectDirectoryEntity> findByMediaStatusAndType(MediaEntity media, ObjectRecordStatusEntity status,
                                                                 ExternalLocationTypeEntity externalLocationType);

    @Query(
        """
                SELECT eod.media.id FROM ExternalObjectDirectoryEntity eod
                WHERE eod.media.id in :mediaIdList
                AND eod.status = :status
                AND eod.externalLocationType = :externalLocationType
            """
    )
    List<Integer> findMediaIdsByInMediaIdStatusAndType(List<Integer> mediaIdList, ObjectRecordStatusEntity status,
                                                       ExternalLocationTypeEntity externalLocationType);

    @Query(
        """
            SELECT eo FROM ExternalObjectDirectoryEntity eo
            WHERE eo.status = :status
            AND eo.externalLocationType = :location1
            AND eo.id NOT IN
              (
              SELECT eod.id FROM ExternalObjectDirectoryEntity eod, ExternalObjectDirectoryEntity eod2
              WHERE (eod.media = eod2.media
              OR eod.transcriptionDocumentEntity = eod2.transcriptionDocumentEntity
              OR eod.annotationDocumentEntity = eod2.annotationDocumentEntity
              OR eod.caseDocument = eod2.caseDocument )
              AND eod.status = :status
              AND eod.externalLocationType = :location1
              AND eod2.externalLocationType = :location2
              )
            """
    )
    List<ExternalObjectDirectoryEntity> findExternalObjectsNotIn2StorageLocations(ObjectRecordStatusEntity status,
                                                                                  ExternalLocationTypeEntity location1,
                                                                                  ExternalLocationTypeEntity location2);


    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status = :status
            AND eod.externalLocationType = :location
            AND (eod.media = :media
            or eod.transcriptionDocumentEntity = :transcription
            or eod.annotationDocumentEntity = :annotation
            or eod.caseDocument = :caseDocument)
            """
    )
    Optional<ExternalObjectDirectoryEntity> findMatchingExternalObjectDirectoryEntityByLocation(ObjectRecordStatusEntity status,
                                                                                                ExternalLocationTypeEntity location,
                                                                                                MediaEntity media,
                                                                                                TranscriptionDocumentEntity transcription,
                                                                                                AnnotationDocumentEntity annotation,
                                                                                                CaseDocumentEntity caseDocument);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status in :failedStatuses
            AND eod.externalLocationType = :type
            AND eod.transferAttempts <= :transferAttempts
            """
    )
    List<ExternalObjectDirectoryEntity> findNotFinishedAndNotExceededRetryInStorageLocation(List<ObjectRecordStatusEntity> failedStatuses,
                                                                                            ExternalLocationTypeEntity type,
                                                                                            Integer transferAttempts);

    List<EntityIdOnly> findByStatusAndExternalLocationType(ObjectRecordStatusEntity status, ExternalLocationTypeEntity type);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status = :status AND eod.externalLocationType = :type
            """
    )
    List<ExternalObjectDirectoryEntity> findByStatusAndType(ObjectRecordStatusEntity status, ExternalLocationTypeEntity type);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status.id in :statusList
            AND eod.externalLocationType = :type
            """
    )
    List<ExternalObjectDirectoryEntity> findByStatusIdInAndType(List<Integer> statusList,
                                                                ExternalLocationTypeEntity type);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.externalLocationType = :externalLocationTypeEntity
            AND eod.status = :status
            """
    )
    List<ExternalObjectDirectoryEntity> findByExternalLocationTypeAndObjectStatus(ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                                  ObjectRecordStatusEntity status);

    List<ExternalObjectDirectoryEntity> findByMediaAndExternalLocationType(MediaEntity media,
                                                                           ExternalLocationTypeEntity externalLocationType);

    List<ExternalObjectDirectoryEntity> findByMedia(MediaEntity media);

    @Query(
        """
            SELECT eod.id FROM ExternalObjectDirectoryEntity eod, ExternalObjectDirectoryEntity eod2
            WHERE eod.media is not null
            AND eod.media = eod2.media
            AND eod.status = :status1
            AND eod2.status = :status2
            AND eod.externalLocationType = :location1
            AND eod2.externalLocationType = :location2
            AND eod2.lastModifiedDateTime < :lastModifiedBefore
            """
    )
    List<Integer> findMediaFileIdsIn2StorageLocationsBeforeTime(ObjectRecordStatusEntity status1,
                                                                ObjectRecordStatusEntity status2,
                                                                ExternalLocationTypeEntity location1,
                                                                ExternalLocationTypeEntity location2,
                                                                OffsetDateTime lastModifiedBefore);

    @Query(
        """

            SELECT eod FROM ExternalObjectDirectoryEntity eod
                  JOIN eod.annotationDocumentEntity ade
                  JOIN ade.annotation ann
                  JOIN ann.annotationDocuments annD
                  WHERE ann.id = :annotationId
                  AND eod.status = :status
                  AND annD.id = :annotationDocumentId
                  ORDER BY eod.createdDateTime DESC
                  """
    )
    List<ExternalObjectDirectoryEntity> findByAnnotationIdAndAnnotationDocumentId(Integer annotationId,
                                                                                  Integer annotationDocumentId,
                                                                                  ObjectRecordStatusEntity status);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status in :statuses
            AND eod.externalLocationType = :locationType
            AND eod.responseCleaned = :responseCleaned
            AND eod.lastModifiedDateTime < :lastModifiedBefore
            order by eod.lastModifiedDateTime
            """
    )
    List<ExternalObjectDirectoryEntity> findByStatusInAndExternalLocationTypeAndResponseCleanedAndLastModifiedDateTimeBefore(
        List<ObjectRecordStatusEntity> statuses,
        ExternalLocationTypeEntity locationType,
        boolean responseCleaned,
        OffsetDateTime lastModifiedBefore);

    @Modifying(clearAutomatically = true)
    @Query(
        """
            update ExternalObjectDirectoryEntity eod
            set eod.status = :newStatus,
            eod.lastModifiedBy = :userAccount,
            eod.lastModifiedDateTime = :timestamp
            where eod.id in :idsToDelete
            """
    )
    void updateStatus(ObjectRecordStatusEntity newStatus, UserAccountEntity userAccount, List<Integer> idsToDelete, OffsetDateTime timestamp);

    @Query(
        """
            SELECT eod.id FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status = :status 
            AND eod.externalLocationType = :type
            AND NOT EXISTS (select 1 from ExternalObjectDirectoryEntity eod2 
            where (eod2.status = :notExistsStatus or eod2.transferAttempts >= :maxTransferAttempts) 
            AND eod2.externalLocationType = :notExistsType
            and (eod.media = eod2.media
              OR eod.transcriptionDocumentEntity = eod2.transcriptionDocumentEntity
              OR eod.annotationDocumentEntity = eod2.annotationDocumentEntity
              OR eod.caseDocument = eod2.caseDocument ))
            order by eod.lastModifiedDateTime
            LIMIT :limitRecords
            """
    )
    List<Integer> findEodIdsForTransfer(ObjectRecordStatusEntity status, ExternalLocationTypeEntity type,
                                        ObjectRecordStatusEntity notExistsStatus, ExternalLocationTypeEntity notExistsType,
                                        Integer maxTransferAttempts, Integer limitRecords);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status.id in :failureStatesList and
            (eod.media.id = :mediaId or eod.caseDocument.id = :caseDocumentId
            or eod.annotationDocumentEntity.id = :annotationDocumentId
            or eod.transcriptionDocumentEntity.id = :transcriptionDocumentId)
            and eod.transferAttempts < 3
            order by eod.lastModifiedDateTime
            """
    )
    ExternalObjectDirectoryEntity findByIdsAndFailure(Integer mediaId, Integer caseDocumentId, Integer annotationDocumentId, Integer transcriptionDocumentId,
                                                      List<Integer> failureStatesList);
}
