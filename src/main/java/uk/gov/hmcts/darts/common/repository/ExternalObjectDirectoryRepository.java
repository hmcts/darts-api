package uk.gov.hmcts.darts.common.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@SuppressWarnings({
    "PMD.TooManyMethods",//TODO - refactor to reduce methods when this class is next edited,
    "PMD.ExcessivePublicCount"//TODO - refactor to reduce public methods when this class is next edited
})
public interface ExternalObjectDirectoryRepository extends JpaRepository<ExternalObjectDirectoryEntity, Long> {

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


    List<ExternalObjectDirectoryEntity> findByTranscriptionDocumentEntityAndExternalLocationTypeAndStatus(
        TranscriptionDocumentEntity transcriptionDocument,
        ExternalLocationTypeEntity externalLocationType,
        ObjectRecordStatusEntity status);

    @Query(
        """
                SELECT eod.media.id FROM ExternalObjectDirectoryEntity eod
                WHERE eod.media.id in :mediaIdList
                AND eod.status = :status
                AND eod.externalLocationType in :externalLocationTypes
            """
    )
    List<Long> findMediaIdsByInMediaIdStatusAndType(List<Long> mediaIdList, ObjectRecordStatusEntity status,
                                                    ExternalLocationTypeEntity... externalLocationTypes);


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
            SELECT eod.id FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status in :failedStatuses
            AND eod.externalLocationType = :type
            AND eod.transferAttempts <= :transferAttempts
            AND eod.osrUuid is null
            """
    )
    List<Long> findNotFinishedAndNotExceededRetryInStorageLocation(List<ObjectRecordStatusEntity> failedStatuses,
                                                                   ExternalLocationTypeEntity type,
                                                                   Integer transferAttempts,
                                                                   Pageable pageable);

    @Query(
        """
            SELECT eod.id FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status in :failedStatuses
            AND eod.externalLocationType = :type
            AND eod.transferAttempts <= :transferAttempts
            AND eod.osrUuid is not null
            """
    )
    List<Long> findNotFinishedAndNotExceededRetryInStorageLocationForDets(List<ObjectRecordStatusEntity> failedStatuses,
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

    List<ExternalObjectDirectoryEntity> findByMediaAndExternalLocationTypeAndStatus(MediaEntity media,
                                                                                    ExternalLocationTypeEntity externalLocationType,
                                                                                    ObjectRecordStatusEntity status);

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
    List<Long> findIdsIn2StorageLocationsBeforeTime(ObjectRecordStatusEntity status1,
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
    List<Long> findIdsIn2StorageLocationsBeforeTime(ObjectRecordStatusEntity status1,
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
    List<Long> findIdsForAudioToBeDeletedFromUnstructured(ObjectRecordStatusEntity storedStatus,
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
            eod.lastModifiedById = :userAccount,
            eod.lastModifiedDateTime = :timestamp
            where eod.id in :idsToUpdate
            """
    )
    void updateStatus(ObjectRecordStatusEntity newStatus, Integer userAccount, List<Long> idsToUpdate, OffsetDateTime timestamp);


    default List<Long> findEodsForTransfer(ObjectRecordStatusEntity status, ExternalLocationTypeEntity type,
                                           ObjectRecordStatusEntity notExistsStatus, ExternalLocationTypeEntity notExistsType,
                                           Integer maxTransferAttempts, Limit limit) {
        List<Long> results = new ArrayList<>();//Ensures no duplicates
        results.addAll(findEodsForTransferOnlyMedia(status.getId(), type.getId(), notExistsStatus.getId(),
                                                    notExistsType.getId(), maxTransferAttempts, limit.max()));
        if (results.size() < limit.max()) {
            results.addAll(findEodsForTransferExcludingMedia(status, type, notExistsStatus, notExistsType, maxTransferAttempts,
                                                             Limit.of(limit.max() - results.size())));
        }
        return new ArrayList<>(results);
    }

    @Query(
        nativeQuery = true,
        value = """
            WITH filtered_eod AS (
                 SELECT eod_id, med_id, last_modified_ts
                 FROM darts.external_object_directory
                 WHERE ors_id = :status AND elt_id = :type AND med_id IS NOT NULL
             ),
                  med_ids_to_exclude AS (
                      SELECT DISTINCT med_id
                      FROM darts.external_object_directory
                      WHERE elt_id = :notExistsType AND med_id IS NOT NULL AND (ors_id = :notExistsStatus OR transfer_attempts >= :maxTransferAttempts)
                  )
             SELECT f.eod_id
             FROM filtered_eod f
                      LEFT JOIN med_ids_to_exclude mediaToExclude ON f.med_id = mediaToExclude.med_id
             WHERE mediaToExclude.med_id IS NULL
             ORDER BY f.last_modified_ts
             FETCH FIRST :limit ROWS ONLY;
            """
    )
    List<Long> findEodsForTransferOnlyMedia(Integer status, Integer type,
                                            Integer notExistsStatus, Integer notExistsType,
                                            Integer maxTransferAttempts, Integer limit);

    @Query(
        """
            SELECT eod.id FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status = :status
            AND eod.externalLocationType = :type
            AND eod.media is null            
            AND NOT EXISTS (select 1 from ExternalObjectDirectoryEntity eod2
            where (eod2.status = :notExistsStatus or eod2.transferAttempts >= :maxTransferAttempts)
            AND eod2.externalLocationType = :notExistsType
            and ((eod.transcriptionDocumentEntity is not null and eod.transcriptionDocumentEntity = eod2.transcriptionDocumentEntity)
              OR (eod.annotationDocumentEntity is not null and eod.annotationDocumentEntity = eod2.annotationDocumentEntity)
              OR (eod.caseDocument is not null and eod.caseDocument = eod2.caseDocument )))
            order by eod.lastModifiedDateTime
            """
    )
    List<Long> findEodsForTransferExcludingMedia(ObjectRecordStatusEntity status, ExternalLocationTypeEntity type,
                                                 ObjectRecordStatusEntity notExistsStatus, ExternalLocationTypeEntity notExistsType,
                                                 Integer maxTransferAttempts, Limit limit);


    default List<Long> findEodsNotInOtherStorage(ObjectRecordStatusEntity status, ExternalLocationTypeEntity type,
                                                 ExternalLocationTypeEntity notExistsLocation, Integer limitRecords) {
        Set<Long> results = new HashSet<>();//Ensures no duplicates
        results.addAll(findEodsNotInOtherStorageOnlyMedia(status.getId(), type.getId(), notExistsLocation.getId(), limitRecords));
        if (results.size() < limitRecords) {
            results.addAll(findEodsNotInOtherStorageExcludingMedia(status.getId(), type.getId(), notExistsLocation.getId(), limitRecords - results.size()));
        }
        return new ArrayList<>(results);
    }

    @Query(
        value = """
            SELECT eod1.eod_id
            FROM darts.external_object_directory eod1
            WHERE eod1.ors_id = :status
            AND eod1.elt_id = :type
            AND eod1.med_id IS NOT NULL
            AND NOT EXISTS (
                SELECT 1
                FROM darts.external_object_directory eod2
                WHERE eod2.elt_id = :notExistsLocation
                AND eod1.med_id = eod2.med_id
            )
            FETCH FIRST :limitRecords rows only
            """,
        nativeQuery = true
    )
    List<Long> findEodsNotInOtherStorageOnlyMedia(Integer status, Integer type,
                                                  Integer notExistsLocation, Integer limitRecords);

    @Query(
        value = """
            SELECT eod1.eod_id
            FROM darts.external_object_directory eod1
            WHERE eod1.ors_id = :status
            AND eod1.elt_id = :type
            AND eod1.med_id IS NULL
            AND NOT EXISTS (
                SELECT 1
                FROM darts.external_object_directory eod2
                WHERE eod2.elt_id = :notExistsLocation
                AND (
                    (eod1.trd_id IS NOT NULL AND eod1.trd_id = eod2.trd_id) OR
                    (eod1.ado_id IS NOT NULL AND eod1.ado_id = eod2.ado_id) OR
                    (eod1.cad_id IS NOT NULL AND eod1.cad_id = eod2.cad_id)
                )
            )
            fetch first :limitRecords rows only            
            """,
        nativeQuery = true
    )
    List<Long> findEodsNotInOtherStorageExcludingMedia(Integer status, Integer type,
                                                       Integer notExistsLocation, Integer limitRecords);

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
    ExternalObjectDirectoryEntity findByIdsAndFailure(Long mediaId, Long caseDocumentId, Long annotationDocumentId, Long transcriptionDocumentId,
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

    @Query("""
        SELECT eod.id FROM ExternalObjectDirectoryEntity eod        
        LEFT JOIN eod.media med
        LEFT JOIN eod.transcriptionDocumentEntity td       
        LEFT JOIN eod.annotationDocumentEntity ad
        LEFT JOIN eod.caseDocument cd
        WHERE eod.externalLocationType = :externalLocationTypeEntity
        AND (
           (med.retainUntilTs is not null and med.retConfScore is not null) or
           (td.retainUntilTs is not null and td.retConfScore is not null) or
           (ad.retainUntilTs is not null and ad.retConfScore is not null) or
           (cd.retainUntilTs is not null and cd.retConfScore is not null) 
        )        
        AND eod.updateRetention = :updateRetention
        ORDER BY eod.id
        """)
    List<Long> findByExternalLocationTypeAndUpdateRetention(ExternalLocationTypeEntity externalLocationTypeEntity,
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
    List<ExternalObjectDirectoryEntity> findStoredInInboundAndUnstructuredByMediaId(@Param("mediaId") Long mediaId);

    @Query("""
        SELECT eod FROM ExternalObjectDirectoryEntity eod
                WHERE eod.transcriptionDocumentEntity.id = :transcriptionDocumentId
                AND eod.status.id = 2
                AND eod.externalLocationType.id IN (1, 2)
        """)
    List<ExternalObjectDirectoryEntity> findStoredInInboundAndUnstructuredByTranscriptionId(@Param("transcriptionDocumentId") Long id);

    @Modifying
    @Query("""
        update ExternalObjectDirectoryEntity eod
        set eod.status = :newStatus,
            eod.lastModifiedById = :currentUser,
            eod.lastModifiedDateTime = current_timestamp
        where eod.status = :currentStatus
        and eod.dataIngestionTs <= :maxDataIngestionTs
        """)
    @Transactional
    void updateByStatusEqualsAndDataIngestionTsBefore(ObjectRecordStatusEntity currentStatus, OffsetDateTime maxDataIngestionTs,
                                                      ObjectRecordStatusEntity newStatus,
                                                      Integer currentUser,
                                                      Limit limit);

    @Modifying(clearAutomatically = true)
    @Query(
        """
            update ExternalObjectDirectoryEntity eod
            set eod.status = :newStatus,
                eod.transferAttempts = :transferAttempts,
                eod.lastModifiedById = :currentUser,
                eod.lastModifiedDateTime = current_timestamp,
                eod.inputUploadProcessedTs = null
            where eod.id in :idsToUpdate
            """
    )
    void updateEodStatusAndTransferAttemptsWhereIdIn(ObjectRecordStatusEntity newStatus, Integer transferAttempts, Integer currentUser,
                                                     List<Long> idsToUpdate);


    @Query(
        """
            SELECT eod FROM ExternalObjectDirectoryEntity eod
            WHERE eod.status = :status 
            AND eod.inputUploadProcessedTs between :rpoCsvStartTime AND :rpoCsvEndTime
            """
    )
    Page<ExternalObjectDirectoryEntity> findByStatusAndInputUploadProcessedTsWithPaging(ObjectRecordStatusEntity status,
                                                                                        OffsetDateTime rpoCsvStartTime,
                                                                                        OffsetDateTime rpoCsvEndTime,
                                                                                        Pageable pageable);

    @Query("""
        SELECT eod FROM ExternalObjectDirectoryEntity eod
        WHERE eod.status = :status
        AND eod.inputUploadProcessedTs BETWEEN :ingestionStartDateTime AND :ingestionEndDateTime
        """)
    List<ExternalObjectDirectoryEntity> findAllByStatusAndInputUploadProcessedTsBetweenAndLimit(
        @Param("status") ObjectRecordStatusEntity status,
        @Param("ingestionStartDateTime") OffsetDateTime ingestionStartDateTime,
        @Param("ingestionEndDateTime") OffsetDateTime ingestionEndDateTime,
        Limit limit);

    @Query("""
        SELECT eod.id FROM ExternalObjectDirectoryEntity eod
        WHERE eod.status = :status
        AND eod.lastModifiedDateTime BETWEEN :startDateTime AND :endDateTime
        AND eod.externalLocationType = :locationType
        """)
    List<Long> findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit(@Param("status") ObjectRecordStatusEntity status,
                                                                        @Param("startDateTime") OffsetDateTime startDateTime,
                                                                        @Param("endDateTime") OffsetDateTime endDateTime,
                                                                        @Param("locationType") ExternalLocationTypeEntity locationType,
                                                                        Limit limit);


}