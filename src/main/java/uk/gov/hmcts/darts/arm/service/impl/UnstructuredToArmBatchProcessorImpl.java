package uk.gov.hmcts.darts.arm.service.impl;

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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.util.EodEntities.equalsAny;
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
            armDataManagementConfiguration.getArmClient(), armDataManagementConfiguration.getBatchSize()
        );

        if (!allPendingUnstructuredToArmEntities.isEmpty()) {

            userAccount = userIdentity.getUserAccount();
            File archiveRecordsFile = createEmptyArchiveRecordsFile();
            var batchEntities = new BatchEntities();

            for (var currentEod : allPendingUnstructuredToArmEntities) {
                BatchEntity batchEntity = new BatchEntity();
                try {
                    ExternalObjectDirectoryEntity unstructuredEod;
                    ExternalObjectDirectoryEntity armEod;
                    if (isEqual(currentEod.getExternalLocationType(), EodEntities.armLocation())) {
                        armEod = currentEod;
                        var matchingEntity = getUnstructuredExternalObjectDirectoryEntity(armEod, EodEntities.storedStatus());
                        if (matchingEntity.isPresent()) {
                            batchEntity.setArmEod(armEod);
                            batchEntities.add(batchEntity);
                            unstructuredEod = matchingEntity.get();
                            armEod.setManifestFile(archiveRecordsFile.getName());
                            updateExternalObjectDirectoryStatus(armEod, EodEntities.armIngestionStatus());
                        } else {
                            //TODO ask Hemanta if it's ok to keep current failed status here
                            log.error("Unable to find matching external object directory for {}", armEod.getId());
                            updateExternalObjectDirectoryFailedTransferAttempts(armEod);
                            //TODO this might not work, need to use the result of the previous method? Also, this has failed. Verify manifest is not generated
                            continue;
                        }
                    } else {
                        unstructuredEod = currentEod;
                        armEod = createArmExternalObjectDirectoryEntity(currentEod, EodEntities.armIngestionStatus());
                        batchEntity.setArmEod(armEod);
                        batchEntities.add(batchEntity);
                        armEod.setManifestFile(archiveRecordsFile.getName());
                        externalObjectDirectoryRepository.saveAndFlush(armEod);
                    }

                    String rawFilename = generateFilename(armEod);
                    if (equalsAny(batchEntity.getPreviousStatus(), EodEntities.armIngestionStatus(), EodEntities.failedArmRawDataStatus())) {
                        log.info("Start of batch ARM Push processing for EOD {} running at: {}", armEod.getId(), OffsetDateTime.now());
                        boolean copyRawDataToArmSuccessful = copyRawDataToArm(
                            unstructuredEod,
                            armEod,
                            rawFilename,
                            batchEntity.getPreviousStatus(),
                            () -> updateExternalObjectDirectoryStatusToFailed(batchEntity.getArmEod(), EodEntities.failedArmRawDataStatus())
                        );
                        if (copyRawDataToArmSuccessful) {
                            batchEntity.setRawFilePushSuccessful(true);
                            var archiveRecord = archiveRecordService.generateArchiveRecord(batchEntity.getArmEod().getId(), rawFilename);
                            batchEntity.setArchiveRecord(archiveRecord);
                        }
                    } else if (equalsAny(batchEntity.getPreviousStatus(), EodEntities.failedArmManifestFileStatus(), EodEntities.failedArmResponseManifestFileStatus())) {
                        var archiveRecord = archiveRecordService.generateArchiveRecord(batchEntity.getArmEod().getId(), rawFilename);
                        batchEntity.setArchiveRecord(archiveRecord);
                    }
                } catch (Exception e) {
                    log.error("Unable to batch push EOD {} to ARM", currentEod.getId(), e);
                    if (batchEntity.getArmEod() != null) {
                        batchEntity.undoManifestFileChange();
                        //FIXME what would be the status if the code fails before the push is even attempted? should not be failedArmRawDataStatus
                        if (!Boolean.TRUE.equals(batchEntity.getRawFilePushSuccessful())) {
                            updateExternalObjectDirectoryStatusToFailed(batchEntity.getArmEod(), EodEntities.failedArmRawDataStatus());
                        } else {
                            updateExternalObjectDirectoryStatusToFailed(batchEntity.getArmEod(), EodEntities.failedArmManifestFileStatus());
                        }
                    }
                }
            }

            //TODO add try catch this can fail too
            archiveRecordFileGenerator.generateArchiveRecords(batchEntities.getArchiveRecords(), archiveRecordsFile);

            copyMetadataToArm(
                archiveRecordsFile,
                () -> {
                    for (var batchEntity : batchEntities.getSuccessful()) {
                        batchEntity.undoManifestFileChange();
                        updateExternalObjectDirectoryStatusToFailed(batchEntity.getArmEod(), EodEntities.failedArmManifestFileStatus());
                    }
                }
            );

            for (var batchEntity : batchEntities.getSuccessful()) {
                //TODO handle potential error
                updateExternalObjectDirectoryStatus(batchEntity.getArmEod(), EodEntities.armDropZoneStatus());
            }

            log.info("Finished running ARM Batch Push processing running at: {}", OffsetDateTime.now());
        }
    }

    private List<ExternalObjectDirectoryEntity> getArmExternalObjectDirectoryEntities(String armClient, int batchSize) {

        ExternalLocationTypeEntity sourceLocation;
        if (armClient.equalsIgnoreCase("darts")) {
             sourceLocation = EodEntities.unstructuredLocation();
        } else if (armClient.equalsIgnoreCase("dets")) {
            sourceLocation = EodEntities.detsLocation();
        } else {
            log.error("unknown arm client '{}'", armDataManagementConfiguration.getArmClient());
            return Collections.emptyList();
        }

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

    @Data
    static class BatchEntity {

        private ExternalObjectDirectoryEntity armEod;
        private String previousManifestFile;
        private ObjectRecordStatusEntity previousStatus;
        private Boolean rawFilePushSuccessful;
        private ArchiveRecord archiveRecord;

        public BatchEntity() {}

        public void setArmEod(ExternalObjectDirectoryEntity armEod) {
            this.armEod = armEod;
            this.previousManifestFile = armEod.getManifestFile();
            this.previousStatus = armEod.getStatus();
        }

        public void undoManifestFileChange() {
            this.armEod.setManifestFile(this.previousManifestFile);
        }

        public boolean isRawFilePushAttemptedAndSuccessful() {
            return rawFilePushSuccessful == null || rawFilePushSuccessful;
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
            return batchEntities.stream().filter(batchEntity -> batchEntity.isRawFilePushAttemptedAndSuccessful() && batchEntity.getArchiveRecord() != null).toList();
        }

        public List<ArchiveRecord> getArchiveRecords() {
            return getSuccessful().stream().map(BatchEntity::getArchiveRecord).toList();
        }
    }

}
