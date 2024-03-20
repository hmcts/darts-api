package uk.gov.hmcts.darts.arm.service.impl;

import lombok.Data;
import lombok.RequiredArgsConstructor;
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
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.File;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.darts.arm.service.impl.EodEntities.armIngestionStatus;
import static uk.gov.hmcts.darts.arm.service.impl.EodEntities.armLocation;
import static uk.gov.hmcts.darts.arm.service.impl.EodEntities.detsLocation;
import static uk.gov.hmcts.darts.arm.service.impl.EodEntities.equalsAny;
import static uk.gov.hmcts.darts.arm.service.impl.EodEntities.failedArmManifestFileStatus;
import static uk.gov.hmcts.darts.arm.service.impl.EodEntities.failedArmRawDataStatus;
import static uk.gov.hmcts.darts.arm.service.impl.EodEntities.failedArmResponseManifestFileStatus;
import static uk.gov.hmcts.darts.arm.service.impl.EodEntities.storedStatus;
import static uk.gov.hmcts.darts.arm.service.impl.EodEntities.unstructuredLocation;

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
            armDataManagementConfiguration.getArmClient(), armDataManagementConfiguration.getBatchSize()
        );

        if (!allPendingUnstructuredToArmEntities.isEmpty()) {

            userAccount = userIdentity.getUserAccount();
            File archiveRecordsFile = createEmptyArchiveRecordsFile();
            var batchEntities = new BatchEntities();

            for (var currentExternalObjectDirectory : allPendingUnstructuredToArmEntities) {
                BatchEntity batchEntity;
                try {
                    ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory;
                    ExternalObjectDirectoryEntity armExternalObjectDirectory;
                    if (currentExternalObjectDirectory.getExternalLocationType().getId().equals(armLocation.getId())) {
                        armExternalObjectDirectory = currentExternalObjectDirectory;
                        var matchingEntity = getUnstructuredExternalObjectDirectoryEntity(armExternalObjectDirectory, storedStatus);
                        if (matchingEntity.isPresent()) {
                            batchEntity = new BatchEntity(armExternalObjectDirectory);
                            batchEntities.add(batchEntity);
                            unstructuredExternalObjectDirectory = matchingEntity.get();
                            armExternalObjectDirectory.setManifestFile(archiveRecordsFile.getName());
                            updateExternalObjectDirectoryStatus(armExternalObjectDirectory, armIngestionStatus);
                        } else {
                            log.error("Unable to find matching external object directory for {}", armExternalObjectDirectory.getId());
                            updateTransferAttempts(armExternalObjectDirectory);
                            updateExternalObjectDirectoryStatus(armExternalObjectDirectory, failedArmRawDataStatus);
                            //TODO this might not work, need to use the result of the previous method
                            batchEntity = new BatchEntity(armExternalObjectDirectory);
                            batchEntities.add(batchEntity);
                            //TODO verify continue stops the current iteration
                            continue;
                        }
                    } else {
                        unstructuredExternalObjectDirectory = currentExternalObjectDirectory;
                        armExternalObjectDirectory = createArmExternalObjectDirectoryEntity(currentExternalObjectDirectory, armIngestionStatus);
                        batchEntity = new BatchEntity(armExternalObjectDirectory);
                        batchEntities.add(batchEntity);
                        armExternalObjectDirectory.setManifestFile(archiveRecordsFile.getName());
                        externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
                    }


                    String rawFilename = generateFilename(armExternalObjectDirectory);
                    if (equalsAny(batchEntity.getPreviousStatus(), armIngestionStatus, failedArmRawDataStatus)) {
                        log.info("Start of ARM Push processing for EOD {} running at: {}", armExternalObjectDirectory.getId(), OffsetDateTime.now());
                        boolean copyRawDataToArmSuccessful = copyRawDataToArm(
                            unstructuredExternalObjectDirectory,
                            armExternalObjectDirectory,
                            rawFilename,
                            batchEntity.getPreviousStatus(),
                            () -> {
                                batchEntity.undoManifestFileChange();
                                updateExternalObjectDirectoryStatusToFailed(batchEntity.getArmEod(), failedArmRawDataStatus);
                            }
                        );
                        if (copyRawDataToArmSuccessful) {
                            var archiveRecord = archiveRecordService.generateArchiveRecord(currentExternalObjectDirectory.getId(), rawFilename);
                            batchEntity.setArchiveRecord(archiveRecord);
                        } else {

                        }
                    } else if (equalsAny(batchEntity.getPreviousStatus(), failedArmManifestFileStatus, failedArmResponseManifestFileStatus)) {
                        var archiveRecord = archiveRecordService.generateArchiveRecord(currentExternalObjectDirectory.getId(), rawFilename);
                        batchEntity.setArchiveRecord(archiveRecord);
                    } else {
                        //TODO
                    }

                } catch (Exception e) {
                    //TODO fix error message
                    log.error("Unable to push EOD {} to ARM", currentExternalObjectDirectory.getId(), e);
                }
            }

            archiveRecordFileGenerator.generateArchiveRecords(batchEntities.getArchiveRecords(), archiveRecordsFile);

            var isMetadataPushSuccessful = copyMetadataToArm(
                archiveRecordsFile,
                () -> {}
            );

            if (isMetadataPushSuccessful) {
                for (var batchEntity : batchEntities.getSuccessful()) {
                    updateExternalObjectDirectoryStatus(batchEntity.getArmEod(), EodEntities.armDropZoneStatus);
                }
            } else {
                for (var batchEntity : batchEntities.getSuccessful()) {
                    batchEntity.undoManifestFileChange();
                    updateExternalObjectDirectoryStatusToFailed(batchEntity.getArmEod(), failedArmManifestFileStatus);
                }
            }
//            log.info("Finished running ARM Push processing for EODs {} running at: {}", armExternalObjectDirectory.getId(), OffsetDateTime.now());
        }
    }

    private List<ExternalObjectDirectoryEntity> getArmExternalObjectDirectoryEntities(String armClient, int batchSize) {

        ExternalLocationTypeEntity sourceLocation = null;
        if (armClient.equalsIgnoreCase("darts")) {
             sourceLocation = unstructuredLocation;
        } else if (armClient.equalsIgnoreCase("dets")) {
            sourceLocation = detsLocation;
        } else {
            log.error("unknown arm client {}", armDataManagementConfiguration.getArmClient());
            return Collections.emptyList();
        }

        var result = new ArrayList<ExternalObjectDirectoryEntity>();
        result.addAll(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            storedStatus,
            sourceLocation,
            armLocation,
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
        Path path = fileOperationService.createFile(fileName, armDataManagementConfiguration.getTempBlobWorkspace(), true);
        return path.toFile();
    }

    @Data
    @RequiredArgsConstructor
    static class BatchEntity {

        private final ExternalObjectDirectoryEntity armEod;
        private final String previousManifestFile;
        private final ObjectRecordStatusEntity previousStatus;
        private ArchiveRecord archiveRecord;
        private boolean isSuccessfullyProcessed;

        public BatchEntity(ExternalObjectDirectoryEntity armEod) {
            this.armEod = armEod;
            this.previousManifestFile = armEod.getManifestFile();
            this.previousStatus = armEod.getStatus();
        }

        public void undoManifestFileChange() {
            this.armEod.setManifestFile(this.previousManifestFile);
        }
    }

    static class BatchEntities implements Iterable<BatchEntity> {

        private final List<BatchEntity> batchEntities = new ArrayList<>();

        public void add(BatchEntity batchEntity) {
            batchEntities.add(batchEntity);
        }
        @Override
        public Iterator<BatchEntity> iterator() {
            return batchEntities.iterator();
        }

        public List<BatchEntity> getSuccessful() {
            return batchEntities.stream().filter(batchEntity -> batchEntity.isSuccessfullyProcessed).toList();
        }

        public List<ArchiveRecord> getArchiveRecords() {
            return getSuccessful().stream().map(BatchEntity::getArchiveRecord).toList();
        }
    }

}
