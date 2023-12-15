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
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_ARM_MANIFEST_FILE_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

@Service
@Slf4j
public class UnstructuredToArmProcessorImpl implements UnstructuredToArmProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final DataManagementApi dataManagementApi;
    private final ArmDataManagementApi armDataManagementApi;
    private final UserAccountRepository userAccountRepository;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    private final FileOperationService fileOperationService;
    private final ArchiveRecordService archiveRecordService;

    private final EnumMap<ObjectDirectoryStatusEnum, ObjectRecordStatusEntity> armStatuses =
        new EnumMap<>(ObjectDirectoryStatusEnum.class);

    public UnstructuredToArmProcessorImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                          ObjectDirectoryStatusRepository objectDirectoryStatusRepository,
                                          ExternalLocationTypeRepository externalLocationTypeRepository, DataManagementApi dataManagementApi,
                                          ArmDataManagementApi armDataManagementApi, UserAccountRepository userAccountRepository,
                                          ArmDataManagementConfiguration armDataManagementConfiguration, FileOperationService fileOperationService,
                                          ArchiveRecordService archiveRecordService) {
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.objectDirectoryStatusRepository = objectDirectoryStatusRepository;
        this.externalLocationTypeRepository = externalLocationTypeRepository;
        this.dataManagementApi = dataManagementApi;
        this.armDataManagementApi = armDataManagementApi;
        this.userAccountRepository = userAccountRepository;
        this.armDataManagementConfiguration = armDataManagementConfiguration;
        this.fileOperationService = fileOperationService;
        this.archiveRecordService = archiveRecordService;


    }

    private void preloadObjectRecordStatuses(ObjectDirectoryStatusRepository objectDirectoryStatusRepository) {
        if (!armStatuses.containsKey(STORED)) {
            ObjectRecordStatusEntity storedStatus = objectDirectoryStatusRepository.getReferenceById(STORED.getId());
            armStatuses.put(STORED, storedStatus);
        }
        if (!armStatuses.containsKey(FAILURE_ARM_RAW_DATA_FAILED)) {
            ObjectRecordStatusEntity failedArmRawDataStatus = objectDirectoryStatusRepository.getReferenceById(FAILURE_ARM_RAW_DATA_FAILED.getId());
            armStatuses.put(FAILURE_ARM_RAW_DATA_FAILED, failedArmRawDataStatus);
        }
        if (!armStatuses.containsKey(FAILURE_ARM_MANIFEST_FILE_FAILED)) {
            ObjectRecordStatusEntity failedArmManifestFileStatus =
                objectDirectoryStatusRepository.getReferenceById(FAILURE_ARM_MANIFEST_FILE_FAILED.getId());
            armStatuses.put(FAILURE_ARM_MANIFEST_FILE_FAILED, failedArmManifestFileStatus);
        }
        if (!armStatuses.containsKey(ARM_INGESTION)) {
            ObjectRecordStatusEntity armIngestionStatus = objectDirectoryStatusRepository.getReferenceById(ARM_INGESTION.getId());
            armStatuses.put(ARM_INGESTION, armIngestionStatus);
        }
        if (!armStatuses.containsKey(ARM_DROP_ZONE)) {
            ObjectRecordStatusEntity armDropZoneStatus = objectDirectoryStatusRepository.getReferenceById(ARM_DROP_ZONE.getId());
            armStatuses.put(ARM_DROP_ZONE, armDropZoneStatus);
        }
    }

    @Override
    @Transactional
    public void processUnstructuredToArm() {
        processPendingUnstructured();
    }

    private void processPendingUnstructured() {
        preloadObjectRecordStatuses(objectDirectoryStatusRepository);

        ExternalLocationTypeEntity inboundLocation = externalLocationTypeRepository.getReferenceById(
            ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(
            ExternalLocationTypeEnum.ARM.getId());

        List<ExternalObjectDirectoryEntity> allPendingUnstructuredToArmEntities =
            getArmExternalObjectDirectoryEntities(inboundLocation, armLocation);

        for (var currentExternalObjectDirectory : allPendingUnstructuredToArmEntities) {

            ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory;
            ExternalObjectDirectoryEntity armExternalObjectDirectory;

            if (currentExternalObjectDirectory.getExternalLocationType().getId().equals(armLocation.getId())) {
                armExternalObjectDirectory = currentExternalObjectDirectory;

                var matchingEntity = getUnstructuredExternalObjectDirectoryEntity(armExternalObjectDirectory);
                if (matchingEntity.isPresent()) {
                    unstructuredExternalObjectDirectory = matchingEntity.get();
                } else {
                    updateTransferAttempts(armExternalObjectDirectory);
                    externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
                    log.error("Unable to get external object");
                    continue;
                }
            } else {
                unstructuredExternalObjectDirectory = currentExternalObjectDirectory;
                armExternalObjectDirectory = createArmExternalObjectDirectoryEntity(currentExternalObjectDirectory);
            }

            armExternalObjectDirectory.setStatus(armStatuses.get(ARM_INGESTION));
            externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
            String filename = generateFilename(armExternalObjectDirectory);

            boolean copyRawDataToArmSuccessful = copyRawDataToArm(unstructuredExternalObjectDirectory, armExternalObjectDirectory, filename);
            if (copyRawDataToArmSuccessful) {
                generateAndCopyMetadataToArm(armExternalObjectDirectory);
            }
        }
    }

    private void generateAndCopyMetadataToArm(ExternalObjectDirectoryEntity armExternalObjectDirectory) {

        ArchiveRecordFileInfo archiveRecordFileInfo = archiveRecordService.generateArchiveRecord(
                armExternalObjectDirectory,
                armExternalObjectDirectory.getTransferAttempts());


        File archiveRecordFile = archiveRecordFileInfo.getArchiveRecordFile();
        if (archiveRecordFileInfo.isFileGenerationSuccessful() && archiveRecordFile.exists()) {
            try {
                BinaryData metadataFileBinary = fileOperationService.saveFileToBinaryData(archiveRecordFile.getAbsolutePath());
                armDataManagementApi.saveBlobDataToArm(archiveRecordFileInfo.getArchiveRecordFile().getName(), metadataFileBinary);
                armExternalObjectDirectory.setStatus(armStatuses.get(ARM_DROP_ZONE));
                externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);

            } catch (BlobStorageException e) {
                log.error("Failed to move BLOB metadata for file {} due to {}", archiveRecordFile.getAbsolutePath(), e.getMessage());
                updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, FAILURE_ARM_MANIFEST_FILE_FAILED);
            }
        } else {
            log.error("Failed to generate metadata file {}", archiveRecordFile.getAbsolutePath());
            updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, FAILURE_ARM_MANIFEST_FILE_FAILED);
        }
    }

    private List<ExternalObjectDirectoryEntity> getArmExternalObjectDirectoryEntities(ExternalLocationTypeEntity inboundLocation,
                                                                                   ExternalLocationTypeEntity armLocation) {

        List<ObjectRecordStatusEntity> failedArmStatuses = new ArrayList<>();
        failedArmStatuses.add(armStatuses.get(FAILURE_ARM_RAW_DATA_FAILED));
        failedArmStatuses.add(armStatuses.get(FAILURE_ARM_MANIFEST_FILE_FAILED));

        var pendingUnstructuredExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            armStatuses.get(STORED),
            inboundLocation,
            armLocation
        );

        var failedArmExternalObjectDirectoryEntities = externalObjectDirectoryRepository.findNotFinishedAndNotExceededRetryInStorageLocation(
            failedArmStatuses,
            armLocation,
            armDataManagementConfiguration.getMaxRetryAttempts());

        List<ExternalObjectDirectoryEntity> allPendingUnstructuredToArmEntities =
            Stream.concat(pendingUnstructuredExternalObjectDirectoryEntities.stream(), failedArmExternalObjectDirectoryEntities.stream())
                .toList();
        return allPendingUnstructuredToArmEntities;
    }

    private boolean copyRawDataToArm(ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory,
                                     ExternalObjectDirectoryEntity armExternalObjectDirectory,
                                     String filename) {
        boolean copySuccessful = false;
        try {

            BinaryData inboundFile = dataManagementApi.getBlobDataFromUnstructuredContainer(
                unstructuredExternalObjectDirectory.getExternalLocation());

            armDataManagementApi.saveBlobDataToArm(filename, inboundFile);
            armExternalObjectDirectory.setChecksum(unstructuredExternalObjectDirectory.getChecksum());
            armExternalObjectDirectory.setExternalLocation(UUID.randomUUID());
            externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);

            copySuccessful = true;

        } catch (BlobStorageException e) {
            log.error("Failed to move BLOB data for file {} due to {}",
                      unstructuredExternalObjectDirectory.getExternalLocation(),
                      e.getMessage());

            updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, FAILURE_ARM_RAW_DATA_FAILED);
        }
        return copySuccessful;
    }

    private void updateExternalObjectDirectoryStatusToFailed(ExternalObjectDirectoryEntity armExternalObjectDirectory,
                                                             ObjectDirectoryStatusEnum objectDirectoryStatusEnum) {
        armExternalObjectDirectory.setStatus(armStatuses.get(objectDirectoryStatusEnum));
        updateTransferAttempts(armExternalObjectDirectory);
        externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
    }

    private Optional<ExternalObjectDirectoryEntity> getUnstructuredExternalObjectDirectoryEntity(
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        return externalObjectDirectoryRepository.findMatchingExternalObjectDirectoryEntityByLocation(
            armStatuses.get(STORED),
            externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId()),
            externalObjectDirectoryEntity.getMedia(),
            externalObjectDirectoryEntity.getTranscriptionDocumentEntity(),
            externalObjectDirectoryEntity.getAnnotationDocumentEntity());
    }

    private ExternalObjectDirectoryEntity createArmExternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectory) {

        ExternalObjectDirectoryEntity  armExternalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        armExternalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(ARM.getId()));
        armExternalObjectDirectoryEntity.setStatus(armStatuses.get(ARM_INGESTION));
        armExternalObjectDirectoryEntity.setExternalLocation(externalObjectDirectory.getExternalLocation());

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
        var systemUser = userAccountRepository.getReferenceById(SystemUsersEnum.DEFAULT.getId());
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
