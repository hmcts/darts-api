package uk.gov.hmcts.darts.arm.helper;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItem;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItems;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.util.EodHelper.equalsAnyStatus;

@Component
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
public class DataStoreToArmHelper {

    private static final int BLOB_ALREADY_EXISTS_STATUS_CODE = 409;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final LogApi logApi;
    private final ArmDataManagementApi armDataManagementApi;
    private final ArchiveRecordFileGenerator archiveRecordFileGenerator;

    public List<Long> getEodEntitiesToSendToArm(ExternalLocationTypeEntity sourceLocation,
                                                ExternalLocationTypeEntity armLocation, int maxResultSize) {

        List<ObjectRecordStatusEntity> statusEntityList = List.of(
            EodHelper.failedArmRawDataStatus(), EodHelper.failedArmManifestFileStatus(),
            EodHelper.armIngestionStatus(), EodHelper.armRawDataPushedStatus());

        var failedArmExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(
            statusEntityList,
            armLocation,
            armDataManagementConfiguration.getMaxRetryAttempts(),
            Pageable.ofSize(maxResultSize)
        );

        List<Long> returnList = new ArrayList<>(failedArmExternalObjectDirectoryEntities);

        int remainingBatchSizeEods = maxResultSize - failedArmExternalObjectDirectoryEntities.size();
        if (remainingBatchSizeEods > 0) {
            var pendingUnstructuredExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findEodsNotInOtherStorage(
                EodHelper.storedStatus(), sourceLocation,
                EodHelper.armLocation(),
                remainingBatchSizeEods);
            returnList.addAll(pendingUnstructuredExternalObjectDirectoryEntities);
        }

        return returnList;
    }


