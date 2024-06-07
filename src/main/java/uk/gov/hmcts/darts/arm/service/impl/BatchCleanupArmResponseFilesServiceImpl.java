package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.exception.UnableToReadArmFileException;
import uk.gov.hmcts.darts.arm.helper.ArmResponseFileHelper;
import uk.gov.hmcts.darts.arm.model.EodIdAndAssociatedFilenames;
import uk.gov.hmcts.darts.arm.model.InputUploadAndAssociatedFilenames;
import uk.gov.hmcts.darts.arm.service.BatchCleanupArmResponseFilesService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
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

        List<ExternalObjectDirectoryEntity> eodEntitiesToBeDeleted =
            externalObjectDirectoryRepository.findBatchArmResponseFiles(
                statusToSearch,
                armLocation,
                false,
                dateTimeForDeletion,
                armDataManagementConfiguration.getManifestFilePrefix(),
                batchsize
            );

        if (CollectionUtils.isNotEmpty(eodEntitiesToBeDeleted)) {
            int counter = 1;
            for (ExternalObjectDirectoryEntity eodEntity : eodEntitiesToBeDeleted) {
                log.info("Batch Cleanup ARM Response Files about to process {} of {} rows", counter++, eodEntitiesToBeDeleted.size());
                try {
                    cleanupResponseFilesForExternalObjectDirectory(eodEntity);
                } catch (UnableToReadArmFileException e) {
                    log.error("Cannot process eodId {} due to corrupt ARM file.", eodEntity.getId());
                }
            }
        } else {
            log.info("No ARM responses found to be deleted}");
        }
    }

    private void cleanupResponseFilesForExternalObjectDirectory(ExternalObjectDirectoryEntity eodEntity) throws UnableToReadArmFileException {
        String manifestFile = eodEntity.getManifestFile();
        log.debug("Found ARM manifest file {} for cleanup", manifestFile);
        List<InputUploadAndAssociatedFilenames> inputUploadAndAssociatedList = armResponseFileHelper.getCorrespondingArmFilesForManifestFilename(manifestFile);
        for (InputUploadAndAssociatedFilenames inputUploadAndAssociates : inputUploadAndAssociatedList) {
            deleteResponseFiles(eodEntity, inputUploadAndAssociates);
        }
    }

    private void deleteResponseFiles(ExternalObjectDirectoryEntity externalObjectDirectory, InputUploadAndAssociatedFilenames inputUploadAndAssociates) {
        List<EodIdAndAssociatedFilenames> eodIdAndAssociatedFilenamesList = inputUploadAndAssociates.getEodIdAndAssociatedFilenamesList();
        List<Boolean> deletedFileStatuses = new ArrayList<>();
        for (EodIdAndAssociatedFilenames eodIdAndAssociatedFilenames : eodIdAndAssociatedFilenamesList) {
            boolean successfullyDeletedAssociatedFiles = true;
            Integer eodId = eodIdAndAssociatedFilenames.getEodId();
            for (String associatedFile : eodIdAndAssociatedFilenames.getAssociatedFiles()) {
                try {
                    log.info("About to delete file {} for EOD {}, linked to EOD {}", associatedFile, eodId, externalObjectDirectory.getId());
                    boolean responseFileDeletedSuccessfully = armDataManagementApi.deleteBlobData(associatedFile);
                    deletedFileStatuses.add(responseFileDeletedSuccessfully);
                    if (!responseFileDeletedSuccessfully) {
                        log.warn("Response file {} failed to delete successfully, but ignoring.", associatedFile);
                        successfullyDeletedAssociatedFiles = false;
                        break;
                    }
                } catch (Exception e) {
                    log.error("Failure to delete response file {} for EOD {} - {}", associatedFile, externalObjectDirectory.getId(), e.getMessage(), e);
                    deletedFileStatuses.add(false);
                    successfullyDeletedAssociatedFiles = false;
                }
            }
            if (successfullyDeletedAssociatedFiles) {
                Optional<ExternalObjectDirectoryEntity> eodEntityOpt = externalObjectDirectoryRepository.findById(eodId);
                if (eodEntityOpt.isEmpty()) {
                    log.error("EodEntity {} cannot be found.", eodId);
                    break;
                }
                ExternalObjectDirectoryEntity eodEntity = eodEntityOpt.get();
                setResponseCleaned(eodEntity);
            }
        }


        if (deletedFileStatuses.stream().allMatch(Boolean.TRUE::equals)) {
            String armInputUploadFilename = inputUploadAndAssociates.getInputUploadFilename();
            log.info("All associated Eod entries deleted, about to delete {} for EOD {}", armInputUploadFilename, externalObjectDirectory.getId());
            // Make sure to only delete the Input Upload filename after the other response files have been deleted as once this is deleted
            // you cannot find the other response files
            boolean inputUploadFileDeletedSuccessfully = armDataManagementApi.deleteBlobData(armInputUploadFilename);
            setResponseCleaned(externalObjectDirectory);
            if (inputUploadFileDeletedSuccessfully) {
                log.info("Successfully cleaned up response files for EOD {}", externalObjectDirectory.getId());
            } else {
                log.warn("Unable to delete input upload response file for EOD {}", externalObjectDirectory.getId());
            }
        } else {
            log.warn("Unable to delete all response files for EOD {}", externalObjectDirectory.getId());
        }
    }

    private void setResponseCleaned(ExternalObjectDirectoryEntity externalObjectDirectory) {
        externalObjectDirectory.setResponseCleaned(true);
        externalObjectDirectory.setLastModifiedBy(userAccount);
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
    }

}
