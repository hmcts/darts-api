package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_MANIFEST_FILE_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@Slf4j
public class UnstructuredToArmProcessorImpl implements UnstructuredToArmProcessor {

    private static final int BLOB_ALREADY_EXISTS_STATUS_CODE = 409;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final DataManagementApi dataManagementApi;
    private final ArmDataManagementApi armDataManagementApi;
    private final UserIdentity userIdentity;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    private final FileOperationService fileOperationService;
    private final ArchiveRecordService archiveRecordService;

    private ObjectRecordStatusEntity storedStatus;
    private ObjectRecordStatusEntity failedArmRawDataStatus;
    private ObjectRecordStatusEntity failedArmManifestFileStatus;
    private ObjectRecordStatusEntity armIngestionStatus;
    private ObjectRecordStatusEntity armDropZoneStatus;
    private UserAccountEntity userAccount;


    public UnstructuredToArmProcessorImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                          ObjectRecordStatusRepository objectRecordStatusRepository,
                                          ExternalLocationTypeRepository externalLocationTypeRepository, DataManagementApi dataManagementApi,
                                          ArmDataManagementApi armDataManagementApi, UserIdentity userIdentity,
                                          ArmDataManagementConfiguration armDataManagementConfiguration, FileOperationService fileOperationService,
                                          ArchiveRecordService archiveRecordService) {
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.objectRecordStatusRepository = objectRecordStatusRepository;
        this.externalLocationTypeRepository = externalLocationTypeRepository;
        this.dataManagementApi = dataManagementApi;
        this.armDataManagementApi = armDataManagementApi;
        this.userIdentity = userIdentity;
        this.armDataManagementConfiguration = armDataManagementConfiguration;
        this.fileOperationService = fileOperationService;
        this.archiveRecordService = archiveRecordService;
    }

    @Override
    @Transactional
    public void processUnstructuredToArm() {
        preloadObjectRecordStatuses();

        userAccount = userIdentity.getUserAccount();

        ExternalLocationTypeEntity inboundLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());

        List<ExternalObjectDirectoryEntity> allPendingUnstructuredToArmEntities = getArmExternalObjectDirectoryEntities(inboundLocation, armLocation);

        for (var currentExternalObjectDirectory : allPendingUnstructuredToArmEntities) {

            ObjectRecordStatusEntity previousStatus = null;
            ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory;
            ExternalObjectDirectoryEntity armExternalObjectDirectory;

            if (currentExternalObjectDirectory.getExternalLocationType().getId().equals(armLocation.getId())) {
                armExternalObjectDirectory = currentExternalObjectDirectory;
                previousStatus = armExternalObjectDirectory.getStatus();
                var matchingEntity = getUnstructuredExternalObjectDirectoryEntity(armExternalObjectDirectory);
                if (matchingEntity.isPresent()) {
                    unstructuredExternalObjectDirectory = matchingEntity.get();
                } else {
                    log.error("Unable to find matching external object directory for {}", armExternalObjectDirectory.getId());
                    updateTransferAttempts(armExternalObjectDirectory);
                    updateExternalObjectDirectoryStatus(armExternalObjectDirectory, failedArmRawDataStatus);
                    continue;
                }
            } else {
                unstructuredExternalObjectDirectory = currentExternalObjectDirectory;
                armExternalObjectDirectory = createArmExternalObjectDirectoryEntity(currentExternalObjectDirectory);
            }

            updateExternalObjectDirectoryStatus(armExternalObjectDirectory, armIngestionStatus);

            String filename = generateFilename(armExternalObjectDirectory);

            boolean copyRawDataToArmSuccessful = copyRawDataToArm(
                    unstructuredExternalObjectDirectory,
                    armExternalObjectDirectory,
                    filename,
                    previousStatus
            );
            if (copyRawDataToArmSuccessful && generateAndCopyMetadataToArm(armExternalObjectDirectory)) {
                updateExternalObjectDirectoryStatus(armExternalObjectDirectory, armDropZoneStatus);
            }
        }
    }

    private void preloadObjectRecordStatuses() {
        storedStatus = objectRecordStatusRepository.findById(STORED.getId()).get();
        failedArmRawDataStatus = objectRecordStatusRepository.findById(FAILURE_ARM_RAW_DATA_FAILED.getId()).get();
        failedArmManifestFileStatus = objectRecordStatusRepository.findById(FAILURE_ARM_MANIFEST_FILE_FAILED.getId()).get();
        armIngestionStatus = objectRecordStatusRepository.findById(ARM_INGESTION.getId()).get();
        armDropZoneStatus = objectRecordStatusRepository.findById(ARM_DROP_ZONE.getId()).get();

    }

    private void updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity armExternalObjectDirectory, ObjectRecordStatusEntity armStatus) {
        log.debug(
                "Updating ARM status from {} to {} for ID {}",
                armExternalObjectDirectory.getStatus().getDescription(),
                armStatus.getDescription(),
                armExternalObjectDirectory.getId()
        );
        armExternalObjectDirectory.setStatus(armStatus);
        armExternalObjectDirectory.setLastModifiedBy(userAccount);
        armExternalObjectDirectory.setLastModifiedDateTime(OffsetDateTime.now());
        externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
    }

    private boolean generateAndCopyMetadataToArm(ExternalObjectDirectoryEntity armExternalObjectDirectory) {
        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(armExternalObjectDirectory.getId());

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

        List<ObjectRecordStatusEntity> failedArmStatuses = new ArrayList<>();
        failedArmStatuses.add(failedArmRawDataStatus);
        failedArmStatuses.add(failedArmManifestFileStatus);

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

    private boolean copyRawDataToArm(ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory,
                                     ExternalObjectDirectoryEntity armExternalObjectDirectory,
                                     String filename,
                                     ObjectRecordStatusEntity previousStatus) {
        try {
            if (previousStatus == null
                    || FAILURE_ARM_RAW_DATA_FAILED.getId().equals(previousStatus.getId())
                    || ARM_INGESTION.getId().equals(previousStatus.getId())) {
                BinaryData inboundFile = dataManagementApi.getBlobDataFromUnstructuredContainer(
                        unstructuredExternalObjectDirectory.getExternalLocation());
                log.info("About to push raw data to ARM for EOD {}", armExternalObjectDirectory.getId());
                armDataManagementApi.saveBlobDataToArm(filename, inboundFile);
                log.info("Pushed raw data to ARM for EOD {}", armExternalObjectDirectory.getId());
                armExternalObjectDirectory.setChecksum(unstructuredExternalObjectDirectory.getChecksum());
                armExternalObjectDirectory.setExternalLocation(UUID.randomUUID());
                armExternalObjectDirectory.setLastModifiedBy(userIdentity.getUserAccount());
                externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
            }
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == BLOB_ALREADY_EXISTS_STATUS_CODE) {
                log.info("BLOB raw data already exists {}", e.getMessage());
            } else {
                log.error("Failed to move BLOB data for file {} due to {}", unstructuredExternalObjectDirectory.getExternalLocation(),
                          e.getMessage()
                );
                updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, failedArmRawDataStatus);
                return false;
            }
        } catch (Exception e) {
            log.error(
                    "Error moving BLOB data for file {} due to {}",
                    unstructuredExternalObjectDirectory.getExternalLocation(),
                    e.getMessage()
            );

            updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, failedArmRawDataStatus);
            return false;
        }

        return true;
    }

    private void updateExternalObjectDirectoryStatusToFailed(ExternalObjectDirectoryEntity armExternalObjectDirectory,
                                                             ObjectRecordStatusEntity objectRecordStatus) {
        log.debug(
                "Updating ARM status from {} to {} for ID ",
                armExternalObjectDirectory.getStatus().getDescription(),
                objectRecordStatus.getDescription(),
                armExternalObjectDirectory.getId()
        );
        armExternalObjectDirectory.setStatus(objectRecordStatus);
        updateTransferAttempts(armExternalObjectDirectory);
        armExternalObjectDirectory.setLastModifiedBy(userIdentity.getUserAccount());
        armExternalObjectDirectory.setLastModifiedDateTime(OffsetDateTime.now());
        externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
    }

    private Optional<ExternalObjectDirectoryEntity> getUnstructuredExternalObjectDirectoryEntity(
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        return externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
                storedStatus,
                externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId()),
                externalObjectDirectoryEntity.getMedia(),
                externalObjectDirectoryEntity.getTranscriptionDocumentEntity(),
                externalObjectDirectoryEntity.getAnnotationDocumentEntity(),
                externalObjectDirectoryEntity.getCaseDocument()
        );
    }

    private ExternalObjectDirectoryEntity createArmExternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectory) {

        ExternalObjectDirectoryEntity armExternalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        armExternalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(ARM.getId()));
        armExternalObjectDirectoryEntity.setStatus(armIngestionStatus);
        armExternalObjectDirectoryEntity.setExternalLocation(externalObjectDirectory.getExternalLocation());
        armExternalObjectDirectoryEntity.setVerificationAttempts(1);


        if (nonNull(externalObjectDirectory.getMedia())) {
            armExternalObjectDirectoryEntity.setMedia(externalObjectDirectory.getMedia());
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            armExternalObjectDirectoryEntity.setTranscriptionDocumentEntity(externalObjectDirectory.getTranscriptionDocumentEntity());
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            armExternalObjectDirectoryEntity.setAnnotationDocumentEntity(externalObjectDirectory.getAnnotationDocumentEntity());
        }
        OffsetDateTime now = OffsetDateTime.now();
        armExternalObjectDirectoryEntity.setCreatedDateTime(now);
        armExternalObjectDirectoryEntity.setLastModifiedDateTime(now);
        var systemUser = userIdentity.getUserAccount();
        armExternalObjectDirectoryEntity.setCreatedBy(systemUser);
        armExternalObjectDirectoryEntity.setLastModifiedBy(systemUser);
        armExternalObjectDirectoryEntity.setTransferAttempts(1);

        return armExternalObjectDirectoryEntity;
    }


    public String generateFilename(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        final Integer entityId = externalObjectDirectoryEntity.getId();
        final Integer transferAttempts = externalObjectDirectoryEntity.getTransferAttempts();

        Integer documentId = 0;
        if (nonNull(externalObjectDirectoryEntity.getMedia())) {
            documentId = externalObjectDirectoryEntity.getMedia().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getTranscriptionDocumentEntity())) {
            documentId = externalObjectDirectoryEntity.getTranscriptionDocumentEntity().getId();
        } else if (nonNull(externalObjectDirectoryEntity.getAnnotationDocumentEntity())) {
            documentId = externalObjectDirectoryEntity.getAnnotationDocumentEntity().getId();
        }

        return String.format("%s_%s_%s", entityId, documentId, transferAttempts);
    }

    private void updateTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        int currentNumberOfAttempts = externalObjectDirectoryEntity.getTransferAttempts();
        externalObjectDirectoryEntity.setTransferAttempts(currentNumberOfAttempts + 1);
    }
}
