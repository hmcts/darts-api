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
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodEntities;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.File;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.util.EodEntities.equalsAnyStatus;
import static uk.gov.hmcts.darts.common.util.EodEntities.isEqual;


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

        List<ExternalObjectDirectoryEntity> allPendingUnstructuredToArmEntities = getArmExternalObjectDirectoryEntities(
            armDataManagementConfiguration.getBatchSize()
        );

        if (!allPendingUnstructuredToArmEntities.isEmpty()) {

            userAccount = userIdentity.getUserAccount();
            File archiveRecordsFile = createEmptyArchiveRecordsFile();

            var batchItems = new BatchItems();

            for (var currentEod : allPendingUnstructuredToArmEntities) {
                BatchItem batchItem = new BatchItem();
                try {
                    ExternalObjectDirectoryEntity armEod;
                    if (isEqual(currentEod.getExternalLocationType(), EodEntities.armLocation())) {
                        armEod = currentEod;

                        var matchingEntity = getExternalObjectDirectoryEntity(armEod, getEodSourceLocation(), EodEntities.storedStatus());
                        if (matchingEntity.isPresent()) {
                            batchItem.setArmEod(armEod);
                            batchItem.setUnstructuredEod(matchingEntity.get());
                            batchItems.add(batchItem);
                            armEod.setManifestFile(archiveRecordsFile.getName());
                            updateExternalObjectDirectoryStatus(armEod, EodEntities.armIngestionStatus());
                        } else {
                            log.error("Unable to find matching external object directory for {}", armEod.getId());
                            updateExternalObjectDirectoryFailedTransferAttempts(armEod);
                            continue;
                        }
                    } else {
                        armEod = createArmExternalObjectDirectoryEntity(currentEod, EodEntities.armIngestionStatus());
                        batchItem.setArmEod(armEod);
                        batchItem.setUnstructuredEod(currentEod);
                        batchItems.add(batchItem);
                        armEod.setManifestFile(archiveRecordsFile.getName());
                        externalObjectDirectoryRepository.saveAndFlush(armEod);
                    }

                    String rawFilename = generateFilename(armEod);
                    if (shouldPushRawDataToArm(batchItem)) {
                        pushRawDataAndCreateArchiveRecordIfSuccess(batchItem, rawFilename);
                    } else if (shouldAddEntryToManifestFile(batchItem)) {
                        var archiveRecord = archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename);
                        //TODO check why archive record is missing some data
                        batchItem.setArchiveRecord(archiveRecord);
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
                //TODO set the manifest file here rather than before and then having to undo it?
                updateExternalObjectDirectoryStatus(batchItem.getArmEod(), EodEntities.armDropZoneStatus());
            }

            log.info("Finished running ARM Batch Push processing running at: {}", OffsetDateTime.now());
        }
    }

    private ExternalLocationTypeEntity getEodSourceLocation() {
        var armClient = armDataManagementConfiguration.getArmClient();
        if (armClient.equalsIgnoreCase("darts")) {
            return EodEntities.unstructuredLocation();
        } else if (armClient.equalsIgnoreCase("dets")) {
            return EodEntities.detsLocation();
        } else {
            throw new RuntimeException(String.format("Invalid arm client '%s'", armClient));
        }
    }

    private List<ExternalObjectDirectoryEntity> getArmExternalObjectDirectoryEntities(int batchSize) {

        ExternalLocationTypeEntity sourceLocation = getEodSourceLocation();

        var result = new ArrayList<ExternalObjectDirectoryEntity>();
        result.addAll(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            EodEntities.storedStatus(),
            sourceLocation,
            EodEntities.armLocation(),
            Pageable.ofSize(batchSize)
        ));
        var remaining = batchSize - result.size();
        if (remaining > 0) {
            result.addAll(eodService.findFailedStillRetriableArmEODs(Pageable.ofSize(remaining)));
        }
        return result;
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
        return filePath.toFile();
    }

    private boolean shouldPushRawDataToArm(BatchItem batchItem) {
        return equalsAnyStatus(batchItem.getPreviousStatus(), EodEntities.armIngestionStatus(), EodEntities.failedArmRawDataStatus());
    }

    private void pushRawDataAndCreateArchiveRecordIfSuccess(BatchItem batchItem, String rawFilename) {
        log.info("Start of batch ARM Push processing for EOD {} running at: {}", batchItem.getArmEod().getId(), OffsetDateTime.now());
        boolean copyRawDataToArmSuccessful = copyRawDataToArm(
            batchItem.getUnstructuredEod(),
            batchItem.getArmEod(),
            rawFilename,
            batchItem.getPreviousStatus(),
            () -> {
                batchItem.setRawFilePushSuccessful(false);
                batchItem.undoManifestFileChange();
                updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodEntities.failedArmRawDataStatus());
            }
        );
        if (copyRawDataToArmSuccessful) {
            batchItem.setRawFilePushSuccessful(true);
            var archiveRecord = archiveRecordService.generateArchiveRecordInfo(batchItem.getArmEod().getId(), rawFilename);
            batchItem.setArchiveRecord(archiveRecord);
        }
    }

    private boolean shouldAddEntryToManifestFile(BatchItem batchItem) {
        return equalsAnyStatus(batchItem.getPreviousStatus(), EodEntities.failedArmManifestFileStatus(), EodEntities.failedArmResponseManifestFileStatus());
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
            //TODO ask Hemanta: what would be the status if the code fails before the push is even attempted? should not be failedArmRawDataStatus
            if (!batchItem.isRawFilePushSuccessfulWhenAttempted()) {
                updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodEntities.failedArmRawDataStatus());
            } else {
                updateExternalObjectDirectoryStatusToFailed(batchItem.getArmEod(), EodEntities.failedArmManifestFileStatus());
            }
        }
    }

    /**
     * Contains info related to the processing of a batch item
     */
    @Data
    static class BatchItem {

        private ExternalObjectDirectoryEntity unstructuredEod;
        private ExternalObjectDirectoryEntity armEod;
        private String previousManifestFile;
        private ObjectRecordStatusEntity previousStatus;
        private Boolean rawFilePushSuccessful;
        private ArchiveRecord archiveRecord;

        public BatchItem() {}

        public void setArmEod(ExternalObjectDirectoryEntity armEod) {
            this.armEod = armEod;
            this.previousManifestFile = armEod.getManifestFile();
            this.previousStatus = armEod.getStatus();
        }

        public void undoManifestFileChange() {
            this.armEod.setManifestFile(this.previousManifestFile);
        }

        public boolean isRawFilePushSuccessfulWhenAttempted() {
            return rawFilePushSuccessful == null || rawFilePushSuccessful;
        }
    }

    static class BatchItems implements Iterable<BatchItem> {

        private final List<BatchItem> batchItems = new ArrayList<>();

        public void add(BatchItem batchItem) {
            batchItems.add(batchItem);
        }
        @Override
        public Iterator<BatchItem> iterator() {
            return batchItems.iterator();
        }

        public List<BatchItem> getSuccessful() {
            return batchItems.stream().filter(batchItem -> batchItem.isRawFilePushSuccessfulWhenAttempted() && batchItem.getArchiveRecord() != null).toList();
        }

        public List<ArchiveRecord> getArchiveRecords() {
            return getSuccessful().stream().map(BatchItem::getArchiveRecord).toList();
        }
    }

}
