package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.UnstructuredToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
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
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnstructuredToArmProcessorImpl implements UnstructuredToArmProcessor {

    private final ArchiveRecordService archiveRecordService;
    private final DataStoreToArmHelper unstructuredToArmHelper;
    private final UserIdentity userIdentity;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final LogApi logApi;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementApi armDataManagementApi;
    private final UnstructuredToArmProcessorConfiguration unstructuredToArmProcessorConfiguration;

    private static final int BLOB_ALREADY_EXISTS_STATUS_CODE = 409;

    @Override
    public void processUnstructuredToArm() {

        UserAccountEntity userAccount = userIdentity.getUserAccount();

        ExternalLocationTypeEntity inboundLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());

        List<ExternalObjectDirectoryEntity> allPendingUnstructuredToArmEntities = unstructuredToArmHelper.getEodEntitiesToSendToArm(
            inboundLocation,
            armLocation,
            unstructuredToArmProcessorConfiguration.getMaxArmSingleModeItems());

        for (var currentExternalObjectDirectory : allPendingUnstructuredToArmEntities) {
            try {
                ObjectRecordStatusEntity previousStatus = null;
                ExternalObjectDirectoryEntity unstructuredExternalObjectDirectory;
                ExternalObjectDirectoryEntity armExternalObjectDirectory;

                if (currentExternalObjectDirectory.getExternalLocationType().getId().equals(armLocation.getId())) {
                    armExternalObjectDirectory = currentExternalObjectDirectory;
                    previousStatus = armExternalObjectDirectory.getStatus();
                    var matchingEntity = unstructuredToArmHelper.getExternalObjectDirectoryEntity(
                        armExternalObjectDirectory,
                        externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId()),
                        EodHelper.storedStatus());
                    if (matchingEntity.isPresent()) {
                        unstructuredExternalObjectDirectory = matchingEntity.get();
                    } else {
                        log.error("Unable to find matching external object directory for {}", armExternalObjectDirectory.getId());
                        unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, EodHelper.failedArmRawDataStatus(),
                                                                                            userAccount);
                        continue;
                    }
                } else {
                    unstructuredExternalObjectDirectory = currentExternalObjectDirectory;
                    armExternalObjectDirectory = unstructuredToArmHelper.createArmExternalObjectDirectoryEntity(currentExternalObjectDirectory,
                                                                                                                EodHelper.armIngestionStatus(), userAccount);
                    unstructuredToArmHelper.updateExternalObjectDirectoryStatus(armExternalObjectDirectory, EodHelper.armIngestionStatus(), userAccount);
                }

                String rawFilename = unstructuredToArmHelper.generateRawFilename(armExternalObjectDirectory);
                log.info("Start of ARM Push processing for EOD {} running at: {}", armExternalObjectDirectory.getId(), OffsetDateTime.now());
                boolean copyRawDataToArmSuccessful = unstructuredToArmHelper.copyUnstructuredRawDataToArm(
                    unstructuredExternalObjectDirectory,
                    armExternalObjectDirectory,
                    rawFilename,
                    previousStatus,
                    userAccount
                );
                if (!copyRawDataToArmSuccessful) {
                    unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(
                        armExternalObjectDirectory,
                        EodHelper.failedArmRawDataStatus(), userAccount);
                }
                if (copyRawDataToArmSuccessful && generateAndCopyMetadataToArm(armExternalObjectDirectory, rawFilename, userAccount)) {
                    unstructuredToArmHelper.updateExternalObjectDirectoryStatus(armExternalObjectDirectory, EodHelper.armDropZoneStatus(), userAccount);
                    logApi.armPushSuccessful(armExternalObjectDirectory.getId());
                }
                log.info("Finished running ARM Push processing for EOD {} running at: {}", armExternalObjectDirectory.getId(), OffsetDateTime.now());
            } catch (Exception e) {
                log.error("Unable to push EOD {} to ARM", currentExternalObjectDirectory.getId(), e);
            }

        }
    }

    private boolean generateAndCopyMetadataToArm(ExternalObjectDirectoryEntity armExternalObjectDirectory, String rawFilename, UserAccountEntity userAccount) {
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
                    unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, EodHelper.failedArmManifestFileStatus(),
                                                                                        userAccount);
                    return false;
                }
            } catch (Exception e) {
                log.error("Unable to move BLOB metadata for file {} due to {}", archiveRecordFile.getAbsolutePath(), e.getMessage());
                unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, EodHelper.failedArmManifestFileStatus(),
                                                                                    userAccount);
                return false;
            }
        } else {
            log.error("Failed to generate metadata file {}", archiveRecordFile.getAbsolutePath());
            unstructuredToArmHelper.updateExternalObjectDirectoryStatusToFailed(armExternalObjectDirectory, EodHelper.failedArmManifestFileStatus(),
                                                                                userAccount);
            return false;
        }
        return true;
    }


}
