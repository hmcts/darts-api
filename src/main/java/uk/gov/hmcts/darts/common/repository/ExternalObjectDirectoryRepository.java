package uk.gov.hmcts.darts.common.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
                SELECT eod FROM ExternalObjectDirectoryEntity eod
                WHERE eod.media = :media
                AND eod.status = :status
            """
    )
    List<ExternalObjectDirectoryEntity> findByEntityAndStatus(MediaEntity media, ObjectRecordStatusEntity status);

    @Query(
        """
                SELECT eod FROM ExternalObjectDirectoryEntity eod
                WHERE eod.annotationDocumentEntity = :annotationDocument
                AND eod.status = :status
            """
    )
    List<ExternalObjectDirectoryEntity> findByEntityAndStatus(AnnotationDocumentEntity annotationDocument, ObjectRecordStatusEntity status);

    @Query(
        """
                SELECT eod FROM ExternalObjectDirectoryEntity eod
                WHERE eod.transcriptionDocumentEntity = :transcriptionDocument
                AND eod.status = :status
            """
    )
    List<ExternalObjectDirectoryEntity> findByEntityAndStatus(TranscriptionDocumentEntity transcriptionDocument, ObjectRecordStatusEntity status);

    List<ExternalObjectDirectoryEntity> findByTranscriptionDocumentEntityAndExternalLocationType(TranscriptionDocumentEntity transcriptionDocument,
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
            WHERE eod.externalLocationType = :location
            AND (eod.media = :media
            or eod.transcriptionDocumentEntity = :transcription
            or eod.annotationDocumentEntity = :annotation
            or eod.caseDocument = :caseDocument)
            """
    )
    List<ExternalObjectDirectoryEntity> findExternalObjectDirectoryByLocation(ExternalLocationTypeEntity location,
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
            AND eod.osrUuid is null
            """
    )
    List<ExternalObjectDirectoryEntity> findNotFinishedAndNotExceededRetryInStorageLocation(List<ObjectRecordStatusEntity> failedStatuses,
                                                                                            ExternalLocationTypeEntity type,
                                                                                            Integer transferAttempts,
                                                                                            Pageable pageable);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status in :failedStatuses
            AND eod.externalLocationType = :type
            AND eod.transferAttempts <= :transferAttempts
            AND eod.osrUuid is not null
            """
    )
    List<ExternalObjectDirectoryEntity> findNotFinishedAndNotExceededRetryInStorageLocationForDets(List<ObjectRecordStatusEntity> failedStatuses,
                                                                                                   ExternalLocationTypeEntity type,
                                                                                                   Integer transferAttempts,
                                                                                                   Limit limit);


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
                                                                                  ObjectRecordStatusEntity status, Limit limit);

    List<ExternalObjectDirectoryEntity> findByMediaAndExternalLocationType(MediaEntity media,
                                                                           ExternalLocationTypeEntity externalLocationType);

    List<ExternalObjectDirectoryEntity> findByAnnotationDocumentEntityAndExternalLocationType(AnnotationDocumentEntity annotationDocument,
                                                                                              ExternalLocationTypeEntity externalLocationType);

    List<ExternalObjectDirectoryEntity> findByCaseDocumentAndExternalLocationType(CaseDocumentEntity caseDocument,
                                                                                  ExternalLocationTypeEntity externalLocationTypeEntity);


    List<ExternalObjectDirectoryEntity> findByMedia(MediaEntity media);

    List<ExternalObjectDirectoryEntity> findByTranscriptionDocumentEntity(TranscriptionDocumentEntity transcriptionDocument);

    List<ExternalObjectDirectoryEntity> findByAnnotationDocumentEntity(AnnotationDocumentEntity annotationDocument);

    List<ExternalObjectDirectoryEntity> findByCaseDocument(CaseDocumentEntity caseDocument);

    @Query(
        """
            SELECT eod.id FROM ExternalObjectDirectoryEntity eod, ExternalObjectDirectoryEntity eod2
            WHERE
            ((:externalObjectDirectoryQueryTypeEnumIndex=1 AND eod.media = eod2.media) OR                 
            (:externalObjectDirectoryQueryTypeEnumIndex=2 
            AND (eod.transcriptionDocumentEntity=eod2.transcriptionDocumentEntity OR eod.annotationDocumentEntity=eod2.annotationDocumentEntity)))
            AND eod.status = :status1
            AND eod2.status = :status2
            AND eod.externalLocationType = :location1
            AND eod2.externalLocationType = :location2
            AND eod2.lastModifiedDateTime <= :lastModifiedBefore
            """
    )
    List<Integer> findIdsIn2StorageLocationsBeforeTime(ObjectRecordStatusEntity status1,
                                                       ObjectRecordStatusEntity status2,
                                                       ExternalLocationTypeEntity location1,
                                                       ExternalLocationTypeEntity location2,
                                                       OffsetDateTime lastModifiedBefore,
                                                       Integer externalObjectDirectoryQueryTypeEnumIndex,
                                                       Limit limit);

    @Query(
        """
            SELECT eod.id FROM ExternalObjectDirectoryEntity eod, ExternalObjectDirectoryEntity eod2
            WHERE
            (eod.transcriptionDocumentEntity=eod2.transcriptionDocumentEntity
            OR eod.annotationDocumentEntity=eod2.annotationDocumentEntity)
            AND eod.status = :status1
            AND eod2.status = :status2
            AND eod.externalLocationType = :location1
            AND eod2.externalLocationType = :location2
            AND eod2.lastModifiedDateTime <= :lastModifiedBefore2
            AND eod.lastModifiedDateTime <= :lastModifiedBefore1
            """
    )
    List<Integer> findIdsIn2StorageLocationsBeforeTime(ObjectRecordStatusEntity status1,
                                                       ObjectRecordStatusEntity status2,
                                                       ExternalLocationTypeEntity location1,
                                                       ExternalLocationTypeEntity location2,
                                                       OffsetDateTime lastModifiedBefore1,
                                                       OffsetDateTime lastModifiedBefore2,
                                                       Limit limit);

    @Query(
        """
            SELECT eod.id FROM ExternalObjectDirectoryEntity eod, ExternalObjectDirectoryEntity eod2
            WHERE
            eod.media = eod2.media
            AND eod.status = :storedStatus
            AND eod.externalLocationType = :unstructuredLocation
            AND eod.lastModifiedDateTime <= :unstructuredLastModifiedBefore
            AND eod2.externalLocationType = :armLocation
            AND eod2.status = :storedStatus
            """
    )
    List<Integer> findIdsForAudioToBeDeletedFromUnstructured(ObjectRecordStatusEntity storedStatus,
                                                             ExternalLocationTypeEntity unstructuredLocation,
                                                             ExternalLocationTypeEntity armLocation,
                                                             OffsetDateTime unstructuredLastModifiedBefore,
                                                             Limit limit);


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
            SELECT distinct(eod.manifestFile) FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status in :statuses
            AND eod.externalLocationType = :locationType
            AND eod.responseCleaned = :responseCleaned
            AND eod.lastModifiedDateTime < :lastModifiedBefore
            and eod.manifestFile is not null
            and eod.manifestFile like CONCAT(:manifestFileBatchPrefix, '%')
            ORDER BY 1
            """
    )
    List<String> findBatchCleanupManifestFilenames(
        List<ObjectRecordStatusEntity> statuses,
        ExternalLocationTypeEntity locationType,
        boolean responseCleaned,
        OffsetDateTime lastModifiedBefore, String manifestFileBatchPrefix, Limit limit);

    @Query(
        """
            SELECT distinct(eod.manifestFile) FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status in :statuses
            AND eod.externalLocationType = :locationType
            AND eod.responseCleaned = :responseCleaned
            AND eod.lastModifiedDateTime < :lastModifiedBefore
            and eod.manifestFile is not null
            ORDER BY 1
            """
    )
    List<String> findBatchCleanupManifestFilenames(
        List<ObjectRecordStatusEntity> statuses,
        ExternalLocationTypeEntity locationType,
        boolean responseCleaned,
        OffsetDateTime lastModifiedBefore, Limit limit);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.externalLocationType = :locationType
            AND eod.responseCleaned = :responseCleaned
            and eod.manifestFile = :manifestFilename
            order by eod.lastModifiedDateTime
            """
    )
    List<ExternalObjectDirectoryEntity> findBatchCleanupEntriesByManifestFilename(
        ExternalLocationTypeEntity locationType,
        boolean responseCleaned,
        String manifestFilename);


    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status in :statuses
            AND eod.externalLocationType = :locationType
            AND eod.responseCleaned = :responseCleaned
            AND eod.lastModifiedDateTime < :lastModifiedBefore
            and (eod.manifestFile is null
            or eod.manifestFile not like ':manifestFileBatchPrefix%')
            order by eod.lastModifiedDateTime
            """
    )
    List<ExternalObjectDirectoryEntity> findSingleArmResponseFiles(
        List<ObjectRecordStatusEntity> statuses,
        ExternalLocationTypeEntity locationType,
        boolean responseCleaned,
        OffsetDateTime lastModifiedBefore,
        String manifestFileBatchPrefix);

    @Modifying(clearAutomatically = true)
    @Query(
        """
            update ExternalObjectDirectoryEntity eod
            set eod.status = :newStatus,
            eod.lastModifiedBy = :userAccount,
            eod.lastModifiedDateTime = :timestamp
            where eod.id in :idsToUpdate
            """
    )
    void updateStatus(ObjectRecordStatusEntity newStatus, UserAccountEntity userAccount, List<Integer> idsToUpdate, OffsetDateTime timestamp);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
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
            """
    )
    List<ExternalObjectDirectoryEntity> findEodsForTransfer(ObjectRecordStatusEntity status, ExternalLocationTypeEntity type,
                                                            ObjectRecordStatusEntity notExistsStatus, ExternalLocationTypeEntity notExistsType,
                                                            Integer maxTransferAttempts, Limit limit);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status = :status
            AND eod.externalLocationType = :type
            AND NOT EXISTS (select 1 from ExternalObjectDirectoryEntity eod2
            where eod2.externalLocationType = :notExistsLocation
            and (eod.media = eod2.media
              OR eod.transcriptionDocumentEntity = eod2.transcriptionDocumentEntity
              OR eod.annotationDocumentEntity = eod2.annotationDocumentEntity
              OR eod.caseDocument = eod2.caseDocument ))
            order by eod.lastModifiedDateTime
            LIMIT :limitRecords
            """
    )
    List<ExternalObjectDirectoryEntity> findEodsNotInOtherStorage(ObjectRecordStatusEntity status, ExternalLocationTypeEntity type,
                                                                  ExternalLocationTypeEntity notExistsLocation, Integer limitRecords);

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


    @Query(
        """
            SELECT COUNT(eod) > 0
            FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status = :inboundStatus
            AND eod.media = :media
            AND eod.externalLocationType = :inboundLocation
            AND NOT EXISTS
              (
              SELECT eod2 FROM ExternalObjectDirectoryEntity eod2
              WHERE eod.media = eod2.media
              AND eod2.status != :ignoredUnstructuredStatus
              AND eod2.externalLocationType IN :destinationLocations
              )
            """
    )
    boolean hasMediaNotBeenCopiedFromInboundStorage(MediaEntity media, ObjectRecordStatusEntity inboundStatus,
                                                    ExternalLocationTypeEntity inboundLocation,
                                                    ObjectRecordStatusEntity ignoredUnstructuredStatus,
                                                    List<ExternalLocationTypeEntity> destinationLocations);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status = :status 
            AND eod.manifestFile = :manifestFile
            ORDER BY eod.lastModifiedDateTime
            """
    )
    List<ExternalObjectDirectoryEntity> findAllByStatusAndManifestFile(ObjectRecordStatusEntity status, String manifestFile);

    List<ExternalObjectDirectoryEntity> findByExternalLocationTypeAndUpdateRetention(ExternalLocationTypeEntity externalLocationTypeEntity,
                                                                                     boolean updateRetention, Limit limit);


    List<ExternalObjectDirectoryEntity> findByManifestFile(String manifestName);

    @Query("""
               SELECT eod
               FROM ExternalObjectDirectoryEntity eod
               JOIN TranscriptionDocumentEntity t ON eod.transcriptionDocumentEntity = t
               WHERE t.retainUntilTs < :maxRetentionDate
               AND t.isDeleted = false
               AND eod.externalLocationType.id in (1,2,3)
               AND EXISTS (
                SELECT 1 FROM ExternalObjectDirectoryEntity eod2
                WHERE eod2.externalLocationType.id = 3
                AND eod2.status.id = 2
                AND eod2.transcriptionDocumentEntity = eod.transcriptionDocumentEntity
               )
        """)
    List<ExternalObjectDirectoryEntity> findExpiredTranscriptionDocuments(OffsetDateTime maxRetentionDate,
                                                                          Limit batchSize);

    @Query("""
               SELECT eod
               FROM ExternalObjectDirectoryEntity eod
               JOIN AnnotationDocumentEntity a ON eod.annotationDocumentEntity = a
               WHERE a.retainUntilTs < :maxRetentionDate
               AND a.isDeleted = false
               AND eod.externalLocationType.id in (1,2,3)
               AND EXISTS (
                SELECT 1 FROM ExternalObjectDirectoryEntity eod2
                WHERE eod2.externalLocationType.id = 3
                AND eod2.status.id = 2
                AND eod2.annotationDocumentEntity = eod.annotationDocumentEntity
               )
        """)
    List<ExternalObjectDirectoryEntity> findExpiredAnnotationDocuments(OffsetDateTime maxRetentionDate, Limit batchSize);

    @Query("""
               SELECT eod
               FROM ExternalObjectDirectoryEntity eod
               JOIN CaseDocumentEntity c ON eod.caseDocument = c
               WHERE c.retainUntilTs < :maxRetentionDate
               AND c.isDeleted = false
               AND eod.externalLocationType.id in (1,2,3)
               AND EXISTS (
                SELECT 1 FROM ExternalObjectDirectoryEntity eod2
                WHERE eod2.externalLocationType.id = 3
                AND eod2.status.id = 2
                AND eod2.caseDocument = eod.caseDocument
               )
        """)
    List<ExternalObjectDirectoryEntity> findExpiredCaseDocuments(OffsetDateTime maxRetentionDate, Limit batchSize);

    @Query("""
               SELECT eod
               FROM ExternalObjectDirectoryEntity eod
               JOIN MediaEntity m ON eod.media = m
               WHERE m.retainUntilTs < :maxRetentionDate
               AND m.isDeleted = false
               AND eod.externalLocationType.id in (1,2,3)
               AND EXISTS (
                SELECT 1 FROM ExternalObjectDirectoryEntity eod2
                WHERE eod2.externalLocationType.id = 3
                AND eod2.status.id = 2
                AND eod2.media = eod.media
               )
        """)
    List<ExternalObjectDirectoryEntity> findExpiredMediaEntries(OffsetDateTime maxRetentionDate, Limit batchSize);

    @Query("""
        SELECT eod FROM ExternalObjectDirectoryEntity eod
                WHERE eod.media.id = :mediaId
                AND eod.status.id = 2
                AND eod.externalLocationType.id IN (1, 2)
        """)
    List<ExternalObjectDirectoryEntity> findStoredInInboundAndUnstructuredByMediaId(@Param("mediaId") Integer mediaId);

    @Query("""
        SELECT eod FROM ExternalObjectDirectoryEntity eod
                WHERE eod.transcriptionDocumentEntity.id = :transcriptionDocumentId
                AND eod.status.id = 2
                AND eod.externalLocationType.id IN (1, 2)
        """)
    List<ExternalObjectDirectoryEntity> findStoredInInboundAndUnstructuredByTranscriptionId(@Param("transcriptionDocumentId") Integer id);

    @Modifying
    @Query("""
        update ExternalObjectDirectoryEntity eod
        set eod.status = :newStatus
        where eod.status = :currentStatus
        and eod.dataIngestionTs <= :maxDataIngestionTs
        """)
    @Transactional
    void updateByStatusEqualsAndDataIngestionTsBefore(ObjectRecordStatusEntity currentStatus, OffsetDateTime maxDataIngestionTs,
                                                      ObjectRecordStatusEntity newStatus, Limit limit);

    @Query(value = """
        select fileSize from
        (
            (
                select file_size as fileSize from darts.media as med
                join darts.external_object_directory as eod on eod.med_id = med.med_id
                where eod.eod_id = :externalObjectDirectoryId
            )
            union
            (
                select file_size as fileSize from darts.annotation_document as ado
                join darts.external_object_directory as eod on eod.ado_id = ado.ado_id
                where eod.eod_id = :externalObjectDirectoryId
            )
            union
            (
                select file_size as fileSize from darts.case_document as cad
                join darts.external_object_directory as eod on eod.cad_id = cad.cad_id
                where eod.eod_id = :externalObjectDirectoryId
            )
            union
            (
                select file_size as fileSize from darts.transcription_document as trd
                join darts.external_object_directory as eod on eod.trd_id = trd.trd_id
                where eod.eod_id = :externalObjectDirectoryId
            )
        ) a
        """, nativeQuery = true)
    Long findFileSize(Integer externalObjectDirectoryId);


    @Query("""
            update ExternalObjectDirectoryEntity eod
            set eod.status = :newStatus,
                eod.transferAttempts = :transferAttempts,
                eod.lastModifiedBy = :currentUser,
                eod.lastModifiedDateTime = current_timestamp
            where eod.status = :oldStatus
            and eod.lastModifiedDateTime between :startTime and :endTime
        """)
    @Modifying
    void updateEodStatusAndTransferAttemptsWhereLastModifiedIsBetweenTwoDateTimesAndHasStatus(
        ObjectRecordStatusEntity newStatus, Integer transferAttempts,
        ObjectRecordStatusEntity oldStatus,
        OffsetDateTime startTime, OffsetDateTime endTime,
        UserAccountEntity currentUser);

    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status = :status 
            AND eod.dataIngestionTs between :rpoCsvStartTime AND :rpoCsvEndTime
            """
    )
    List<ExternalObjectDirectoryEntity> findByStatusAndIngestionDate(ObjectRecordStatusEntity status,
                                                                     OffsetDateTime rpoCsvStartTime,
                                                                     OffsetDateTime rpoCsvEndTime);

    @Query("""
        SELECT eod FROM ExternalObjectDirectoryEntity eod
        WHERE eod.status = :status
        AND eod.dataIngestionTs BETWEEN :ingestionStartDateTime AND :ingestionEndDateTime
        """)
    List<ExternalObjectDirectoryEntity> findAllByStatusAndDataIngestionTsBetweenAndLimit(@Param("status") ObjectRecordStatusEntity status,
                                                                                         @Param("ingestionStartDateTime") OffsetDateTime ingestionStartDateTime,
                                                                                         @Param("ingestionEndDateTime") OffsetDateTime ingestionEndDateTime,
                                                                                         Limit limit);
}