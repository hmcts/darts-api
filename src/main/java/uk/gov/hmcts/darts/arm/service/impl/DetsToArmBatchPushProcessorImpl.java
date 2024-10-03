package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.DetsToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItem;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItems;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.DetsToArmBatchPushProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.util.EodHelper.isEqual;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetsToArmBatchPushProcessorImpl implements DetsToArmBatchPushProcessor {
    private final ArchiveRecordService archiveRecordService;
    private final DataStoreToArmHelper dataStoreToArmHelper;
    private final UserIdentity userIdentity;
    private final LogApi logApi;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementApi armDataManagementApi;
    private final DetsToArmProcessorConfiguration detsToArmProcessorConfiguration;
    private final ObjectStateRecordRepository objectStateRecordRepository;
    private final CurrentTimeHelper currentTimeHelper;

    private static final int BLOB_ALREADY_EXISTS_STATUS_CODE = 409;

    public void processDetsToArm(int taskBatchSize) {
        log.info("Started running DETS ARM Batch Push processing at: {}", OffsetDateTime.now());
        ExternalLocationTypeEntity eodSourceLocation = EodHelper.detsLocation();

        // Because the query is long-running, get all the EODs that need to be processed in one go
        List<ExternalObjectDirectoryEntity> eodsForTransfer = dataStoreToArmHelper
            .getDetsEodEntitiesToSendToArm(eodSourceLocation,
                                           EodHelper.armLocation(),
                                           taskBatchSize);

        log.info("Found {} DETS pending entities to process from source '{}'", eodsForTransfer.size(), eodSourceLocation.getDescription());
        if (!eodsForTransfer.isEmpty()) {
            //ARM has a max batch size for manifest items, so lets loop through the big list creating lots of individual batches for ARM to process separately
            List<List<ExternalObjectDirectoryEntity>> batchesForArm = ListUtils.partition(eodsForTransfer,
                                                                                          detsToArmProcessorConfiguration.getMaxArmManifestItems());
            int batchCounter = 1;
            UserAccountEntity userAccount = userIdentity.getUserAccount();
            for (List<ExternalObjectDirectoryEntity> eodsForBatch : batchesForArm) {
                log.info("Creating DETS batch {} out of {}", batchCounter++, batchesForArm.size());
                createAndSendBatchFile(eodsForBatch, userAccount);
            }
        }
        log.info("Finished running DETS ARM Batch Push processing at: {}", OffsetDateTime.now());
    }

    private void createAndSendBatchFile(List<ExternalObjectDirectoryEntity> eodsForBatch, UserAccountEntity userAccount) {
        File archiveRecordsFile = dataStoreToArmHelper.createEmptyArchiveRecordsFile(armDataManagementConfiguration.getManifestFilePrefix());
        var batchItems = new ArmBatchItems();

        for (var currentEod : eodsForBatch) {
            ObjectStateRecordEntity objectStateRecord = null;
            var batchItem = new ArmBatchItem();
            try {
                ExternalObjectDirectoryEntity armEod;
                if (isEqual(currentEod.getExternalLocationType(), EodHelper.armLocation())) {
                    //retry existing attempt that has previously failed.
                    armEod = currentEod;
                    batchItem.setArmEod(armEod);
                    dataStoreToArmHelper.updateArmEodToArmIngestionStatus(
                        currentEod, batchItem, batchItems, archiveRecordsFile, userAccount);
                    getObjectStateRecordEntity(currentEod);

                } else {

                    armEod = dataStoreToArmHelper.createArmEodWithArmIngestionStatus(
                        currentEod, batchItem, batchItems, archiveRecordsFile, userAccount);
                    objectStateRecord = getObjectStateRecordEntity(currentEod);
                    // Save the new ARM EOD to the object state record
                    if (objectStateRecord != null) {
                        objectStateRecord.setArmEodId(String.valueOf(armEod.getId()));
                        objectStateRecordRepository.save(objectStateRecord);
                    }
                }

                String rawFilename = dataStoreToArmHelper.generateRawFilename(armEod);

                if (dataStoreToArmHelper.shouldPushRawDataToArm(batchItem)) {
                    pushRawDataAndCreateArchiveRecordIfSuccess(batchItem, rawFilename, userAccount, objectStateRecord);
                } else if (dataStoreToArmHelper.shouldAddEntryToManifestFile(batchItem)) {
                    batchItem.setArchiveRecord(archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename));
                }
            } catch (Exception e) {
                log.error("Unable to batch push DETS EOD {} to ARM", currentEod.getId(), e);
                dataStoreToArmHelper.recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount);
            }
        }

        try {
            if (!batchItems.getSuccessful().isEmpty()) {
                dataStoreToArmHelper.writeManifestFile(batchItems, archiveRecordsFile);
                updateObjectStateRecordManifestSuccessOrFailure(batchItems, archiveRecordsFile);
                copyMetadataToArm(archiveRecordsFile);
            } else {
                log.warn("No EODs were able to be processed, skipping manifest file creation");
            }
        } catch (Exception e) {
            String errorMessage = String.format("Error during generation of DETS batch manifest file %s - %s", archiveRecordsFile.getName(), e.getMessage());
            log.error(errorMessage, e);
            batchItems.getSuccessful().forEach(batchItem -> dataStoreToArmHelper.recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount));
            batchItems.getFailed().forEach(batchItem -> updateObjectStateRecordStatus(batchItem.getArmEod(), errorMessage));
            return;
        }

        for (var batchItem : batchItems.getSuccessful()) {
            dataStoreToArmHelper.updateExternalObjectDirectoryStatus(batchItem.getArmEod(), EodHelper.armDropZoneStatus(), userAccount);
            logApi.armPushSuccessful(batchItem.getArmEod().getId());
        }
    }

    private void updateObjectStateRecordManifestSuccessOrFailure(ArmBatchItems batchItems, File archiveRecordsFile) {
        for (var batchItem : batchItems.getSuccessful()) {
            ObjectStateRecordEntity objectStateRecord = getObjectStateRecordEntity(batchItem.getArmEod());
            if (isNull(objectStateRecord.getObjectStatus())) {
                objectStateRecord.setFlagFileMfstCreated(true);
                objectStateRecord.setDateFileMfstCreated(currentTimeHelper.currentOffsetDateTime());
                objectStateRecord.setIdManifestFile(archiveRecordsFile.getName());
                objectStateRecord.setFlagMfstTransfToArml(true);
                objectStateRecord.setDateMfstTransfToArml(currentTimeHelper.currentOffsetDateTime());
                objectStateRecordRepository.save(objectStateRecord);
            }
            objectStateRecordRepository.save(objectStateRecord);
        }
        for (var batchItem : batchItems.getFailed()) {
            String errorMessage = "Manifest file creation failed";
            updateObjectStateRecordStatus(batchItem.getArmEod(), errorMessage);
        }

    }

    private void updateObjectStateRecordStatus(ExternalObjectDirectoryEntity externalObjectDirectory, String errorMessage) {
        ObjectStateRecordEntity objectStateRecord = getObjectStateRecordEntity(externalObjectDirectory);
        if (isNull(objectStateRecord)) {
            if (nonNull(objectStateRecord.getObjectStatus())) {
                objectStateRecord.setObjectStatus(objectStateRecord.getObjectStatus() + " " + errorMessage);
            } else {
                objectStateRecord.setObjectStatus(errorMessage);
            }
            objectStateRecordRepository.save(objectStateRecord);
        }
    }

    private ObjectStateRecordEntity getObjectStateRecordEntity(ExternalObjectDirectoryEntity externalObjectDirectory) {
        if (nonNull(externalObjectDirectory.getOsrUuid())) {
            return objectStateRecordRepository.findById(externalObjectDirectory.getOsrUuid())
                .orElseThrow(() -> new DartsException(
                    "Unable to find ObjectStateRecordEntity for ARM EOD ID: " + externalObjectDirectory.getId()
                        + " for OSR UUID " + externalObjectDirectory.getOsrUuid()));
        } else {
            throw new DartsException("Unable to find ObjectStateRecordEntity for ARM EOD ID: " + externalObjectDirectory.getId() + " as OSR UUID is null");
        }
    }

    private void pushRawDataAndCreateArchiveRecordIfSuccess(ArmBatchItem batchItem, String rawFilename, UserAccountEntity userAccount,
                                                            ObjectStateRecordEntity objectStateRecord) {
        log.info("Start of batch ARM Push processing for EOD {} running at: {}", batchItem.getArmEod().getId(), OffsetDateTime.now());
        boolean copyRawDataToArmSuccessful = copyRawDataToArm(
            batchItem.getSourceEod(),
            batchItem.getArmEod(),
            rawFilename,
            batchItem.getPreviousStatus(),
            userAccount,
            objectStateRecord
        );

        if (copyRawDataToArmSuccessful) {
            batchItem.setRawFilePushSuccessful(true);
            var archiveRecord = archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename);
            batchItem.setArchiveRecord(archiveRecord);
        } else {
            batchItem.setRawFilePushSuccessful(false);
            batchItem.undoManifestFileChange();
            dataStoreToArmHelper.updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmRawDataStatus(), userAccount);
        }
    }

    private boolean copyRawDataToArm(ExternalObjectDirectoryEntity detsExternalObjectDirectory,
                                     ExternalObjectDirectoryEntity armExternalObjectDirectory,
                                     String filename,
                                     ObjectRecordStatusEntity previousStatus, UserAccountEntity userAccount,
                                     ObjectStateRecordEntity objectStateRecord) {
        try {
            if (previousStatus == null
                || ARM_RAW_DATA_FAILED.getId().equals(previousStatus.getId())
                || ARM_INGESTION.getId().equals(previousStatus.getId())) {
                Instant start = Instant.now();
                log.info("ARM PERFORMANCE PUSH START for EOD {} started at {}", armExternalObjectDirectory.getId(), start);

                log.info("About to push raw data to ARM for EOD {}", armExternalObjectDirectory.getId());
                armDataManagementApi.copyDetsBlobDataToArm(detsExternalObjectDirectory.getExternalLocation().toString(), filename);
                log.info("Pushed raw data to ARM for EOD {}", armExternalObjectDirectory.getId());

                Instant finish = Instant.now();
                long timeElapsed = Duration.between(start, finish).toMillis();
                log.info("ARM PERFORMANCE PUSH END for EOD {} ended at {}", armExternalObjectDirectory.getId(), finish);
                log.info("ARM PERFORMANCE PUSH ELAPSED TIME for EOD {} took {} ms", armExternalObjectDirectory.getId(), timeElapsed);

                armExternalObjectDirectory.setChecksum(detsExternalObjectDirectory.getChecksum());
                armExternalObjectDirectory.setExternalLocation(UUID.randomUUID());
                armExternalObjectDirectory.setLastModifiedBy(userAccount);
                externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);

                if (nonNull(objectStateRecord)) {
                    objectStateRecord.setFlagFileTransferToDets(true);
                    objectStateRecord.setDateFileTransferToDets(currentTimeHelper.currentOffsetDateTime());
                    objectStateRecord.setMd5FileTransfArml(detsExternalObjectDirectory.getChecksum());
                    setFileSize(detsExternalObjectDirectory, objectStateRecord);
                    objectStateRecordRepository.save(objectStateRecord);
                }
            }
        } catch (Exception e) {
            String errorMessage = String.format("Error copying BLOB data for file %s - %s", detsExternalObjectDirectory.getExternalLocation(), e.getMessage());
            log.error("Error copying BLOB data for file {}", detsExternalObjectDirectory.getExternalLocation(), e);
            updateObjectStateRecordStatus(armExternalObjectDirectory, errorMessage);
            return false;
        }

        return true;
    }

    private void copyMetadataToArm(File manifestFile) {
        try {
            BinaryData metadataFileBinary = fileOperationService.convertFileToBinaryData(manifestFile.getAbsolutePath());
            armDataManagementApi.saveBlobDataToArm(manifestFile.getName(), metadataFileBinary);
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == BLOB_ALREADY_EXISTS_STATUS_CODE) {
                log.info("Metadata BLOB already exists", e);
            } else {
                log.error("Failed to move BLOB metadata for file {}", manifestFile.getAbsolutePath(), e);
                throw e;
            }
        } catch (Exception e) {
            log.error("Unable to move BLOB metadata for file {}", manifestFile.getAbsolutePath(), e);
            throw e;
        }
    }

    private static void setFileSize(ExternalObjectDirectoryEntity detsExternalObjectDirectory, ObjectStateRecordEntity objectStateRecord) {
        if (nonNull(detsExternalObjectDirectory.getMedia())) {
            objectStateRecord.setFileSizeBytesArml(detsExternalObjectDirectory.getMedia().getFileSize());
        } else if (nonNull(detsExternalObjectDirectory.getAnnotationDocumentEntity())) {
            objectStateRecord.setFileSizeBytesArml(Long.valueOf(detsExternalObjectDirectory.getAnnotationDocumentEntity().getFileSize()));
        } else if (nonNull(detsExternalObjectDirectory.getTranscriptionDocumentEntity())) {
            objectStateRecord.setFileSizeBytesArml(Long.valueOf(detsExternalObjectDirectory.getTranscriptionDocumentEntity().getFileSize()));
        } else if (nonNull(detsExternalObjectDirectory.getCaseDocument())) {
            objectStateRecord.setFileSizeBytesArml(Long.valueOf(detsExternalObjectDirectory.getCaseDocument().getFileSize()));
        }

    }
}