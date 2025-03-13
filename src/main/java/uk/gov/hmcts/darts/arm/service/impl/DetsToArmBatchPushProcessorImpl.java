package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.DetsToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItem;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItems;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.DetsToArmBatchPushProcessor;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
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
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.DetsToArmPushAutomatedTaskConfig;
import uk.gov.hmcts.darts.util.AsyncUtil;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final ArmDataManagementApi armDataManagementApi;
    private final DetsToArmProcessorConfiguration detsToArmProcessorConfiguration;
    private final ObjectStateRecordRepository objectStateRecordRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final ExternalObjectDirectoryService externalObjectDirectoryService;
    private final DetsToArmPushAutomatedTaskConfig automatedTaskConfigurationProperties;


    @Override
    public void processDetsToArm(int taskBatchSize) {
        log.info("Started running DETS ARM Batch Push processing at: {}", OffsetDateTime.now());
        ExternalLocationTypeEntity eodSourceLocation = EodHelper.detsLocation();

        // Because the query is long-running, get all the EODs that need to be processed in one go
        List<Integer> eodsForTransfer = getDetsEodEntitiesToSendToArm(eodSourceLocation,
                                                                      EodHelper.armLocation(),
                                                                      taskBatchSize);

        log.info("Found {} DETS pending entities to process from source '{}'", eodsForTransfer.size(), eodSourceLocation.getDescription());
        if (CollectionUtils.isNotEmpty(eodsForTransfer)) {
            //ARM has a max batch size for manifest items, so lets loop through the big list creating lots of individual batches for ARM to process separately
            List<List<Integer>> batchesForArm = ListUtils.partition(eodsForTransfer,
                                                                    detsToArmProcessorConfiguration.getMaxArmManifestItems());
            UserAccountEntity userAccount = userIdentity.getUserAccount();

            AtomicInteger batchCounter = new AtomicInteger(1);
            List<Callable<Void>> tasks = batchesForArm
                .stream()
                .map(eodsForBatch -> (Callable<Void>) () -> {
                    int batchNumber = batchCounter.getAndIncrement();
                    try {
                        log.info("Creating DETS batch {} out of {}", batchNumber, batchesForArm.size());
                        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = externalObjectDirectoryRepository.findAllById(eodsForBatch);
                        log.info("Starting processing DETS batch {} out of {}", batchNumber, batchesForArm.size());
                        createAndSendBatchFile(externalObjectDirectoryEntities, userAccount);
                        log.info("Finished processing DETS batch {} out of {}", batchNumber, batchesForArm.size());
                    } catch (Exception e) {
                        log.error("Unexpected exception when processing DETS batch {}", batchNumber, e);
                    }
                    return null;
                })
                .toList();

            try {
                AsyncUtil.invokeAllAwaitTermination(tasks, automatedTaskConfigurationProperties);
            } catch (InterruptedException e) {
                log.error("DETS to ARM batch processing interrupted", e);
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("DETS to ARM batch unexpected exception", e);
                return;
            }
        } else {
            log.info("No DETS EODs to process");
        }
        log.info("Finished running DETS ARM Batch Push processing at: {}", OffsetDateTime.now());
    }

    List<Integer> getDetsEodEntitiesToSendToArm(ExternalLocationTypeEntity sourceLocation,
                                                ExternalLocationTypeEntity armLocation, int maxResultSize) {
        ObjectRecordStatusEntity armRawStatusFailed = EodHelper.failedArmRawDataStatus();
        ObjectRecordStatusEntity armManifestFailed = EodHelper.failedArmManifestFileStatus();

        List<ObjectRecordStatusEntity> failedArmStatuses = List.of(armRawStatusFailed, armManifestFailed);

        var failedArmExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocationForDets(
            failedArmStatuses,
            armLocation,
            armDataManagementConfiguration.getMaxRetryAttempts(),
            Limit.of(maxResultSize)
        );

        List<Integer> returnList = new ArrayList<>(failedArmExternalObjectDirectoryEntities);

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

    private void createAndSendBatchFile(List<ExternalObjectDirectoryEntity> eodsForBatch, UserAccountEntity userAccount) {
        String archiveRecordsFileName = dataStoreToArmHelper.getArchiveRecordsFileName(detsToArmProcessorConfiguration.getManifestFilePrefix());

        var batchItems = new ArmBatchItems();

        for (var currentEod : eodsForBatch) {
            ObjectStateRecordEntity objectStateRecord;
            var batchItem = new ArmBatchItem();
            try {
                ExternalObjectDirectoryEntity armEod;
                if (isEqual(currentEod.getExternalLocationType(), EodHelper.armLocation())) {
                    //retry existing attempt that has previously failed.
                    armEod = currentEod;
                    batchItem.setArmEod(armEod);
                    dataStoreToArmHelper.updateArmEodToArmIngestionStatus(
                        currentEod, batchItem, batchItems, archiveRecordsFileName, userAccount, EodHelper.detsLocation());
                    objectStateRecord = getObjectStateRecordEntity(armEod);
                    // Reset the failed status from previous attempt
                    objectStateRecord.setObjectStatus(null);
                    objectStateRecordRepository.save(objectStateRecord);
                } else {
                    armEod = dataStoreToArmHelper.createArmEodWithArmIngestionStatus(
                        currentEod, batchItem, batchItems, archiveRecordsFileName, userAccount);
                    objectStateRecord = getObjectStateRecordEntity(currentEod);
                    // Save the new ARM EOD to the object state record
                    objectStateRecord.setArmEodId(armEod.getId());
                    objectStateRecordRepository.save(objectStateRecord);
                }

                String rawFilename = dataStoreToArmHelper.generateRawFilename(armEod);

                if (dataStoreToArmHelper.shouldPushRawDataToArm(batchItem)) {
                    pushRawDataAndCreateArchiveRecordIfSuccess(batchItem, rawFilename, userAccount, objectStateRecord);
                } else if (dataStoreToArmHelper.shouldAddEntryToManifestFile(batchItem)) {
                    batchItem.setArchiveRecord(archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename));
                }
            } catch (Exception e) {
                StringBuilder errorMessage = new StringBuilder(String.format("Error during batch push DETS EOD %s to ARM", currentEod.getId()));
                log.error(errorMessage.toString(), e);
                dataStoreToArmHelper.recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount);
                errorMessage.append(" - ").append(ExceptionUtils.getStackTrace(e));
                updateObjectStateRecordStatus(batchItem.getArmEod(), errorMessage.toString());
            }
        }

        if (!writeManifestAndCopyToArm(userAccount, batchItems, archiveRecordsFileName)) {
            return;
        }

        updateBatchedItemsStatuses(userAccount, batchItems);
    }

    private void updateBatchedItemsStatuses(UserAccountEntity userAccount, ArmBatchItems batchItems) {
        for (var batchItem : batchItems.getItems()) {
            if (batchItem.isRawFilePushNotNeededOrSuccessfulWhenNeeded() && batchItem.getArchiveRecord() != null) {
                dataStoreToArmHelper.updateExternalObjectDirectoryStatus(batchItem.getArmEod(), EodHelper.armDropZoneStatus(), userAccount);
                logApi.armPushSuccessful(batchItem.getArmEod().getId());
            } else {
                dataStoreToArmHelper.recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount);
            }
        }
    }

    private boolean writeManifestAndCopyToArm(UserAccountEntity userAccount, ArmBatchItems batchItems, String archiveRecordsFileName) {
        try {

            if (CollectionUtils.isNotEmpty(batchItems.getSuccessful())) {
                String archiveRecordsContents = dataStoreToArmHelper.generateManifestFileContents(batchItems, archiveRecordsFileName);

                updateObjectStateRecordManifestCreated(batchItems, archiveRecordsFileName);
                dataStoreToArmHelper.copyMetadataToArm(archiveRecordsContents, archiveRecordsFileName);
                updateObjectStateRecordManifestSuccessOrFailure(batchItems, true);
            } else {
                log.warn("No EODs were able to be processed, skipping manifest file creation");
            }

        } catch (Exception e) {
            String errorMessage = String.format("Error during generation of DETS batch manifest file %s", archiveRecordsFileName);
            log.error(errorMessage, e);
            batchItems.getSuccessful().forEach(batchItem -> dataStoreToArmHelper.recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount));
            final String errorMessageWithStackTrace = errorMessage + " - " + ExceptionUtils.getStackTrace(e);
            batchItems.getFailed().forEach(batchItem -> updateObjectStateRecordStatus(batchItem.getArmEod(), errorMessageWithStackTrace));
            updateObjectStateRecordManifestSuccessOrFailure(batchItems, false);
            return false;
        }
        return true;
    }

    private void updateObjectStateRecordManifestCreated(ArmBatchItems batchItems, String archiveRecordsFileName) {
        for (var batchItem : batchItems.getSuccessful()) {
            ObjectStateRecordEntity objectStateRecord = getObjectStateRecordEntity(batchItem.getArmEod());
            objectStateRecord.setFlagFileMfstCreated(true);
            objectStateRecord.setDateFileMfstCreated(currentTimeHelper.currentOffsetDateTime());
            objectStateRecord.setIdManifestFile(archiveRecordsFileName);
            objectStateRecordRepository.save(objectStateRecord);
        }

        for (var batchItem : batchItems.getFailed()) {
            String errorMessage = "Manifest file creation failed";
            updateObjectStateRecordStatus(batchItem.getArmEod(), errorMessage);
        }
    }

    private void updateObjectStateRecordManifestSuccessOrFailure(ArmBatchItems batchItems, boolean isSuccessfulTransfer) {
        for (var batchItem : batchItems.getSuccessful()) {
            ObjectStateRecordEntity objectStateRecord = getObjectStateRecordEntity(batchItem.getArmEod());
            objectStateRecord.setFlagMfstTransfToArml(isSuccessfulTransfer);
            objectStateRecord.setDateMfstTransfToArml(currentTimeHelper.currentOffsetDateTime());
            objectStateRecordRepository.save(objectStateRecord);
        }

        for (var batchItem : batchItems.getFailed()) {
            String errorMessage = "Manifest file transfer to ARM failed";
            updateObjectStateRecordStatus(batchItem.getArmEod(), errorMessage);
        }
    }

    private void updateObjectStateRecordStatus(ExternalObjectDirectoryEntity externalObjectDirectory, String errorMessage) {
        ObjectStateRecordEntity objectStateRecord = getObjectStateRecordEntity(externalObjectDirectory);
        if (nonNull(objectStateRecord)) {
            if (nonNull(objectStateRecord.getObjectStatus())) {
                objectStateRecord.setObjectStatus(objectStateRecord.getObjectStatus() + " " + errorMessage);
            } else {
                objectStateRecord.setObjectStatus(errorMessage);
            }
            objectStateRecordRepository.save(objectStateRecord);
        } else if (nonNull(externalObjectDirectory)) {
            log.error("Unable to find ObjectStateRecordEntity for ARM EOD ID: {}", externalObjectDirectory.getId());
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
                log.info("ARM PERFORMANCE PUSH START for DETS EOD {} started at {}", armExternalObjectDirectory.getId(), start);

                log.info("About to push raw data to ARM for DETS EOD {}", armExternalObjectDirectory.getId());
                armDataManagementApi.copyDetsBlobDataToArm(detsExternalObjectDirectory.getExternalLocation(), filename);
                log.info("Pushed raw data to ARM for DETS EOD {}", armExternalObjectDirectory.getId());

                Instant finish = Instant.now();
                long timeElapsed = Duration.between(start, finish).toMillis();
                log.info("ARM PERFORMANCE PUSH END for DETS EOD {} ended at {}", armExternalObjectDirectory.getId(), finish);
                log.info("ARM PERFORMANCE PUSH ELAPSED TIME for DETS EOD {} took {} ms", armExternalObjectDirectory.getId(), timeElapsed);

                armExternalObjectDirectory.setChecksum(detsExternalObjectDirectory.getChecksum());
                armExternalObjectDirectory.setExternalLocation(UUID.randomUUID().toString());
                armExternalObjectDirectory.setLastModifiedBy(userAccount);
                externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);

                setRawDataPushed(detsExternalObjectDirectory, objectStateRecord);
            }
        } catch (Exception e) {
            String errorMessage = String.format("Error copying DETS BLOB data for file %s - %s", detsExternalObjectDirectory.getExternalLocation(),
                                                e.getMessage());
            log.error("Error copying DETS BLOB data for file {}", detsExternalObjectDirectory.getExternalLocation(), e);
            updateObjectStateRecordStatus(armExternalObjectDirectory, errorMessage);
            return false;
        }

        return true;
    }

    private void setRawDataPushed(ExternalObjectDirectoryEntity detsExternalObjectDirectory, ObjectStateRecordEntity objectStateRecord) {
        objectStateRecord.setFlagFileTransfToarml(true);
        objectStateRecord.setDateFileTransfToarml(currentTimeHelper.currentOffsetDateTime());
        objectStateRecord.setMd5FileTransfArml(detsExternalObjectDirectory.getChecksum());
        setFileSize(detsExternalObjectDirectory, objectStateRecord);
        objectStateRecordRepository.save(objectStateRecord);
    }

    private void setFileSize(ExternalObjectDirectoryEntity detsExternalObjectDirectory, ObjectStateRecordEntity objectStateRecord) {
        Long fileSize = externalObjectDirectoryService.getFileSize(detsExternalObjectDirectory);
        if (nonNull(fileSize)) {
            objectStateRecord.setFileSizeBytesArml(fileSize);
        }
    }
}