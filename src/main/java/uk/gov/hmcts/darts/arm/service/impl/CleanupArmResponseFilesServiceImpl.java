package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.CleanupArmResponseFilesService;
import uk.gov.hmcts.darts.arm.util.ArmResponseFilesHelper;
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
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_INVALID_FILE_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_UPLOAD_FILE_FILENAME_KEY;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_MANIFEST_FILE_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_RESPONSE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_RESPONSE_PROCESSING;
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

    private ObjectRecordStatusEntity storedStatus;
    private ObjectRecordStatusEntity failedArmManifestFileStatus;
    private ObjectRecordStatusEntity failedArmResponseProcessing;
    private ObjectRecordStatusEntity failedArmResponseChecksum;
    private UserAccountEntity userAccount;


    @Override
    public void cleanupResponseFiles() {
        initialisePreloadedObjects();

        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());
        List<ObjectRecordStatusEntity> statuses = List.of(storedStatus,
                                                          failedArmManifestFileStatus,
                                                          failedArmResponseProcessing,
                                                          failedArmResponseChecksum);

        OffsetDateTime dateTimeForDeletion = currentTimeHelper.currentOffsetDateTime().minusDays(
            armDataManagementConfiguration.getResponseCleanupBufferDays());

        List<ExternalObjectDirectoryEntity> objectDirectoryDataToBeDeleted =
            externalObjectDirectoryRepository.findByStatusInAndStorageLocationAndResponseCleanedAndLastModifiedDateTimeBefore(
                statuses,
                armLocation,
                false,
                dateTimeForDeletion
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
            log.info("About to look for files to cleanup starting with prefix: {}", prefix);
            inputUploadBlobs = armDataManagementApi.listResponseBlobs(prefix);
        } catch (Exception e) {
            log.error("Unable to cleanup response files for prefix: {} - {}", prefix, e.getMessage());
        }
        if (CollectionUtils.isNotEmpty(inputUploadBlobs)) {
            for (String armInputUploadFilename : inputUploadBlobs) {
                log.debug("Found ARM input upload file {} for cleanup", armInputUploadFilename);
                if (armInputUploadFilename.endsWith(ArmResponseFilesHelper.generateSuffix(ARM_INPUT_UPLOAD_FILENAME_KEY))) {
                    readInputUploadFile(externalObjectDirectory, armInputUploadFilename);
                } else {
                    log.warn("ARM cleanup file {} not input upload file for EOD {}", armInputUploadFilename, externalObjectDirectory.getId());
                }
            }
        } else {
            //Assume if no IU file found then it has already been cleaned up
            log.info("Unable to find input upload file with prefix {} for cleanup", prefix);
            externalObjectDirectory.setResponseCleaned(true);
            updateExternalObjectDirectory(externalObjectDirectory);
        }
    }

    private void readInputUploadFile(ExternalObjectDirectoryEntity externalObjectDirectory, String armInputUploadFilename) {
        try {
            InputUploadFilenameProcessor inputUploadFilenameProcessor = new InputUploadFilenameProcessor(armInputUploadFilename);
            String responseFilesHashcode = inputUploadFilenameProcessor.getHashcode();
            log.debug("List response files starting with hashcode {}", responseFilesHashcode);
            List<String> responseBlobs = armDataManagementApi.listResponseBlobs(responseFilesHashcode);
            if (CollectionUtils.isNotEmpty(responseBlobs)) {
                processResponseBlobs(responseBlobs, externalObjectDirectory, armInputUploadFilename);
            } else {
                log.warn("Unable to find input upload response file {} for {}", armInputUploadFilename, externalObjectDirectory.getId());
                //Assume if no other response files found then they have already been cleaned up
                externalObjectDirectory.setResponseCleaned(true);
                updateExternalObjectDirectory(externalObjectDirectory);
            }
        } catch (IllegalArgumentException e) {
            // This occurs when the filename is not parsable
            log.error("Unable to process filename: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Unable to list responses: {}", e.getMessage());
        }
    }

    private void processResponseBlobs(List<String> responseBlobs, ExternalObjectDirectoryEntity externalObjectDirectory,
                                      String armInputUploadFilename) {
        String createRecordFilename = null;
        String uploadFilename = null;
        String invalidFileFilename = null;

        for (String responseFile : responseBlobs) {
            if (responseFile.endsWith(ArmResponseFilesHelper.generateSuffix(ARM_CREATE_RECORD_FILENAME_KEY))) {
                createRecordFilename = responseFile;
            } else if (responseFile.endsWith(ArmResponseFilesHelper.generateSuffix(ARM_UPLOAD_FILE_FILENAME_KEY))) {
                uploadFilename = responseFile;
            } else if (responseFile.endsWith(ArmResponseFilesHelper.generateSuffix(ARM_INVALID_FILE_FILENAME_KEY))) {
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
            log.error("Failure to cleanup response files {}", e.getMessage(), e);
        }
    }

    private void deleteResponseFiles(ExternalObjectDirectoryEntity externalObjectDirectory, String armInputUploadFilename) {
        List<Boolean> deletedFileStatuses = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(armResponseFilesToBeDeleted)) {
            for (String responseFile : armResponseFilesToBeDeleted) {
                try {
                    log.info("About to delete {} for EOD {}", responseFile, externalObjectDirectory.getId());
                    deletedFileStatuses.add(armDataManagementApi.deleteBlobData(responseFile));
                } catch (Exception e) {
                    log.error("Failure to delete response file {} - {}", responseFile, e.getMessage(), e);
                    deletedFileStatuses.add(false);
                }
            }

        }
        if (deletedFileStatuses.isEmpty() || deletedFileStatuses.stream().allMatch(Boolean.TRUE::equals)) {
            log.info("About to delete {} for EOD {}", armInputUploadFilename, externalObjectDirectory.getId());
            // Make sure to only delete the Input Upload filename last as once this is deleted you cannot find the other response files
            boolean deletedInputUploadFile = deletedFileStatuses.add(armDataManagementApi.deleteBlobData(armInputUploadFilename));
            if (deletedInputUploadFile) {
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
            externalObjectDirectory.setResponseCleaned(false);
            updateExternalObjectDirectory(externalObjectDirectory);
        }
        armResponseFilesToBeDeleted.clear();

    }

    private void updateExternalObjectDirectory(ExternalObjectDirectoryEntity externalObjectDirectory) {
        externalObjectDirectory.setLastModifiedBy(userAccount);
        externalObjectDirectory.setLastModifiedDateTime(OffsetDateTime.now());
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
    }


    @SuppressWarnings("java:S3655")
    private void initialisePreloadedObjects() {
        storedStatus = objectRecordStatusRepository.findById(STORED.getId()).get();
        failedArmManifestFileStatus = objectRecordStatusRepository.findById(FAILURE_ARM_MANIFEST_FILE_FAILED.getId()).get();
        failedArmResponseProcessing = objectRecordStatusRepository.findById(FAILURE_ARM_RESPONSE_PROCESSING.getId()).get();
        failedArmResponseChecksum = objectRecordStatusRepository.findById(FAILURE_ARM_RESPONSE_CHECKSUM_FAILED.getId()).get();

        userAccount = userIdentity.getUserAccount();
    }

    public static String getPrefix(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return new StringBuilder(externalObjectDirectory.getId().toString())
            .append(ARM_FILENAME_SEPARATOR)
            .append(ArmResponseFilesHelper.getObjectTypeId(externalObjectDirectory))
            .append(ARM_FILENAME_SEPARATOR).toString();
    }
}
