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
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

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

    protected final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    protected final ObjectRecordStatusRepository objectRecordStatusRepository;
    protected final ExternalLocationTypeRepository externalLocationTypeRepository;
    protected final ArmDataManagementApi armDataManagementApi;
    protected final UserIdentity userIdentity;
    protected final ArmBatchCleanupConfiguration batchCleanupConfiguration;
    protected final ArmDataManagementConfiguration armDataManagementConfiguration;
    protected final CurrentTimeHelper currentTimeHelper;
    protected final ArmResponseFileHelper armResponseFileHelper;
    protected final String manifestFilePrefix;
    protected final String loggingPrefix;

    public BatchCleanupArmResponseFilesServiceCommon(ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                     ObjectRecordStatusRepository objectRecordStatusRepository,
                                                     ExternalLocationTypeRepository externalLocationTypeRepository,
                                                     ArmDataManagementApi armDataManagementApi,
                                                     UserIdentity userIdentity,
                                                     ArmBatchCleanupConfiguration batchCleanupConfiguration,
                                                     ArmDataManagementConfiguration armDataManagementConfiguration,
                                                     CurrentTimeHelper currentTimeHelper,
                                                     ArmResponseFileHelper armResponseFileHelper,
                                                     String manifestFilePrefix) {
        this(externalObjectDirectoryRepository, objectRecordStatusRepository, externalLocationTypeRepository, armDataManagementApi, userIdentity,
             batchCleanupConfiguration, armDataManagementConfiguration, currentTimeHelper, armResponseFileHelper,
             manifestFilePrefix, manifestFilePrefix);
    }


    @Override
    public void cleanupResponseFiles(int batchsize) {
        if (batchsize == 0) {
            log.warn("{}: Batch Cleanup ARM Response Files - Batch size is 0, so not running", loggingPrefix);
            return;
        }

        List<String> manifestFilenames = getManifestFileNames(batchsize);
        if (manifestFilenames.isEmpty()) {
            log.info("{}: Batch Cleanup ARM Response Files - 0 rows returned, so stopping.", loggingPrefix);
            return;
        }
        log.info("{}: Batch Cleanup ARM Response Files - {} manifest filenames found to process out of batch size {}",
                 loggingPrefix, manifestFilenames.size(), batchsize);
        OffsetDateTime dateTimeForDeletion = getDateTimeForDeletion();
        List<ObjectRecordStatusEntity> statusToSearch = getStatusToSearch();
        if (CollectionUtils.isNotEmpty(manifestFilenames)) {
            int counter = 1;
            UserAccountEntity userAccount = userIdentity.getUserAccount();
            for (String manifestFilename : manifestFilenames) {
                log.info("{}: Batch Cleanup ARM Response Files - about to process manifest filename {}, row {} of {} rows", loggingPrefix,
                         manifestFilename, counter++,
                         manifestFilenames.size());
                cleanupFilesByManifestFilename(userAccount, EodHelper.armLocation(), statusToSearch, dateTimeForDeletion, manifestFilename);
            }
        } else {
            log.info("{}: No ARM responses found to be deleted", loggingPrefix);
        }
    }

    protected OffsetDateTime getDateTimeForDeletion() {
        Integer cleanupBufferMinutes = batchCleanupConfiguration.getBufferMinutes();
        return currentTimeHelper.currentOffsetDateTime().minusMinutes(cleanupBufferMinutes);
    }

    protected List<ObjectRecordStatusEntity> getStatusToSearch() {
        return objectRecordStatusRepository.getReferencesByStatus(
            List.of(STORED, ARM_RPO_PENDING, ARM_RESPONSE_PROCESSING_FAILED, ARM_RESPONSE_MANIFEST_FAILED, ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED));
    }

    protected List<String> getManifestFileNames(int batchsize) {
        return externalObjectDirectoryRepository.findBatchCleanupManifestFilenames(
            getStatusToSearch(),
            EodHelper.armLocation(),
            false,
            getDateTimeForDeletion(),
            manifestFilePrefix,
            Limit.of(batchsize)
        );
    }

    private void cleanupFilesByManifestFilename(UserAccountEntity userAccount, ExternalLocationTypeEntity armLocation,
                                                List<ObjectRecordStatusEntity> statusToSearch,
                                                OffsetDateTime dateTimeForDeletion, String manifestFilename) {
        try {
            log.debug("{}: Finding associated eodEntries for manifest filename {}", loggingPrefix, manifestFilename);
            List<ExternalObjectDirectoryEntity> eodEntriesWithManifestFilename = externalObjectDirectoryRepository.findBatchCleanupEntriesByManifestFilename(
                armLocation, false, manifestFilename);

            List<Integer> statusIdList = statusToSearch.stream().map(ObjectRecordStatusEntity::getId).toList();
            boolean statusAllValid = eodEntriesWithManifestFilename.stream().allMatch(eodEntity -> statusIdList.contains(eodEntity.getStatusId()));
            if (!statusAllValid) {
                log.warn("{}: Not all statuses are valid for manifestFilename {}, so skipping to next manifestFilename.", loggingPrefix, manifestFilename);
            }

            boolean lastModifiedAllValid = eodEntriesWithManifestFilename.stream().allMatch(
                eodEntity -> eodEntity.getLastModifiedDateTime().isBefore(dateTimeForDeletion));
            if (!lastModifiedAllValid) {
                log.warn("{}: Not all entries have been modified before {} for manifestFilename {}, so skipping to next manifestFilename.",
                         loggingPrefix, dateTimeForDeletion, manifestFilename);
            }

            log.debug("{}: Found ARM manifest file {} for cleanup", loggingPrefix, manifestFilename);
            List<InputUploadAndAssociatedFilenames> inputUploadAndAssociatedList = armResponseFileHelper.getCorrespondingArmFilesForManifestFilename(
                manifestFilePrefix, manifestFilename);
            //inputUploadAndAssociatedList should only contain 1 matching InputUpload file, but looping through it just in case.
            for (InputUploadAndAssociatedFilenames inputUploadAndAssociates : inputUploadAndAssociatedList) {
                deleteResponseFiles(userAccount, inputUploadAndAssociates, eodEntriesWithManifestFilename);
            }

        } catch (UnableToReadArmFileException e) {
            log.error("{}: Cannot process manifest filename {} due to corrupt ARM file.", loggingPrefix, manifestFilename);
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
            log.info("{}: There are {} response files for EOD {}, linked to inputUpload filename {}",
                     loggingPrefix, associatedFiles.size(), eodId, inputUploadFilename);

            for (String associatedFile : associatedFiles) {
                try {
                    log.info("{}: About to delete file {} for EOD {}, linked to inputUpload filename {}", loggingPrefix, associatedFile, eodId,
                             inputUploadFilename);
                    boolean responseFileDeletedSuccessfully = armDataManagementApi.deleteBlobData(associatedFile);
                    if (!responseFileDeletedSuccessfully) {
                        log.warn("{}: Response file {} failed to delete successfully.", loggingPrefix, associatedFile);
                        successfullyDeletedAssociatedFiles = false;
                    }
                } catch (Exception e) {
                    log.error("{}: Failure to delete response file {} for EOD {} - {}", loggingPrefix, associatedFile, eodId, e.getMessage(), e);
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
                    log.error("{}: EodEntity {} in response file for {} cannot be found.", loggingPrefix, eodId, inputUploadFilename);
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
                             loggingPrefix, inputUploadFilename, eodEntityFromRelationId);
                }
                setResponseCleaned(userAccount, eodEntityFromRelationId);
            }
        }

        if (CollectionUtils.isNotEmpty(eodEntriesWithManifestFilename)) {
            log.warn("{}: All ARM response files related to InputUpload file {} have been deleted, but none referred to the following eodId's - {}.",
                     loggingPrefix, inputUploadFilename, eodEntriesWithManifestFilename.stream().map(ExternalObjectDirectoryEntity::getId).toList());
        }

        deleteInputUploadFile(deletedFileStatuses, inputUploadFilename);
    }

    private void deleteInputUploadFile(List<Boolean> deletedFileStatuses, String inputUploadFilename) {
        if (deletedFileStatuses.stream().allMatch(Boolean.TRUE::equals)) {
            log.info("{}: All associated Eod entries deleted, about to delete InputUpload file {}", loggingPrefix, inputUploadFilename);
            // Make sure to only delete the Input Upload filename after the other response files have been deleted as once this is deleted
            // you cannot find the other response files
            boolean inputUploadFileDeletedSuccessfully = armDataManagementApi.deleteBlobData(inputUploadFilename);
            if (inputUploadFileDeletedSuccessfully) {
                log.info("{}: Successfully cleaned up response files for InputUpload file {}", loggingPrefix, inputUploadFilename);
            } else {
                log.warn("{}: Unable to delete input upload response file {}", loggingPrefix, inputUploadFilename);
            }
        } else {
            log.warn("{}: Unable to delete all response files for InputUpload file {}", loggingPrefix, inputUploadFilename);
        }
    }

    protected void setResponseCleaned(UserAccountEntity userAccount, ExternalObjectDirectoryEntity externalObjectDirectory) {
        externalObjectDirectory.setResponseCleaned(true);
        externalObjectDirectory.setLastModifiedBy(userAccount);
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
    }
}