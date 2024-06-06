package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.helper.ArmResponseFileHelper;
import uk.gov.hmcts.darts.arm.model.InputUploadAndAssociatedFilenames;
import uk.gov.hmcts.darts.arm.service.BatchCleanupArmResponseFilesService;
import uk.gov.hmcts.darts.arm.util.ArmResponseFilesUtil;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.DELETED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.task.runner.AutomatedTaskName.BATCH_CLEANUP_ARM_RESPONSE_FILES_TASK_NAME;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchCleanupArmResponseFilesServiceImpl implements BatchCleanupArmResponseFilesService {

    private static final String taskName = BATCH_CLEANUP_ARM_RESPONSE_FILES_TASK_NAME.getTaskName();

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;

    private final ArmDataManagementApi armDataManagementApi;
    private final UserIdentity userIdentity;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final CurrentTimeHelper currentTimeHelper;
    private final ArmResponseFileHelper armResponseFileHelper;

    private UserAccountEntity userAccount;


    @Override
    public void cleanupResponseFiles(int batchsize) {
        List<ObjectRecordStatusEntity> statusToSearch = objectRecordStatusRepository.getReferencesByStatus(
            List.of(STORED, ARM_RESPONSE_PROCESSING_FAILED, ARM_RESPONSE_MANIFEST_FAILED, ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED));
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());

        userAccount = userIdentity.getUserAccount();

        Integer cleanupBufferMinutes = armDataManagementConfiguration.getBatchResponseCleanupBufferMinutes();
        OffsetDateTime dateTimeForDeletion = currentTimeHelper.currentOffsetDateTime().minusMinutes(cleanupBufferMinutes);

        List<ExternalObjectDirectoryEntity> objectDirectoryDataToBeDeleted =
            externalObjectDirectoryRepository.findBatchArmResponseFiles(
                statusToSearch,
                armLocation,
                false,
                dateTimeForDeletion,
                armDataManagementConfiguration.getManifestFilePrefix(),
                batchsize
            );

        if (CollectionUtils.isNotEmpty(objectDirectoryDataToBeDeleted)) {
            int counter = 1;
            for (ExternalObjectDirectoryEntity externalObjectDirectory : objectDirectoryDataToBeDeleted) {
                log.info("Batch Cleanup ARM Response Files about to process {} of {} rows", counter++, objectDirectoryDataToBeDeleted.size());
                cleanupResponseFilesForExternalObjectDirectory(externalObjectDirectory);
            }
        } else {
            log.info("No ARM responses found to be deleted}");
        }
    }

    private void cleanupResponseFilesForExternalObjectDirectory(ExternalObjectDirectoryEntity eodEntity) {
        String manifestFile = eodEntity.getManifestFile();
        log.debug("Found ARM manifest file {} for cleanup", manifestFile);
        List<InputUploadAndAssociatedFilenames> correspondingArmFiles = armResponseFileHelper.getCorrespondingArmFilesForManifestFilename(manifestFile);
        for (InputUploadAndAssociatedFilenames correspondingArmFile : correspondingArmFiles) {
            deleteResponseFiles(eodEntity, correspondingArmFile);
        }
    }

    private void deleteResponseFiles(ExternalObjectDirectoryEntity externalObjectDirectory, InputUploadAndAssociatedFilenames inputUploadAndAssociates) {
        List<String> associatedFiles = inputUploadAndAssociates.getAssociatedFiles();
        List<Boolean> deletedFileStatuses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(associatedFiles)) {
            for (String responseFile : associatedFiles) {
                try {
                    Integer eodIdFromArmFile = armResponseFileHelper.getEodIdFromArmFile(responseFile);
                    log.info("About to delete file {} for EOD {}, linked to EOD {}", responseFile, eodIdFromArmFile, externalObjectDirectory.getId());
                    boolean responseFileDeletedSuccessfully = armDataManagementApi.deleteBlobData(responseFile);
                    deletedFileStatuses.add(responseFileDeletedSuccessfully);
                    if (!responseFileDeletedSuccessfully) {
                        log.warn("Response file {} failed to delete successfully, but ignoring.", responseFile);
                        break;
                    }
                    Optional<ExternalObjectDirectoryEntity> eodEntityOpt = externalObjectDirectoryRepository.findById(eodIdFromArmFile);
                    if (eodEntityOpt.isEmpty()) {
                        log.error("EodEntity {}, found in ARM response file {}, cannot be found.", eodIdFromArmFile, responseFile);
                        throw new Exception();
                    }
                    ExternalObjectDirectoryEntity eodEntity = eodEntityOpt.get();
                    updateExternalObjectDirectory(eodEntity, DELETED);
                } catch (Exception e) {
                    log.error("Failure to delete response file {} for EOD {} - {}", responseFile, externalObjectDirectory.getId(), e.getMessage(), e);
                    deletedFileStatuses.add(false);
                }
            }
        }
        if (deletedFileStatuses.stream().allMatch(Boolean.TRUE::equals)) {
            String armInputUploadFilename = inputUploadAndAssociates.getInputUploadFilename();
            log.info("All associated Eod entries deleted, about to delete {} for EOD {}", armInputUploadFilename, externalObjectDirectory.getId());
            // Make sure to only delete the Input Upload filename after the other response files have been deleted as once this is deleted
            // you cannot find the other response files
            boolean inputUploadFileDeletedSuccessfully = armDataManagementApi.deleteBlobData(armInputUploadFilename);
            if (inputUploadFileDeletedSuccessfully) {
                externalObjectDirectory.setResponseCleaned(true);
                updateExternalObjectDirectory(externalObjectDirectory);
                log.info("Successfully cleaned up response files for EOD {}", externalObjectDirectory.getId());
            } else {
                log.warn("Unable to delete input upload response file for EOD {}", externalObjectDirectory.getId());
                externalObjectDirectory.setResponseCleaned(true);
                updateExternalObjectDirectory(externalObjectDirectory);
            }
        } else {
            log.warn("Unable to delete all response files for EOD {}", externalObjectDirectory.getId());
        }
    }

    private void updateExternalObjectDirectory(ExternalObjectDirectoryEntity externalObjectDirectory, ObjectRecordStatusEnum status) {
        externalObjectDirectory.setStatus(objectRecordStatusRepository.getReferenceById(status.getId()));
        updateExternalObjectDirectory(externalObjectDirectory);
    }

    private void updateExternalObjectDirectory(ExternalObjectDirectoryEntity externalObjectDirectory) {
        externalObjectDirectory.setLastModifiedBy(userAccount);
        externalObjectDirectory.setLastModifiedDateTime(OffsetDateTime.now());
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
    }

    private String getPrefix(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return new StringBuilder(String.valueOf(externalObjectDirectory.getId()))
            .append(ARM_FILENAME_SEPARATOR)
            .append(ArmResponseFilesUtil.getObjectTypeId(externalObjectDirectory))
            .append(ARM_FILENAME_SEPARATOR).toString();
    }
}
