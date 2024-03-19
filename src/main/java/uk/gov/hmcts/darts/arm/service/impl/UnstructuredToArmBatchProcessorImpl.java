package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.ArchiveRecordFileInfo;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@ConditionalOnExpression("${darts.storage.arm.batch-size} > 0")
public class UnstructuredToArmBatchProcessorImpl extends AbstractUnstructuredToArmProcessor {

    private static final int BLOB_ALREADY_EXISTS_STATUS_CODE = 409;
    private final UserIdentity userIdentity;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final FileOperationService fileOperationService;
    private final ArchiveRecordService archiveRecordService;
    private final ExternalObjectDirectoryService eodService;

    private UserAccountEntity userAccount;

    public UnstructuredToArmBatchProcessorImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                               ObjectRecordStatusRepository objectRecordStatusRepository,
                                               ExternalLocationTypeRepository externalLocationTypeRepository,
                                               DataManagementApi dataManagementApi,
                                               ArmDataManagementApi armDataManagementApi,
                                               UserIdentity userIdentity,
                                               ArmDataManagementConfiguration armDataManagementConfiguration,
                                               FileOperationService fileOperationService,
                                               ArchiveRecordService archiveRecordService,
                                               ExternalObjectDirectoryService eodService) {
        super(objectRecordStatusRepository, userIdentity, externalObjectDirectoryRepository, externalLocationTypeRepository, dataManagementApi, armDataManagementApi);
        this.userIdentity = userIdentity;
        this.armDataManagementConfiguration = armDataManagementConfiguration;
        this.fileOperationService = fileOperationService;
        this.archiveRecordService = archiveRecordService;
        this.eodService = eodService;
    }

    @Override
    public void processUnstructuredToArm() {

        List<ExternalObjectDirectoryEntity> allPendingUnstructuredToArmEntities = getArmExternalObjectDirectoryEntities(
            armDataManagementConfiguration.getArmClient(), armDataManagementConfiguration.getBatchSize()
        );

        if (!allPendingUnstructuredToArmEntities.isEmpty()) {

            userAccount = userIdentity.getUserAccount();
            File manifestFile = createEmptyManifestFile();

            ObjectRecordStatusEntity previousStatus = null;
            ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory;
            ExternalObjectDirectoryEntity armExternalObjectDirectory;

            for (var currentExternalObjectDirectory : allPendingUnstructuredToArmEntities) {
                try {
                    if (currentExternalObjectDirectory.getExternalLocationType().getId().equals(EodEntities.armLocation.getId())) {
                        armExternalObjectDirectory = currentExternalObjectDirectory;
                        previousStatus = armExternalObjectDirectory.getStatus();
                        var matchingEntity = getUnstructuredExternalObjectDirectoryEntity(armExternalObjectDirectory, EodEntities.storedStatus);
                        if (matchingEntity.isPresent()) {
                            unstructuredExternalObjectDirectory = matchingEntity.get();
                            armExternalObjectDirectory.setManifestFile(manifestFile.getName());
                            updateExternalObjectDirectoryStatus(armExternalObjectDirectory, EodEntities.armIngestionStatus);
                        } else {
                            log.error("Unable to find matching external object directory for {}", armExternalObjectDirectory.getId());
                            updateTransferAttempts(armExternalObjectDirectory);
                            updateExternalObjectDirectoryStatus(armExternalObjectDirectory, EodEntities.failedArmRawDataStatus);
                            continue;
                        }
                    } else {
                        unstructuredExternalObjectDirectory = currentExternalObjectDirectory;
                        armExternalObjectDirectory = createArmExternalObjectDirectoryEntity(currentExternalObjectDirectory, EodEntities.armIngestionStatus);
                        armExternalObjectDirectory.setManifestFile(manifestFile.getName());
                        externalObjectDirectoryRepository.saveAndFlush(armExternalObjectDirectory);
                    }

                    String rawFilename = generateFilename(armExternalObjectDirectory);
                    log.info("Start of ARM Push processing for EOD {} running at: {}", armExternalObjectDirectory.getId(), OffsetDateTime.now());
                    boolean copyRawDataToArmSuccessful = copyRawDataToArm(
                        unstructuredExternalObjectDirectory,
                        armExternalObjectDirectory,
                        rawFilename,
                        previousStatus
                    );


                } catch (Exception e) {
                    //TODO fix error message
                    log.error("Unable to push EOD {} to ARM", currentExternalObjectDirectory.getId(), e);
                }
            }
        }
    }

    private List<ExternalObjectDirectoryEntity> getArmExternalObjectDirectoryEntities(String armClient, int batchSize) {

        ExternalLocationTypeEntity sourceLocation = null;
        if (armClient.equalsIgnoreCase("darts")) {
             sourceLocation = EodEntities.unstructuredLocation;
        } else if (armClient.equalsIgnoreCase("dets")) {
            sourceLocation = EodEntities.detsLocation;
        } else {
            log.error("unknown arm client {}", armDataManagementConfiguration.getArmClient());
            return Collections.emptyList();
        }

        var result = new ArrayList<ExternalObjectDirectoryEntity>();
        result.addAll(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(
            EodEntities.storedStatus,
            sourceLocation,
            EodEntities.armLocation,
            Pageable.ofSize(batchSize)
        ));
        var remaining = batchSize - result.size();
        if (remaining > 0) {
            result.addAll(eodService.findFailedStillRetriableArmEODs(Pageable.ofSize(remaining)));
        }
        return result;
    }

    @SneakyThrows
    private File createEmptyManifestFile() {
        var fileNameFormat = "%s_%s.%s";
        var fileName = String.format(fileNameFormat,
                                     armDataManagementConfiguration.getManifestFilePrefix(),
                                     UUID.randomUUID(),
                                     armDataManagementConfiguration.getFileExtension()
        );
        var manifestFile = new File(armDataManagementConfiguration.getTempBlobWorkspace(), fileName);
//        Files.createFile(manifestFile.getParentFile().toPath());
        return manifestFile;
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
                    updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, EodEntities.failedArmManifestFileStatus);
                    return false;
                }
            } catch (Exception e) {
                log.error("Unable to move BLOB metadata for file {} due to {}", archiveRecordFile.getAbsolutePath(), e.getMessage());
                updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, EodEntities.failedArmManifestFileStatus);
                return false;
            }
        } else {
            log.error("Failed to generate metadata file {}", archiveRecordFile.getAbsolutePath());
            updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, EodEntities.failedArmManifestFileStatus);
            return false;
        }
        return true;
    }

}
