package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Slf4j
public class UnstructuredToArmProcessorImpl extends AbstractUnstructuredToArmProcessor {

    private final ArchiveRecordService archiveRecordService;

    private ObjectRecordStatusEntity storedStatus;
    private ObjectRecordStatusEntity failedArmRawDataStatus;
    private ObjectRecordStatusEntity failedArmManifestFileStatus;
    private ObjectRecordStatusEntity armIngestionStatus;
    private ObjectRecordStatusEntity armDropZoneStatus;


    public UnstructuredToArmProcessorImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                          ObjectRecordStatusRepository objectRecordStatusRepository,
                                          ExternalLocationTypeRepository externalLocationTypeRepository,
                                          DataManagementApi dataManagementApi,
                                          ArmDataManagementApi armDataManagementApi,
                                          UserIdentity userIdentity,
                                          ArmDataManagementConfiguration armDataManagementConfiguration,
                                          FileOperationService fileOperationService,
                                          ArchiveRecordService archiveRecordService,
                                          Integer batchSize,
                                          LogApi logApi) {
        super(objectRecordStatusRepository, userIdentity, externalObjectDirectoryRepository, externalLocationTypeRepository, dataManagementApi,
              armDataManagementApi, fileOperationService, batchSize, logApi, armDataManagementConfiguration);
        this.archiveRecordService = archiveRecordService;
    }

    @Override
    public void processUnstructuredToArm() {
        preloadObjectRecordStatuses();

        userAccount = userIdentity.getUserAccount();

        ExternalLocationTypeEntity inboundLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());

        List<ExternalObjectDirectoryEntity> allPendingUnstructuredToArmEntities = getArmExternalObjectDirectoryEntities(inboundLocation, armLocation);

        for (var currentExternalObjectDirectory : allPendingUnstructuredToArmEntities) {
            try {
                ObjectRecordStatusEntity previousStatus = null;
                ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory;
                ExternalObjectDirectoryEntity armExternalObjectDirectory;

                if (currentExternalObjectDirectory.getExternalLocationType().getId().equals(armLocation.getId())) {
                    armExternalObjectDirectory = currentExternalObjectDirectory;
                    previousStatus = armExternalObjectDirectory.getStatus();
                    var matchingEntity = getExternalObjectDirectoryEntity(
                        armExternalObjectDirectory,
                        externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId()),
                        storedStatus);
                    if (matchingEntity.isPresent()) {
                        unstructuredExternalObjectDirectory = matchingEntity.get();
                    } else {
                        log.error("Unable to find matching external object directory for {}", armExternalObjectDirectory.getId());
                        updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, failedArmRawDataStatus);
                        continue;
                    }
                } else {
                    unstructuredExternalObjectDirectory = currentExternalObjectDirectory;
                    armExternalObjectDirectory = createArmExternalObjectDirectoryEntity(currentExternalObjectDirectory, armIngestionStatus);
                    updateExternalObjectDirectoryStatus(armExternalObjectDirectory, armIngestionStatus);
                }


                String rawFilename = generateRawFilename(armExternalObjectDirectory);
                log.info("Start of ARM Push processing for EOD {} running at: {}", armExternalObjectDirectory.getId(), OffsetDateTime.now());
                boolean copyRawDataToArmSuccessful = copyRawDataToArm(
                    unstructuredExternalObjectDirectory,
                    armExternalObjectDirectory,
                    rawFilename,
                    previousStatus,
                    () -> updateExternalObjectDirectoryStatusToFailed(
                        armExternalObjectDirectory,
                        objectRecordStatusRepository.findById(ARM_RAW_DATA_FAILED.getId()).get()
                    )
                );
                if (copyRawDataToArmSuccessful && generateAndCopyMetadataToArm(armExternalObjectDirectory, rawFilename)) {
                    updateExternalObjectDirectoryStatus(armExternalObjectDirectory, armDropZoneStatus);
                    logApi.armPushSuccessful(armExternalObjectDirectory.getId());
                }
                log.info("Finished running ARM Push processing for EOD {} running at: {}", armExternalObjectDirectory.getId(), OffsetDateTime.now());
            } catch (Exception e) {
                log.error("Unable to push EOD {} to ARM", currentExternalObjectDirectory.getId(), e);
            }

        }
    }

    @SuppressWarnings("java:S3655")
    private void preloadObjectRecordStatuses() {
        storedStatus = objectRecordStatusRepository.findById(STORED.getId()).get();
        failedArmRawDataStatus = objectRecordStatusRepository.findById(ARM_RAW_DATA_FAILED.getId()).get();
        failedArmManifestFileStatus = objectRecordStatusRepository.findById(ARM_MANIFEST_FAILED.getId()).get();
        armIngestionStatus = objectRecordStatusRepository.findById(ARM_INGESTION.getId()).get();
        armDropZoneStatus = objectRecordStatusRepository.findById(ARM_DROP_ZONE.getId()).get();

    }

    private boolean generateAndCopyMetadataToArm(ExternalObjectDirectoryEntity armExternalObjectDirectory, String rawFilename) {
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armExternalObjectDirectory.getId(), rawFilename);

        File archiveRecordFile = archiveRecordFileInfo.getArchiveRecordFile();
        if (archiveRecordFileInfo.isFileGenerationSuccessful() && archiveRecordFile.exists()) {
            try {
                BinaryData metadataFileBinary = fileOperationService.convertFileToBinaryData(archiveRecordFile.getAbsolutePath());
                armDataManagementApi.saveBlobDataToArm(archiveRecordFileInfo.getArchiveRecordFile().getName(), metadataFileBinary);
            } catch (BlobStorageException e) {
                if (e.getStatusCode() == BLOB_ALREADY_EXISTS_STATUS_CODE) {
                    log.info("Metadata BLOB already exists {}", e.getMessage());
                } else {
                    log.error("Failed to move BLOB metadata for file {} due to {}", archiveRecordFile.getAbsolutePath(), e.getMessage());
                    updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, failedArmManifestFileStatus);
                    return false;
                }
            } catch (Exception e) {
                log.error("Unable to move BLOB metadata for file {} due to {}", archiveRecordFile.getAbsolutePath(), e.getMessage());
                updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, failedArmManifestFileStatus);
                return false;
            }
        } else {
            log.error("Failed to generate metadata file {}", archiveRecordFile.getAbsolutePath());
            updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, failedArmManifestFileStatus);
            return false;
        }
        return true;
    }

    private List<ExternalObjectDirectoryEntity> getArmExternalObjectDirectoryEntities(ExternalLocationTypeEntity inboundLocation,
                                                                                      ExternalLocationTypeEntity armLocation) {

        List<ObjectRecordStatusEntity> failedArmStatuses = List.of(failedArmRawDataStatus, failedArmManifestFileStatus);

        var pendingUnstructuredExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            storedStatus,
            inboundLocation,
            armLocation
        );

        var failedArmExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(
            failedArmStatuses,
            armLocation,
            armDataManagementConfiguration.getMaxRetryAttempts()
        );

        return Stream.concat(pendingUnstructuredExternalObjectDirectoryEntities.stream(), failedArmExternalObjectDirectoryEntities.stream()).toList();
    }

}