    public Optional<ExternalObjectDirectoryEntity> getExternalObjectDirectoryEntity(
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity, ExternalLocationTypeEntity eodSourceLocation, ObjectRecordStatusEntity status) {

        return externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            status,
            eodSourceLocation,
            externalObjectDirectoryEntity.getMedia(),
            externalObjectDirectoryEntity.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntity.getAnnotationDocumentEntity(),
            externalObjectDirectoryEntity.getCaseDocument()
        );
    }

    public void updateExternalObjectDirectoryStatusToFailed(ExternalObjectDirectoryEntity externalObjectDirectoryEntity,
                                                            ObjectRecordStatusEntity objectRecordStatus, UserAccountEntity userAccount) {
        updateExternalObjectDirectoryStatus(externalObjectDirectoryEntity, objectRecordStatus, userAccount);
    }

    public void incrementTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        int currentNumberOfAttempts = ObjectUtils.firstNonNull(externalObjectDirectoryEntity.getTransferAttempts(), 0);
        int newNumberOfAttempts = currentNumberOfAttempts + 1;
        log.debug(
            "Updating failed transfer attempts from {} to {} for ID {}",
            currentNumberOfAttempts,
            newNumberOfAttempts,
            externalObjectDirectoryEntity.getId()
        );
        externalObjectDirectoryEntity.setTransferAttempts(newNumberOfAttempts);
        if (newNumberOfAttempts > armDataManagementConfiguration.getMaxRetryAttempts()) {
            logApi.armPushFailed(externalObjectDirectoryEntity.getId());
        }
    }

    public void updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity armExternalObjectDirectory, ObjectRecordStatusEntity armStatus,
                                                    UserAccountEntity userAccount) {
        if (nonNull(armExternalObjectDirectory)) {
            log.debug(
                "Updating ARM status from {} to {} for ID {}",
                armExternalObjectDirectory.getStatus().getId(),
                armStatus.getId(),
                armExternalObjectDirectory.getId()
            );
            armExternalObjectDirectory.setStatus(armStatus);
            armExternalObjectDirectory.setLastModifiedBy(userAccount);
            externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
        }
    }

    public ExternalObjectDirectoryEntity createArmExternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                                ObjectRecordStatusEntity status,
                                                                                UserAccountEntity userAccount) {
        ExternalObjectDirectoryEntity armExternalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        armExternalObjectDirectoryEntity.setExternalLocationType(EodHelper.armLocation());
        armExternalObjectDirectoryEntity.setStatus(status);
        armExternalObjectDirectoryEntity.setExternalLocation(externalObjectDirectory.getExternalLocation());
        armExternalObjectDirectoryEntity.setVerificationAttempts(1);

        if (nonNull(externalObjectDirectory.getMedia())) {
            armExternalObjectDirectoryEntity.setMedia(externalObjectDirectory.getMedia());
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            armExternalObjectDirectoryEntity.setTranscriptionDocumentEntity(externalObjectDirectory.getTranscriptionDocumentEntity());
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            armExternalObjectDirectoryEntity.setAnnotationDocumentEntity(externalObjectDirectory.getAnnotationDocumentEntity());
        } else if (nonNull(externalObjectDirectory.getCaseDocument())) {
            armExternalObjectDirectoryEntity.setCaseDocument(externalObjectDirectory.getCaseDocument());
        }
        armExternalObjectDirectoryEntity.setCreatedBy(userAccount);
        armExternalObjectDirectoryEntity.setLastModifiedBy(userAccount);
        armExternalObjectDirectoryEntity.setTransferAttempts(1);
        // This should only get set for DETS records
        armExternalObjectDirectoryEntity.setOsrUuid(externalObjectDirectory.getOsrUuid());
        return armExternalObjectDirectoryEntity;
    }

    public String generateRawFilename(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        final Long entityId = externalObjectDirectoryEntity.getId();
        final Integer transferAttempts = externalObjectDirectoryEntity.getTransferAttempts();

        Long documentId = 0L;
        if (nonNull(externalObjectDirectoryEntity.getMedia())) {
            documentId = externalObjectDirectoryEntity.getMedia().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getTranscriptionDocumentEntity())) {
            documentId = externalObjectDirectoryEntity.getTranscriptionDocumentEntity().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getAnnotationDocumentEntity())) {
            documentId = externalObjectDirectoryEntity.getAnnotationDocumentEntity().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getCaseDocument())) {
            documentId = externalObjectDirectoryEntity.getCaseDocument().getId();
        }

        return String.format("%s_%s_%s", entityId, documentId, transferAttempts);
    }

    public boolean copyUnstructuredRawDataToArm(ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory,
                                                ExternalObjectDirectoryEntity armExternalObjectDirectory,
                                                String filename,
                                                ObjectRecordStatusEntity previousStatus, UserAccountEntity userAccount) {
        try {
            if (previousStatus == null
                || ARM_RAW_DATA_FAILED.getId().equals(previousStatus.getId())
                || ARM_INGESTION.getId().equals(previousStatus.getId())) {
                Instant start = Instant.now();
                log.info("ARM PERFORMANCE PUSH START for EOD {} started at {}", armExternalObjectDirectory.getId(), start);

                log.info("About to push raw data to ARM for EOD {}", armExternalObjectDirectory.getId());
                armDataManagementApi.copyBlobDataToArm(unstructuredExternalObjectDirectory.getExternalLocation(), filename);
                log.info("Pushed raw data to ARM for EOD {}", armExternalObjectDirectory.getId());

                Instant finish = Instant.now();
                long timeElapsed = Duration.between(start, finish).toMillis();
                log.info("ARM PERFORMANCE PUSH END for EOD {} ended at {}", armExternalObjectDirectory.getId(), finish);
                log.info("ARM PERFORMANCE PUSH ELAPSED TIME for EOD {} took {} ms", armExternalObjectDirectory.getId(), timeElapsed);

                armExternalObjectDirectory.setChecksum(unstructuredExternalObjectDirectory.getChecksum());
                armExternalObjectDirectory.setExternalLocation(UUID.randomUUID().toString());
                armExternalObjectDirectory.setLastModifiedBy(userAccount);
                armExternalObjectDirectory.setStatus(EodHelper.armRawDataPushedStatus());
                externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
            }
        } catch (Exception e) {
            log.error("Error copying BLOB data for file {}", unstructuredExternalObjectDirectory.getExternalLocation(), e);
            if (e instanceof InterruptedException) {
                throw e;
            }
            return false;
        }

        return true;
    }

    public void updateExternalObjectDirectoryFailedTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity,
                                                                    UserAccountEntity userAccount) {
        incrementTransferAttempts(externalObjectDirectoryEntity);
        externalObjectDirectoryEntity.setLastModifiedBy(userAccount);
        externalObjectDirectoryEntity.setLastModifiedDateTime(OffsetDateTime.now());
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryEntity);
    }

    public String getArchiveRecordsFileName(String manifestFilePrefix) {
        String fileNameFormat = "%s_%s.%s";
        return String.format(fileNameFormat,
                             manifestFilePrefix,
                             UUID.randomUUID().toString(),
                             armDataManagementConfiguration.getFileExtension()
        );
    }

    public void updateArmEodToArmIngestionStatus(ExternalObjectDirectoryEntity armEod, ArmBatchItem batchItem,
                                                 ArmBatchItems batchItems,
                                                 String archiveRecordsFileName, UserAccountEntity userAccount,
                                                 ExternalLocationTypeEntity eodSourceLocation) {
        var matchingEntity = getExternalObjectDirectoryEntity(armEod, eodSourceLocation, EodHelper.storedStatus());
        if (matchingEntity.isPresent()) {
            batchItem.setSourceEod(matchingEntity.get());
            batchItems.add(batchItem);
            armEod.setManifestFile(archiveRecordsFileName);
            incrementTransferAttempts(armEod);
            updateExternalObjectDirectoryStatus(armEod, EodHelper.armIngestionStatus(), userAccount);
        } else {
            log.error("Unable to find matching external object directory for {} - manifest {}", armEod.getId(), archiveRecordsFileName);
            updateExternalObjectDirectoryFailedTransferAttempts(armEod, userAccount);
            throw new DartsException(MessageFormat.format("Unable to find matching external object directory for {0}", armEod.getId()));
        }
    }

    public ExternalObjectDirectoryEntity createArmEodWithArmIngestionStatus(ExternalObjectDirectoryEntity currentEod,
                                                                            ArmBatchItem batchItem,
                                                                            ArmBatchItems batchItems,
                                                                            String archiveRecordsFileName,
                                                                            UserAccountEntity userAccount) {
        ExternalObjectDirectoryEntity armEod = createArmExternalObjectDirectoryEntity(currentEod, EodHelper.armIngestionStatus(), userAccount);
        armEod.setManifestFile(archiveRecordsFileName);

        var savedArmEod = externalObjectDirectoryRepository.saveAndFlush(armEod);
        batchItem.setArmEod(savedArmEod);
        batchItem.setSourceEod(currentEod);
        batchItems.add(batchItem);

        return savedArmEod;
    }

    public boolean shouldPushRawDataToArm(ArmBatchItem batchItem) {
        return equalsAnyStatus(batchItem.getPreviousStatus(), EodHelper.armIngestionStatus(), EodHelper.failedArmRawDataStatus());
    }


    public boolean shouldAddEntryToManifestFile(ArmBatchItem batchItem) {
        return equalsAnyStatus(batchItem.getPreviousStatus(), EodHelper.failedArmManifestFileStatus(), EodHelper.armResponseManifestFailedStatus());
    }

    public String generateManifestFileContents(ArmBatchItems batchItems, String archiveRecordsFileName) {
        return archiveRecordFileGenerator.generateArchiveRecords(archiveRecordsFileName, batchItems.getArchiveRecords());
    }

    public void recoverByUpdatingEodToFailedArmStatus(ArmBatchItem batchItem, UserAccountEntity userAccount) {
        if (batchItem.getArmEod() != null) {
            logApi.armPushFailed(batchItem.getArmEod().getId());
            batchItem.undoManifestFileChange();
            if (batchItem.isRawFilePushNotNeededOrSuccessfulWhenNeeded()) {
                updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmManifestFileStatus(), userAccount);
            } else {
                updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmRawDataStatus(), userAccount);
            }
        }
    }


    public void copyMetadataToArm(String manifestFileContents, String archiveRecordsFileName) {
        try {
            BinaryData metadataFileBinary = convertStringToBinaryData(Optional.ofNullable(manifestFileContents).orElse(""));
            armDataManagementApi.saveBlobDataToArm(archiveRecordsFileName, metadataFileBinary);
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == BLOB_ALREADY_EXISTS_STATUS_CODE) {
                log.info("Metadata BLOB already exists", e);
            } else {
                log.error("Failed to move BLOB metadata for file {}", archiveRecordsFileName, e);
                throw e;
            }
        } catch (Exception e) {
            log.error("Unable to move BLOB metadata for file {}", archiveRecordsFileName, e);
            throw e;
        }
    }

    public BinaryData convertStringToBinaryData(String manifestFileContents) {
        return BinaryData.fromString(manifestFileContents);
    }
}
