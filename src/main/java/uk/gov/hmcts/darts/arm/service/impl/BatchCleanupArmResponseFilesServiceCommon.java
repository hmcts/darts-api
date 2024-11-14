package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmBatchCleanupConfiguration;
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
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@RequiredArgsConstructor
@Slf4j
public class BatchCleanupArmResponseFilesServiceCommon implements BatchCleanupArmResponseFilesService {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final ArmDataManagementApi armDataManagementApi;
    private final UserIdentity userIdentity;
    private final ArmBatchCleanupConfiguration batchCleanupConfiguration;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final CurrentTimeHelper currentTimeHelper;
    private final ArmResponseFileHelper armResponseFileHelper;
    private final String manifestFilePrefix;


    @Override
    public void cleanupResponseFiles(int batchsize) {
        if (batchsize == 0) {
            log.warn("{}: {}: Batch Cleanup ARM Response Files - Batch size is 0, so not running", manifestFilePrefix);
            return;
        }
        List<ObjectRecordStatusEntity> statusToSearch = objectRecordStatusRepository.getReferencesByStatus(
            List.of(STORED, ARM_RPO_PENDING, ARM_RESPONSE_PROCESSING_FAILED, ARM_RESPONSE_MANIFEST_FAILED, ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED));
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());

        Integer cleanupBufferMinutes = batchCleanupConfiguration.getBufferMinutes();
        OffsetDateTime dateTimeForDeletion = currentTimeHelper.currentOffsetDateTime().minusMinutes(cleanupBufferMinutes);

        List<String> manifestFilenames =
            externalObjectDirectoryRepository.findBatchCleanupManifestFilenames(
                statusToSearch,
                armLocation,
                false,
                dateTimeForDeletion,
                manifestFilePrefix,
                Limit.of(batchsize)
            );
        if (manifestFilenames.isEmpty()) {
            log.info("{}: Batch Cleanup ARM Response Files - 0 rows returned, so stopping.", manifestFilePrefix);
            return;
        }

