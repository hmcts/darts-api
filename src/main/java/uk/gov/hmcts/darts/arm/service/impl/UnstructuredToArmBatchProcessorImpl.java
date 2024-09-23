package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.UnstructuredToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.UnstructuredToArmHelper;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmBatchProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.util.EodHelper.equalsAnyStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.isEqual;


@Slf4j
@Component
@RequiredArgsConstructor
public class UnstructuredToArmBatchProcessorImpl implements UnstructuredToArmBatchProcessor {
    private final ArchiveRecordService archiveRecordService;
    private final ArchiveRecordFileGenerator archiveRecordFileGenerator;
    private final UnstructuredToArmHelper unstructuredToArmHelper;
    private final UserIdentity userIdentity;
    private final LogApi logApi;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementApi armDataManagementApi;
    private final UnstructuredToArmProcessorConfiguration unstructuredToArmProcessorConfiguration;
    private final EodHelper eodHelper;

    private static final int BLOB_ALREADY_EXISTS_STATUS_CODE = 409;

    private static final String DARTS_STRING = "darts";
    private static final String DETS_STRING = "dets";

    @Override
    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
    public void processUnstructuredToArm(int armBatchSize) {

        log.info("Started running ARM Batch Push processing at: {}", OffsetDateTime.now());
        ExternalLocationTypeEntity eodSourceLocation = getEodSourceLocation();

        int querySize = unstructuredToArmProcessorConfiguration.getMaxArmManifestItems();
        if (armBatchSize < unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()) {
            querySize = armBatchSize;
        }

        UserAccountEntity userAccount = userIdentity.getUserAccount();

        for (int batchCounter = 1; batchCounter <= armBatchSize; batchCounter += querySize) {
            log.info("Batching rows from {} out of a potential rows {}", batchCounter, armBatchSize);
            List<ExternalObjectDirectoryEntity> batchedEods =
                unstructuredToArmHelper.getEodEntitiesToSendToArm(eodSourceLocation, EodHelper.armLocation(), querySize);
            log.info("Number of EODs found {}", batchedEods.size());

            if (!batchedEods.isEmpty()) {
                createAndSendBatchFile(batchedEods, userAccount);
                if (batchedEods.size() <= (batchCounter + querySize)) {
                    break;
                }
            } else {
                break;
            }
        }

        log.info("Finished running ARM Batch Push processing at: {}", OffsetDateTime.now());
    }

