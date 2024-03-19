package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_INGESTION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;

@Service
@Slf4j
@ConditionalOnExpression("${darts.storage.arm.batch-size} > 0")
public class UnstructuredToArmBatchProcessorImpl extends AbstractUnstructuredToArmProcessor {

    private static final int BLOB_ALREADY_EXISTS_STATUS_CODE = 409;
    private final DataManagementApi dataManagementApi;
    private final ArmDataManagementApi armDataManagementApi;
    private final UserIdentity userIdentity;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final FileOperationService fileOperationService;
    private final ArchiveRecordService archiveRecordService;
    private final ExternalObjectDirectoryService eodService;

    private UserAccountEntity userAccount;

    public UnstructuredToArmBatchProcessorImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                               ObjectRecordStatusRepository objectRecordStatusRepository,
                                               ExternalLocationTypeRepository externalLocationTypeRepository, DataManagementApi dataManagementApi,
                                               ArmDataManagementApi armDataManagementApi, UserIdentity userIdentity,
                                               ArmDataManagementConfiguration armDataManagementConfiguration, FileOperationService fileOperationService,
                                               ArchiveRecordService archiveRecordService,
                                               ExternalObjectDirectoryService eodService) {
        super(objectRecordStatusRepository, userIdentity, externalObjectDirectoryRepository, externalLocationTypeRepository);
        this.eodService = eodService;
        this.dataManagementApi = dataManagementApi;
        this.armDataManagementApi = armDataManagementApi;
        this.userIdentity = userIdentity;
        this.armDataManagementConfiguration = armDataManagementConfiguration;
        this.fileOperationService = fileOperationService;
        this.archiveRecordService = archiveRecordService;
    }

    @Override
    public void processUnstructuredToArm() {

        List<ExternalObjectDirectoryEntity> pendingEODs = findPendingToArmEODs();

        if (!pendingEODs.isEmpty()) {
            userAccount = userIdentity.getUserAccount();
            createEmptyManifestFile();


            for (var pendingEod : pendingEODs) {


            }
        }







    }

    private List<ExternalObjectDirectoryEntity> findPendingToArmEODs() {

        var batchSize = armDataManagementConfiguration.getBatchSize();
        var destinationLocation = eodService.armLocation();
        ExternalLocationTypeEntity sourceLocation = null;
        if (armDataManagementConfiguration.getArmClient().equalsIgnoreCase("darts")) {
             sourceLocation = eodService.unstructuredLocation();
        } else if (armDataManagementConfiguration.getArmClient().equalsIgnoreCase("dets")) {
            sourceLocation = eodService.detsLocation();
        } else {
            log.error("unknown arm client {}", armDataManagementConfiguration.getArmClient());
            return Collections.emptyList();
        }

        var result = new ArrayList<ExternalObjectDirectoryEntity>();
        result.addAll(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            eodService.storedStatus(),
            sourceLocation,
            destinationLocation,
            Pageable.ofSize(batchSize)
        ));
        var remaining = batchSize - result.size();
        if (remaining > 0) {
            result.addAll(eodService.findFailedStillRetriableArmEODs(Pageable.ofSize(remaining)));
        }
        return result;
    }

    private void createEmptyManifestFile() {
        var manifestFileName = "%s_%s";
        String.format(manifestFileName, armDataManagementConfiguration.getManifestFilePrefix(), UUID.randomUUID());
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
        externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
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
                    updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, eodService.failedArmManifestFileStatus());
                    return false;
                }
            } catch (Exception e) {
                log.error("Unable to move BLOB metadata for file {} due to {}", archiveRecordFile.getAbsolutePath(), e.getMessage());
                updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, eodService.failedArmManifestFileStatus());
                return false;
            }
        } else {
            log.error("Failed to generate metadata file {}", archiveRecordFile.getAbsolutePath());
            updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, eodService.failedArmManifestFileStatus());
            return false;
        }
        return true;
    }


    private boolean copyRawDataToArm(ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory,
                                     ExternalObjectDirectoryEntity armExternalObjectDirectory,
                                     String filename,
                                     ObjectRecordStatusEntity previousStatus) {
        try {
            if (previousStatus == null
                || ARM_RAW_DATA_FAILED.getId().equals(previousStatus.getId())
                || ARM_INGESTION.getId().equals(previousStatus.getId())) {
                Instant start = Instant.now();
                log.info("ARM PERFORMANCE PUSH START for EOD {} started at {}", armExternalObjectDirectory.getId(), start);

                BinaryData inboundFile = dataManagementApi.getBlobDataFromUnstructuredContainer(
                    unstructuredExternalObjectDirectory.getExternalLocation());
                log.info("About to push raw data to ARM for EOD {}", armExternalObjectDirectory.getId());
                armDataManagementApi.saveBlobDataToArm(filename, inboundFile);
                log.info("Pushed raw data to ARM for EOD {}", armExternalObjectDirectory.getId());

                Instant finish = Instant.now();
                long timeElapsed = Duration.between(start, finish).toMillis();
                log.info("ARM PERFORMANCE PUSH END for EOD {} ended at {}", armExternalObjectDirectory.getId(), finish);
                log.info("ARM PERFORMANCE PUSH ELAPSED TIME for EOD {} took {} ms", armExternalObjectDirectory.getId(), timeElapsed);

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
                updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, eodService.failedArmRawDataStatus());
                return false;
            }
        } catch (Exception e) {
            log.error(
                "Error moving BLOB data for file {} due to {}",
                unstructuredExternalObjectDirectory.getExternalLocation(),
                e.getMessage()
            );

            updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, eodService.failedArmRawDataStatus());
            return false;
        }

        return true;
    }

    private void updateExternalObjectDirectoryStatusToFailed(ExternalObjectDirectoryEntity armExternalObjectDirectory,
                                                             ObjectRecordStatusEntity objectRecordStatus) {
        log.debug(
            "Updating ARM status from {} to {} for ID {}",
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



    private ExternalObjectDirectoryEntity createArmExternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectory) {

        ExternalObjectDirectoryEntity armExternalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        armExternalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(ARM.getId()));
        armExternalObjectDirectoryEntity.setStatus(eodService.armIngestionStatus());
        armExternalObjectDirectoryEntity.setExternalLocation(externalObjectDirectory.getExternalLocation());
        armExternalObjectDirectoryEntity.setVerificationAttempts(1);

        if (nonNull(externalObjectDirectory.getMedia())) {
            armExternalObjectDirectoryEntity.setMedia(externalObjectDirectory.getMedia());
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            armExternalObjectDirectoryEntity.setTranscriptionDocumentEntity(externalObjectDirectory.getTranscriptionDocumentEntity());
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            armExternalObjectDirectoryEntity.setAnnotationDocumentEntity(externalObjectDirectory.getAnnotationDocumentEntity());
        } else if (nonNull(externalObjectDirectory.getCaseDocument())) {
            armExternalObjectDirectoryEntity.setCaseDocument(externalObjectDirectory.getCaseDocument());
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
        } else if (nonNull(externalObjectDirectoryEntity.getCaseDocument())) {
            documentId = externalObjectDirectoryEntity.getCaseDocument().getId();
        }

        return String.format("%s_%s_%s", entityId, documentId, transferAttempts);
    }

    private void updateTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        int currentNumberOfAttempts = externalObjectDirectoryEntity.getTransferAttempts();
        externalObjectDirectoryEntity.setTransferAttempts(currentNumberOfAttempts + 1);
    }
}