        if (CollectionUtils.isNotEmpty(manifestFilenames)) {
            int counter = 1;
            UserAccountEntity userAccount = userIdentity.getUserAccount();
            for (String manifestFilename : manifestFilenames) {
                log.info("{}: Batch Cleanup ARM Response Files - about to process manifest filename {}, row {} of {} rows", manifestFilePrefix,
                         manifestFilename, counter++,
                         manifestFilenames.size());
                cleanupFilesByManifestFilename(userAccount, armLocation, statusToSearch, dateTimeForDeletion, manifestFilename);
            }
        } else {
            log.info("{}: No ARM responses found to be deleted", manifestFilePrefix);
        }
    }

    private void cleanupFilesByManifestFilename(UserAccountEntity userAccount, ExternalLocationTypeEntity armLocation,
                                                List<ObjectRecordStatusEntity> statusToSearch,
                                                OffsetDateTime dateTimeForDeletion, String manifestFilename) {
        try {
            log.debug("{}: Finding associated eodEntries for manifest filename {}", manifestFilePrefix, manifestFilename);
            List<ExternalObjectDirectoryEntity> eodEntriesWithManifestFilename = externalObjectDirectoryRepository.findBatchCleanupEntriesByManifestFilename(
                armLocation, false, manifestFilename);

            List<Integer> statusIdList = statusToSearch.stream().map(ObjectRecordStatusEntity::getId).toList();
            boolean statusAllValid = eodEntriesWithManifestFilename.stream().allMatch(eodEntity -> statusIdList.contains(eodEntity.getStatusId()));
            if (!statusAllValid) {
                log.warn("{}: Not all statuses are valid for manifestFilename {}, so skipping to next manifestFilename.", manifestFilePrefix, manifestFilename);
            }

            boolean lastModifiedAllValid = eodEntriesWithManifestFilename.stream().allMatch(
                eodEntity -> eodEntity.getLastModifiedDateTime().isBefore(dateTimeForDeletion));
            if (!lastModifiedAllValid) {
                log.warn("{}: Not all entries have been modified before {} for manifestFilename {}, so skipping to next manifestFilename.",
                         manifestFilePrefix, dateTimeForDeletion, manifestFilename);
            }

            log.debug("{}: Found ARM manifest file {} for cleanup", manifestFilePrefix, manifestFilename);
            List<InputUploadAndAssociatedFilenames> inputUploadAndAssociatedList = armResponseFileHelper.getCorrespondingArmFilesForManifestFilename(
                manifestFilename);
            //inputUploadAndAssociatedList should only contain 1 matching InputUpload file, but looping through it just in case.
            for (InputUploadAndAssociatedFilenames inputUploadAndAssociates : inputUploadAndAssociatedList) {
                deleteResponseFiles(userAccount, inputUploadAndAssociates, eodEntriesWithManifestFilename);
            }

        } catch (UnableToReadArmFileException e) {
            log.error("{}: Cannot process manifest filename {} due to corrupt ARM file.", manifestFilePrefix, manifestFilename);
        }

    }

    private void deleteResponseFiles(UserAccountEntity userAccount, InputUploadAndAssociatedFilenames inputUploadAndAssociates,
                                     List<ExternalObjectDirectoryEntity> eodEntriesWithManifestFilename) {
        List<EodIdAndAssociatedFilenames> eodIdAndAssociatedFilenamesList = inputUploadAndAssociates.getEodIdAndAssociatedFilenamesList();
        List<Boolean> deletedFileStatuses = new ArrayList<>();
        String inputUploadFilename = inputUploadAndAssociates.getInputUploadFilename();
        for (EodIdAndAssociatedFilenames eodIdAndAssociatedFilenames : eodIdAndAssociatedFilenamesList) {
            boolean successfullyDeletedAssociatedFiles = true;
            Integer eodId = eodIdAndAssociatedFilenames.getEodId();
            List<String> associatedFiles = eodIdAndAssociatedFilenames.getAssociatedFiles();
            log.info("{}: {}: There are {} response files for EOD {}, linked to inputUpload filename {}", manifestFilePrefix, associatedFiles.size(), eodId,
                     inputUploadFilename);
            for (String associatedFile : associatedFiles) {
                try {
                    log.info("{}: About to delete file {} for EOD {}, linked to inputUpload filename {}", manifestFilePrefix, associatedFile, eodId,
                             inputUploadFilename);
                    boolean responseFileDeletedSuccessfully = armDataManagementApi.deleteBlobData(associatedFile);
                    if (!responseFileDeletedSuccessfully) {
                        log.warn("{}: Response file {} failed to delete successfully.", manifestFilePrefix, associatedFile);
                        successfullyDeletedAssociatedFiles = false;
                    }
                } catch (Exception e) {
                    log.error("Failure to delete response file {} for EOD {} - {}", manifestFilePrefix, associatedFile, eodId, e.getMessage(), e);
                    successfullyDeletedAssociatedFiles = false;
                }
                if (!successfullyDeletedAssociatedFiles) {
                    break;
                }
            }
            deletedFileStatuses.add(successfullyDeletedAssociatedFiles);
            if (successfullyDeletedAssociatedFiles) {
                Optional<ExternalObjectDirectoryEntity> eodEntityOpt = externalObjectDirectoryRepository.findById(eodId);
                if (eodEntityOpt.isEmpty()) {
                    log.error("EodEntity {} in response file for {} cannot be found.", manifestFilePrefix, eodId, inputUploadFilename);
                    break;
                }
                ExternalObjectDirectoryEntity eodEntityFromRelationId = eodEntityOpt.get();

                boolean matchesEodWithManifestFile = false;
                for (ExternalObjectDirectoryEntity eodEntity : eodEntriesWithManifestFilename) {
                    if (eodEntity.getId().equals(eodEntityFromRelationId.getId())) {
                        matchesEodWithManifestFile = true;
                        eodEntriesWithManifestFilename.remove(eodEntity);
                        break;
                    }
                }

                if (!matchesEodWithManifestFile) {
                    log.warn("{}: Deleted arm response file is not associated with any eodEntity with manifest file {}, but mentions relationId {}.",
                             manifestFilePrefix, inputUploadFilename, eodEntityFromRelationId);
                }
                setResponseCleaned(userAccount, eodEntityFromRelationId);
            }
        }

        if (CollectionUtils.isNotEmpty(eodEntriesWithManifestFilename)) {
            log.warn("{}: All ARM response files related to InputUpload file {} have been deleted, but none referred to the following eodId's - {}.",
                     manifestFilePrefix, inputUploadFilename, eodEntriesWithManifestFilename.stream().map(ExternalObjectDirectoryEntity::getId).toList());
        }

        if (deletedFileStatuses.stream().allMatch(Boolean.TRUE::equals)) {
            log.info("{}: All associated Eod entries deleted, about to delete InputUpload file {}", manifestFilePrefix, inputUploadFilename);
            // Make sure to only delete the Input Upload filename after the other response files have been deleted as once this is deleted
            // you cannot find the other response files
            boolean inputUploadFileDeletedSuccessfully = armDataManagementApi.deleteBlobData(inputUploadFilename);
            if (inputUploadFileDeletedSuccessfully) {
                log.info("{}: Successfully cleaned up response files for InputUpload file {}", manifestFilePrefix, inputUploadFilename);
            } else {
                log.warn("{}: Unable to delete input upload response file {}", manifestFilePrefix, inputUploadFilename);
            }
        } else {
            log.warn("{}: Unable to delete all response files for InputUpload file {}", manifestFilePrefix, inputUploadFilename);
        }
    }

    private void setResponseCleaned(UserAccountEntity userAccount, ExternalObjectDirectoryEntity externalObjectDirectory) {
        externalObjectDirectory.setResponseCleaned(true);
        externalObjectDirectory.setLastModifiedBy(userAccount);
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
    }
}