    private void createAndSendBatchFile(List<ExternalObjectDirectoryEntity> eodsForBatch, UserAccountEntity userAccount) {

        File archiveRecordsFile = createEmptyArchiveRecordsFile();
        var batchItems = new BatchItems();

        for (var currentEod : eodsForBatch) {

            var batchItem = new BatchItem();
            try {
                ExternalObjectDirectoryEntity armEod;
                if (isEqual(currentEod.getExternalLocationType(), EodHelper.armLocation())) {
                    //retry existing attempt that has previously failed.
                    armEod = currentEod;
                    batchItem.setArmEod(armEod);
                    updateArmEodToArmIngestionStatus(currentEod, batchItem, batchItems, archiveRecordsFile, userAccount);
                } else {
                    armEod = createArmEodWithArmIngestionStatus(currentEod, batchItem, batchItems, archiveRecordsFile, userAccount);
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

    private ExternalLocationTypeEntity getEodSourceLocation() {
        var armClient = armDataManagementConfiguration.getArmClient();
        if (DARTS_STRING.equalsIgnoreCase(armClient)) {
            return EodHelper.unstructuredLocation();
        } else if (DETS_STRING.equalsIgnoreCase(armClient)) {
            return EodHelper.detsLocation();
        } else {
            throw new DartsException(String.format("Invalid arm client '%s'", armClient));
        }
    }

    private void updateArmEodToArmIngestionStatus(ExternalObjectDirectoryEntity armEod, BatchItem batchItem, BatchItems batchItems,
                                                  File archiveRecordsFile, UserAccountEntity userAccount) {
        var matchingEntity = unstructuredToArmHelper.getExternalObjectDirectoryEntity(armEod, getEodSourceLocation(), EodHelper.storedStatus());
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

    private ExternalObjectDirectoryEntity createArmEodWithArmIngestionStatus(ExternalObjectDirectoryEntity currentEod, BatchItem batchItem,
                                                                             BatchItems batchItems,
                                                                             File archiveRecordsFile, UserAccountEntity userAccount) {
        ExternalObjectDirectoryEntity armEod;
        armEod = unstructuredToArmHelper.createArmExternalObjectDirectoryEntity(currentEod, EodHelper.armIngestionStatus(), userAccount);
        batchItem.setArmEod(armEod);
        batchItem.setSourceEod(currentEod);
        batchItems.add(batchItem);
        armEod.setManifestFile(archiveRecordsFile.getName());
        externalObjectDirectoryRepository.saveAndFlush(armEod);
        return armEod;
    }

    @SneakyThrows
    private File createEmptyArchiveRecordsFile() {
        var fileNameFormat = "%s_%s.%s";
        var fileName = String.format(fileNameFormat,
                                     armDataManagementConfiguration.getManifestFilePrefix(),
                                     UUID.randomUUID(),
                                     armDataManagementConfiguration.getFileExtension()
        );
        Path filePath = fileOperationService.createFile(fileName, armDataManagementConfiguration.getTempBlobWorkspace(), true);
        log.info("Created empty archive records file {}", filePath.getFileName());
        return filePath.toFile();
    }

    private boolean shouldPushRawDataToArm(BatchItem batchItem) {
        return equalsAnyStatus(batchItem.getPreviousStatus(), EodHelper.armIngestionStatus(), eodHelper.failedArmRawDataStatus());
    }

    private void pushRawDataAndCreateArchiveRecordIfSuccess(BatchItem batchItem, String rawFilename, UserAccountEntity userAccount) {
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
            unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), eodHelper.failedArmRawDataStatus(), userAccount);
        }
    }

    private boolean shouldAddEntryToManifestFile(BatchItem batchItem) {
        return equalsAnyStatus(batchItem.getPreviousStatus(), EodHelper.failedArmManifestFileStatus(), EodHelper.failedArmResponseManifestFileStatus());
    }

    private void writeManifestFile(BatchItems batchItems, File archiveRecordsFile) {
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
    private void recoverByUpdatingEodToFailedArmStatus(BatchItem batchItem, UserAccountEntity userAccount) {
        if (batchItem.getArmEod() != null) {
            logApi.armPushFailed(batchItem.getArmEod().getId());
            batchItem.undoManifestFileChange();
            if (!batchItem.isRawFilePushNotNeededOrSuccessfulWhenNeeded()) {
                unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), eodHelper.failedArmRawDataStatus(), userAccount);
            } else {
                unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmManifestFileStatus(),
                                                                                    userAccount);
            }
        }
    }

    /**
     * Contains info related to the processing of a batch item.
     */
    @Data
    static class BatchItem {

        private ExternalObjectDirectoryEntity sourceEod;
        private ExternalObjectDirectoryEntity armEod;
        private String previousManifestFile;
        private ObjectRecordStatusEntity previousStatus;
        private Boolean rawFilePushSuccessful;
        private ArchiveRecord archiveRecord;

        public void setArmEod(ExternalObjectDirectoryEntity armEod) {
            this.armEod = armEod;
            this.previousManifestFile = armEod.getManifestFile();
            this.previousStatus = armEod.getStatus();
        }

        public void undoManifestFileChange() {
            this.armEod.setManifestFile(this.previousManifestFile);
        }

        public boolean isRawFilePushNotNeededOrSuccessfulWhenNeeded() {
            return rawFilePushSuccessful == null || rawFilePushSuccessful;
        }
    }

    static class BatchItems {

        private final List<BatchItem> items = new ArrayList<>();

        public void add(BatchItem batchItem) {
            items.add(batchItem);
        }

        public List<BatchItem> getSuccessful() {
            return items.stream().filter(
                batchItem -> batchItem.isRawFilePushNotNeededOrSuccessfulWhenNeeded() && batchItem.getArchiveRecord() != null).toList();
        }

        public List<ArchiveRecord> getArchiveRecords() {
            return getSuccessful().stream().map(BatchItem::getArchiveRecord).toList();
        }
    }

}
