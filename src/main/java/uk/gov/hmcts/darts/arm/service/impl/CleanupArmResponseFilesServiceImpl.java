package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.CleanupArmResponseFilesService;
import uk.gov.hmcts.darts.arm.util.ArmResponseFilesUtil;
import uk.gov.hmcts.darts.arm.util.files.InputUploadFilenameProcessor;
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

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_CREATE_RECORD_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_INPUT_UPLOAD_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_INVALID_LINE_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_UPLOAD_FILE_FILENAME_KEY;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupArmResponseFilesServiceImpl implements CleanupArmResponseFilesService {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;

    private final ArmDataManagementApi armDataManagementApi;
    private final UserIdentity userIdentity;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final CurrentTimeHelper currentTimeHelper;

    private final List<String> armResponseFilesToBeDeleted = new ArrayList<>();

    private UserAccountEntity userAccount;


    @Override
    public void cleanupResponseFiles() {
        ObjectRecordStatusEntity storedStatus = objectRecordStatusRepository.getReferenceById(STORED.getId());
        ObjectRecordStatusEntity armRpoPendingStatus = objectRecordStatusRepository.getReferenceById(ARM_RPO_PENDING.getId());
        ObjectRecordStatusEntity failedArmResponseManifestFileStatus = objectRecordStatusRepository.getReferenceById(ARM_RESPONSE_MANIFEST_FAILED.getId());
        ObjectRecordStatusEntity failedArmResponseProcessing = objectRecordStatusRepository.getReferenceById(ARM_RESPONSE_PROCESSING_FAILED.getId());
        ObjectRecordStatusEntity failedArmResponseChecksum = objectRecordStatusRepository.getReferenceById(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId());

        userAccount = userIdentity.getUserAccount();

        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());
        List<ObjectRecordStatusEntity> statuses = List.of(storedStatus,
                                                          armRpoPendingStatus,
                                                          failedArmResponseManifestFileStatus,
                                                          failedArmResponseProcessing,
                                                          failedArmResponseChecksum);

        OffsetDateTime dateTimeForDeletion = currentTimeHelper.currentOffsetDateTime().minusDays(
            armDataManagementConfiguration.getResponseCleanupBufferDays());

        List<ExternalObjectDirectoryEntity> objectDirectoryDataToBeDeleted =
            externalObjectDirectoryRepository.findSingleArmResponseFiles(
                statuses,
                armLocation,
                false,
                dateTimeForDeletion,
                armDataManagementConfiguration.getManifestFilePrefix()
            );

        if (CollectionUtils.isNotEmpty(objectDirectoryDataToBeDeleted)) {
            int row = 1;
            for (ExternalObjectDirectoryEntity externalObjectDirectory : objectDirectoryDataToBeDeleted) {
                log.info("Cleanup ARM Response Files about to process {} of {} rows", row++, objectDirectoryDataToBeDeleted.size());
                cleanupResponseFilesForExternalObjectDirectory(externalObjectDirectory);
            }
        } else {
            log.info("No ARM responses found to be deleted}");
        }
    }

    private void cleanupResponseFilesForExternalObjectDirectory(ExternalObjectDirectoryEntity externalObjectDirectory) {
        armResponseFilesToBeDeleted.clear();
        String prefix = getPrefix(externalObjectDirectory);
        List<String> inputUploadBlobs = null;
        try {
            log.info("About to look for response files to cleanup, starting with prefix {}", prefix);
            inputUploadBlobs = armDataManagementApi.listResponseBlobs(prefix);
        } catch (Exception e) {
            log.error("Unable to cleanup response files for prefix {} with EOD {}", prefix, e.getMessage());
        }
        if (CollectionUtils.isNotEmpty(inputUploadBlobs)) {
            for (String armInputUploadFilename : inputUploadBlobs) {
                log.debug("Found ARM input upload file {} for cleanup", armInputUploadFilename);
                if (armInputUploadFilename.endsWith(ArmResponseFilesUtil.generateSuffix(ARM_INPUT_UPLOAD_FILENAME_KEY))) {
                    processInputUploadFile(externalObjectDirectory, armInputUploadFilename);
                } else {
                    log.warn("ARM response file {} not input upload file for EOD {}", armInputUploadFilename, externalObjectDirectory.getId());
                }
            }
        } else {
            log.info("Unable to find input upload file with prefix {} for cleanup with EOD {}", prefix, externalObjectDirectory.getId());
        }
    }

    private void processInputUploadFile(ExternalObjectDirectoryEntity externalObjectDirectory, String armInputUploadFilename) {
        try {
            InputUploadFilenameProcessor inputUploadFilenameProcessor = new InputUploadFilenameProcessor(armInputUploadFilename);
            String responseFilesHashcode = inputUploadFilenameProcessor.getHashcode();
            log.debug("List response files starting with hashcode {}", responseFilesHashcode);
            List<String> responseBlobs = armDataManagementApi.listResponseBlobs(responseFilesHashcode);
            if (CollectionUtils.isNotEmpty(responseBlobs)) {
                processResponseBlobsForDeletion(responseBlobs, externalObjectDirectory, armInputUploadFilename);
            } else {
                log.warn("Unable to find responses files from Input Upload file {} for EOD {}", armInputUploadFilename, externalObjectDirectory.getId());
            }
        } catch (IllegalArgumentException e) {
            // This occurs when the filename is not parsable
            log.error("Unable to process filename {} for {} - {}", armInputUploadFilename, externalObjectDirectory.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Unable to list responses for file {} for {} - {}", armInputUploadFilename, externalObjectDirectory.getId(), e.getMessage());
        }
    }

    private void processResponseBlobsForDeletion(List<String> responseBlobs, ExternalObjectDirectoryEntity externalObjectDirectory,
                                                 String armInputUploadFilename) {
        String createRecordFilename = null;
        String uploadFilename = null;
        String invalidFileFilename = null;

        for (String responseFile : responseBlobs) {
            if (responseFile.endsWith(ArmResponseFilesUtil.generateSuffix(ARM_CREATE_RECORD_FILENAME_KEY))) {
                createRecordFilename = responseFile;
            } else if (responseFile.endsWith(ArmResponseFilesUtil.generateSuffix(ARM_UPLOAD_FILE_FILENAME_KEY))) {
                uploadFilename = responseFile;
            } else if (responseFile.endsWith(ArmResponseFilesUtil.generateSuffix(ARM_INVALID_LINE_FILENAME_KEY))) {
                invalidFileFilename = responseFile;
            }
        }

        try {
            if (nonNull(createRecordFilename)) {
                armResponseFilesToBeDeleted.add(createRecordFilename);
            }
            if (nonNull(uploadFilename)) {
                armResponseFilesToBeDeleted.add(uploadFilename);
            }
            if (nonNull(invalidFileFilename)) {
                armResponseFilesToBeDeleted.add(invalidFileFilename);
            }
            deleteResponseFiles(externalObjectDirectory, armInputUploadFilename);

        } catch (Exception e) {
            log.error("Failure to cleanup response files for EOD {} - {}", externalObjectDirectory.getId(), e.getMessage());
        }
    }

    private void deleteResponseFiles(ExternalObjectDirectoryEntity externalObjectDirectory, String armInputUploadFilename) {
        List<Boolean> deletedFileStatuses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(armResponseFilesToBeDeleted)) {
            for (String responseFile : armResponseFilesToBeDeleted) {
                try {
                    log.info("About to delete file {} for EOD {}", responseFile, externalObjectDirectory.getId());
                    boolean deletedResponseFile = armDataManagementApi.deleteBlobData(responseFile);
                    deletedFileStatuses.add(deletedResponseFile);
                    if (!deletedResponseFile) {
                        break;
                    }
                } catch (Exception e) {
                    log.error("Failure to delete response file {} for EOD {} - {}", responseFile, externalObjectDirectory.getId(), e.getMessage(), e);
                    deletedFileStatuses.add(false);
                }
            }
        }
        if (deletedFileStatuses.stream().allMatch(Boolean.TRUE::equals)) {
            log.info("About to delete {} for EOD {}", armInputUploadFilename, externalObjectDirectory.getId());
            // Make sure to only delete the Input Upload filename after the other response files have been deleted as once this is deleted
            // you cannot find the other response files
            boolean deletedInputUploadFile = armDataManagementApi.deleteBlobData(armInputUploadFilename);
            if (deletedInputUploadFile) {
                externalObjectDirectory.setResponseCleaned(true);
                updateExternalObjectDirectory(externalObjectDirectory);
                log.info("Successfully cleaned up response files for EOD {}", externalObjectDirectory.getId());
            } else {
                log.warn("Unable to delete input upload response file for EOD {}", externalObjectDirectory.getId());
            }
        } else {
            log.warn("Unable to delete all response files for EOD {}", externalObjectDirectory.getId());
        }
        armResponseFilesToBeDeleted.clear();
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