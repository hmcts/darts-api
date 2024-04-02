package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.File;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.util.EodHelper.equalsAnyStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.isEqual;


@Service
@Slf4j
@ConditionalOnExpression("${darts.storage.arm.batch-size} > 0")
public class UnstructuredToArmBatchProcessorImpl extends AbstractUnstructuredToArmProcessor {
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ArchiveRecordService archiveRecordService;
    private final ExternalObjectDirectoryService eodService;
    private final ArchiveRecordFileGenerator archiveRecordFileGenerator;

    public UnstructuredToArmBatchProcessorImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                               ObjectRecordStatusRepository objectRecordStatusRepository,
                                               ExternalLocationTypeRepository externalLocationTypeRepository,
                                               DataManagementApi dataManagementApi,
                                               ArmDataManagementApi armDataManagementApi,
                                               UserIdentity userIdentity,
                                               ArmDataManagementConfiguration armDataManagementConfiguration,
                                               FileOperationService fileOperationService,
                                               ArchiveRecordService archiveRecordService,
                                               ExternalObjectDirectoryService eodService,
                                               ArchiveRecordFileGenerator archiveRecordFileGenerator) {
        super(objectRecordStatusRepository,
              userIdentity,
              externalObjectDirectoryRepository,
              externalLocationTypeRepository,
              dataManagementApi,
              armDataManagementApi,
              fileOperationService);
        this.armDataManagementConfiguration = armDataManagementConfiguration;
        this.archiveRecordService = archiveRecordService;
        this.eodService = eodService;
        this.archiveRecordFileGenerator = archiveRecordFileGenerator;
    }

    @Override
    public void processUnstructuredToArm() {

        log.info("Started running ARM Batch Push processing at: {}", OffsetDateTime.now());

        List<ExternalObjectDirectoryEntity> allPendingSourceToArmEntities = getArmExternalObjectDirectoryEntities(
            armDataManagementConfiguration.getBatchSize()
        );

        log.info("Found {} pending entities to process from source '{}'", allPendingSourceToArmEntities.size(), getEodSourceLocation().getDescription());

        if (!allPendingSourceToArmEntities.isEmpty()) {

            userAccount = userIdentity.getUserAccount();
            File archiveRecordsFile = createEmptyArchiveRecordsFile();
            var batchItems = new BatchItems();

            for (var currentEod : allPendingSourceToArmEntities) {

                var batchItem = new BatchItem();

                try {
                    ExternalObjectDirectoryEntity armEod;
                    if (isEqual(currentEod.getExternalLocationType(), EodHelper.armLocation())) {
                        armEod = updateArmEodToArmIngestionStatus(currentEod, batchItem, batchItems, archiveRecordsFile);
                        if (armEod == null) {
                            continue;
                        }
                    } else {
                        armEod = createArmEodWithArmIngestionStatus(currentEod, batchItem, batchItems, archiveRecordsFile);
                    }

                    String rawFilename = generateRawFilename(armEod);

                    if (shouldPushRawDataToArm(batchItem)) {
                        pushRawDataAndCreateArchiveRecordIfSuccess(batchItem, rawFilename);
                    } else if (shouldAddEntryToManifestFile(batchItem)) {
                        batchItem.setArchiveRecord(archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename));
                    }
                } catch (Exception e) {
                    log.error("Unable to batch push EOD {} to ARM", currentEod.getId(), e);
                    recoverByUpdatingEodToFailedArmStatus(batchItem);
                }
            }

            try {
                if (!batchItems.getSuccessful().isEmpty()) {
                    writeManifestFile(batchItems, archiveRecordsFile);
                    copyMetadataToArm(archiveRecordsFile);
                }
            } catch (Exception e) {
                log.error("Error during generation of batch manifest file {}", archiveRecordsFile.getName(), e);
                batchItems.getSuccessful().forEach(this::recoverByUpdatingEodToFailedArmStatus);
                return;
            }

            for (var batchItem : batchItems.getSuccessful()) {
                updateExternalObjectDirectoryStatus(batchItem.getArmEod(), EodHelper.armDropZoneStatus());
            }
        }

        log.info("Finished running ARM Batch Push processing at: {}", OffsetDateTime.now());
    }

    private ExternalLocationTypeEntity getEodSourceLocation() {
        var armClient = armDataManagementConfiguration.getArmClient();
        if (armClient.equalsIgnoreCase("darts")) {
            return EodHelper.unstructuredLocation();
        } else if (armClient.equalsIgnoreCase("dets")) {
            return EodHelper.detsLocation();
        } else {
            throw new DartsException(String.format("Invalid arm client '%s'", armClient));
        }
    }

    private List<ExternalObjectDirectoryEntity> getArmExternalObjectDirectoryEntities(int batchSize) {

        ExternalLocationTypeEntity sourceLocation = getEodSourceLocation();

        var result = new ArrayList<ExternalObjectDirectoryEntity>();
        result.addAll(eodService.findFailedStillRetriableArmEods(Pageable.ofSize(batchSize)));
        var remaining = batchSize - result.size();
        if (remaining > 0) {
            result.addAll(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
                EodHelper.storedStatus(),
                sourceLocation,
                EodHelper.armLocation(),
                Pageable.ofSize(remaining)));
        }
        return result;
    }

    private ExternalObjectDirectoryEntity updateArmEodToArmIngestionStatus(ExternalObjectDirectoryEntity armEod, BatchItem batchItem, BatchItems batchItems,
                                                                           File archiveRecordsFile) {

        var matchingEntity = getExternalObjectDirectoryEntity(armEod, getEodSourceLocation(), EodHelper.storedStatus());
        if (matchingEntity.isPresent()) {
            batchItem.setArmEod(armEod);
            batchItem.setSourceEod(matchingEntity.get());
            batchItems.add(batchItem);
            armEod.setManifestFile(archiveRecordsFile.getName());
            updateExternalObjectDirectoryStatus(armEod, EodHelper.armIngestionStatus());
        } else {
            log.error("Unable to find matching external object directory for {}", armEod.getId());
            updateExternalObjectDirectoryFailedTransferAttempts(armEod);
            return null;
        }
        return armEod;
    }

    private ExternalObjectDirectoryEntity createArmEodWithArmIngestionStatus(ExternalObjectDirectoryEntity currentEod, BatchItem batchItem,
                                                                             BatchItems batchItems,
                                                                             File archiveRecordsFile) {
        ExternalObjectDirectoryEntity armEod;
        armEod = createArmExternalObjectDirectoryEntity(currentEod, EodHelper.armIngestionStatus());
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
        return equalsAnyStatus(batchItem.getPreviousStatus(), EodHelper.armIngestionStatus(), EodHelper.failedArmRawDataStatus());
    }

    private void pushRawDataAndCreateArchiveRecordIfSuccess(BatchItem batchItem, String rawFilename) {
        log.info("Start of batch ARM Push processing for EOD {} running at: {}", batchItem.getArmEod().getId(), OffsetDateTime.now());
        boolean copyRawDataToArmSuccessful = copyRawDataToArm(
            batchItem.getSourceEod(),
            batchItem.getArmEod(),
            rawFilename,
            batchItem.getPreviousStatus(),
            () -> {
                batchItem.setRawFilePushSuccessful(false);
                batchItem.undoManifestFileChange();
                updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmRawDataStatus());
            }
        );
        if (copyRawDataToArmSuccessful) {
            batchItem.setRawFilePushSuccessful(true);
            var archiveRecord = archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename);
            batchItem.setArchiveRecord(archiveRecord);
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

    private void recoverByUpdatingEodToFailedArmStatus(BatchItem batchItem) {
        if (batchItem.getArmEod() != null) {
            batchItem.undoManifestFileChange();
            if (!batchItem.isRawFilePushNotNeededOrSuccessfulWhenNeeded()) {
                updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmRawDataStatus());
            } else {
                updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodHelper.failedArmManifestFileStatus());
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
