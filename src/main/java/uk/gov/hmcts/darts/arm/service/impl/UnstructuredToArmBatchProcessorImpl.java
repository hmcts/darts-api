package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.UnstructuredToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItem;
import uk.gov.hmcts.darts.arm.model.batch.ArmBatchItems;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmBatchProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.List;

import static uk.gov.hmcts.darts.common.util.EodHelper.equalsAnyStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.isEqual;


@Slf4j
@Component
@RequiredArgsConstructor
public class UnstructuredToArmBatchProcessorImpl implements UnstructuredToArmBatchProcessor {
    private final ArchiveRecordService archiveRecordService;
    private final ArchiveRecordFileGenerator archiveRecordFileGenerator;
    private final DataStoreToArmHelper unstructuredToArmHelper;
    private final UserIdentity userIdentity;
    private final LogApi logApi;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementApi armDataManagementApi;
    private final UnstructuredToArmProcessorConfiguration unstructuredToArmProcessorConfiguration;
    private final EodHelper eodHelper;

    private static final int BLOB_ALREADY_EXISTS_STATUS_CODE = 409;


    @Override
    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})

    public void processUnstructuredToArm(int taskBatchSize) {

        log.info("Started running ARM Batch Push processing at: {}", OffsetDateTime.now());

        ExternalLocationTypeEntity eodSourceLocation = EodHelper.unstructuredLocation();

        // Because the query is long-running, get all the EODs that need to be processed in one go
        List<ExternalObjectDirectoryEntity> eodsForTransfer = unstructuredToArmHelper
            .getEodEntitiesToSendToArm(eodSourceLocation,
                                       EodHelper.armLocation(),
                                       taskBatchSize);

        log.info("Found {} pending entities to process from source '{}'", eodsForTransfer.size(), eodSourceLocation.getDescription());
        if (!eodsForTransfer.isEmpty()) {
            //ARM has a max batch size for manifest items, so lets loop through the big list creating lots of individual batches for ARM to process separately
            List<List<ExternalObjectDirectoryEntity>> batchesForArm = ListUtils.partition(eodsForTransfer,
                                                                                          unstructuredToArmProcessorConfiguration.getMaxArmManifestItems());
            int batchCounter = 1;
            UserAccountEntity userAccount = userIdentity.getUserAccount();
            for (List<ExternalObjectDirectoryEntity> eodsForBatch : batchesForArm) {
                log.info("Creating batch {} out of {}", batchCounter++, batchesForArm.size());
                createAndSendBatchFile(eodsForBatch, userAccount);
            }
        }
        log.info("Finished running ARM Batch Push processing at: {}", OffsetDateTime.now());
    }

    private void createAndSendBatchFile(List<ExternalObjectDirectoryEntity> eodsForBatch, UserAccountEntity userAccount) {

        File archiveRecordsFile = unstructuredToArmHelper.createEmptyArchiveRecordsFile(armDataManagementConfiguration.getManifestFilePrefix());
        var batchItems = new ArmBatchItems();

        for (var currentEod : eodsForBatch) {

            var batchItem = new ArmBatchItem();
            try {
                ExternalObjectDirectoryEntity armEod;
                if (isEqual(currentEod.getExternalLocationType(), EodHelper.armLocation())) {
                    //retry existing attempt that has previously failed.
                    armEod = currentEod;
                    batchItem.setArmEod(armEod);
                    updateArmEodToArmIngestionStatus(currentEod, batchItem, batchItems, archiveRecordsFile, userAccount);
                } else {
                    armEod = unstructuredToArmHelper.createArmEodWithArmIngestionStatus(currentEod, batchItem, batchItems, archiveRecordsFile, userAccount);
                }

                String rawFilename = unstructuredToArmHelper.generateRawFilename(armEod);

                if (shouldPushRawDataToArm(batchItem)) {
                    pushRawDataAndCreateArchiveRecordIfSuccess(batchItem, rawFilename, userAccount);
                } else if (shouldAddEntryToManifestFile(batchItem)) {
                    batchItem.setArchiveRecord(archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename));
                }
            } catch (Exception e) {
                log.error("Unable to batch push EOD {} to ARM", currentEod.getId(), e);
                recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount);
            }
        }

        try {
            if (!batchItems.getSuccessful().isEmpty()) {
                writeManifestFile(batchItems, archiveRecordsFile);
                copyMetadataToArm(archiveRecordsFile);
            }
        } catch (Exception e) {
            log.error("Error during generation of batch manifest file {}", archiveRecordsFile.getName(), e);
            batchItems.getSuccessful().forEach(batchItem -> recoverByUpdatingEodToFailedArmStatus(batchItem, userAccount));
            return;
        }

        for (var batchItem : batchItems.getSuccessful()) {
            unstructuredToArmHelper.updateExternalObjectDirectoryStatus(batchItem.getArmEod(), EodHelper.armDropZoneStatus(), userAccount);
            logApi.armPushSuccessful(batchItem.getArmEod().getId());
        }
    }

    private void updateArmEodToArmIngestionStatus(ExternalObjectDirectoryEntity armEod, ArmBatchItem batchItem, ArmBatchItems batchItems,
                                                  File archiveRecordsFile, UserAccountEntity userAccount) {
        var matchingEntity = unstructuredToArmHelper.getExternalObjectDirectoryEntity(armEod, EodHelper.unstructuredLocation(), EodHelper.storedStatus());
        if (matchingEntity.isPresent()) {
            batchItem.setSourceEod(matchingEntity.get());
            batchItems.add(batchItem);
            armEod.setManifestFile(archiveRecordsFile.getName());
            unstructuredToArmHelper.incrementTransferAttempts(armEod);
            unstructuredToArmHelper.updateExternalObjectDirectoryStatus(armEod, EodHelper.armIngestionStatus(), userAccount);
        } else {
            log.error("Unable to find matching external object directory for {}", armEod.getId());
            unstructuredToArmHelper.updateExternalObjectDirectoryFailedTransferAttempts(armEod, userAccount);
            throw new RuntimeException(MessageFormat.format("Unable to find matching external object directory for {0}", armEod.getId()));
        }
    }

    private boolean shouldPushRawDataToArm(ArmBatchItem batchItem) {
        return equalsAnyStatus(batchItem.getPreviousStatus(), EodHelper.armIngestionStatus(), EodHelper.failedArmRawDataStatus());
    }

    private void pushRawDataAndCreateArchiveRecordIfSuccess(ArmBatchItem batchItem, String rawFilename, UserAccountEntity userAccount) {
        log.info("Start of batch ARM Push processing for EOD {} running at: {}", batchItem.getArmEod().getId(), OffsetDateTime.now());
        boolean copyRawDataToArmSuccessful = unstructuredToArmHelper.copyRawDataToArm(
            batchItem.getSourceEod(),
            batchItem.getArmEod(),
            rawFilename,
            batchItem.getPreviousStatus(),
            userAccount
        );

        if (copyRawDataToArmSuccessful) {
            batchItem.setRawFilePushSuccessful(true);
            var archiveRecord = archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename);
            batchItem.setArchiveRecord(archiveRecord);
        } else {
            batchItem.setRawFilePushSuccessful(false);
            batchItem.undoManifestFileChange();
            unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmRawDataStatus(), userAccount);
        }
    }

    private boolean shouldAddEntryToManifestFile(ArmBatchItem batchItem) {
        return equalsAnyStatus(batchItem.getPreviousStatus(), EodHelper.failedArmManifestFileStatus(), EodHelper.failedArmResponseManifestFileStatus());
    }

    private void writeManifestFile(ArmBatchItems batchItems, File archiveRecordsFile) {
        archiveRecordFileGenerator.generateArchiveRecords(batchItems.getArchiveRecords(), archiveRecordsFile);
    }

    protected void copyMetadataToArm(File manifestFile) {
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

    @SuppressWarnings({"PMD.ConfusingTernary"})
    private void recoverByUpdatingEodToFailedArmStatus(ArmBatchItem batchItem, UserAccountEntity userAccount) {
        if (batchItem.getArmEod() != null) {
            logApi.armPushFailed(batchItem.getArmEod().getId());
            batchItem.undoManifestFileChange();
            if (!batchItem.isRawFilePushNotNeededOrSuccessfulWhenNeeded()) {
                unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmRawDataStatus(), userAccount);
            } else {
                unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmManifestFileStatus(),
                                                                                    userAccount);
            }
        }
    }
    
}