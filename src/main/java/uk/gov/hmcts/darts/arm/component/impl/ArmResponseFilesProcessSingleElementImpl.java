package uk.gov.hmcts.darts.arm.component.impl;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInvalidLineRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.util.ArmResponseFilesUtil;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.InputUploadFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.InvalidLineFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_CREATE_RECORD_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_INPUT_UPLOAD_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_INVALID_LINE_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_RESPONSE_FILE_EXTENSION;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_RESPONSE_INVALID_STATUS_CODE;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_RESPONSE_SUCCESS_STATUS_CODE;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_UPLOAD_FILE_FILENAME_KEY;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmResponseFilesProcessSingleElementImpl implements ArmResponseFilesProcessSingleElement {


    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ArmDataManagementApi armDataManagementApi;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ObjectMapper objectMapper;
    private final UserIdentity userIdentity;
    private final LogApi logApi;

    private ObjectRecordStatusEntity armDropZoneStatus;
    private ObjectRecordStatusEntity armProcessingResponseFilesStatus;
    private ObjectRecordStatusEntity armResponseProcessingFailedStatus;
    private ObjectRecordStatusEntity armResponseManifestFailedStatus;
    private ObjectRecordStatusEntity storedStatus;
    private ObjectRecordStatusEntity armRpoPendingStatus;
    private ObjectRecordStatusEntity armResponseChecksumVerificationFailedStatus;
    private UserAccountEntity userAccount;

    @PostConstruct
    public void initialisePreloadedObjects() {
        storedStatus = objectRecordStatusRepository.findById(STORED.getId()).orElseThrow();
        armRpoPendingStatus = objectRecordStatusRepository.findById(ARM_RPO_PENDING.getId()).orElseThrow();
        armDropZoneStatus = objectRecordStatusRepository.findById(ARM_DROP_ZONE.getId()).orElseThrow();
        armProcessingResponseFilesStatus = objectRecordStatusRepository.findById(ARM_PROCESSING_RESPONSE_FILES.getId()).orElseThrow();
        armResponseManifestFailedStatus = objectRecordStatusRepository.findById(ARM_RESPONSE_MANIFEST_FAILED.getId()).orElseThrow();
        armResponseProcessingFailedStatus = objectRecordStatusRepository.findById(ARM_RESPONSE_PROCESSING_FAILED.getId()).orElseThrow();
        armResponseChecksumVerificationFailedStatus = objectRecordStatusRepository.findById(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId()).orElseThrow();
    }

    @Transactional
    @Override
    public void processResponseFilesFor(Integer externalObjectDirectoryId) {
        userAccount = userIdentity.getUserAccount();
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = externalObjectDirectoryRepository.findById(externalObjectDirectoryId).get();
        try {
            processInputUploadFile(externalObjectDirectoryEntity);
        } catch (Exception e) {
            log.error("Unable to process response files for external object directory.", e);
            updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectoryEntity, armDropZoneStatus);
        }
    }

    private void processInputUploadFile(ExternalObjectDirectoryEntity externalObjectDirectory) {
        // Search for Input Upload Files starting with the ExternalObjectDirectoryEntity id (EODID), document object id eg. media id and transfer attempts
        // - Expected filename: EODID_ObjectID_TransferAttempts_Hashcode_Status_iu.rsp
        String prefix = ArmResponseFilesUtil.getPrefix(externalObjectDirectory);
        List<String> inputUploadBlobs = null;
        try {
            log.info("About to look for files starting with prefix: {}", prefix);
            inputUploadBlobs = armDataManagementApi.listResponseBlobs(prefix);
        } catch (Exception e) {
            updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
            log.error("Unable to find response file for prefix: {} - {}", prefix, e.getMessage());
        }
        if (CollectionUtils.isNotEmpty(inputUploadBlobs)) {
            for (String armInputUploadFilename : inputUploadBlobs) {
                log.debug("Found ARM input upload file {}", armInputUploadFilename);
                if (armInputUploadFilename.endsWith(generateSuffix(ARM_INPUT_UPLOAD_FILENAME_KEY))) {
                    readInputUploadFile(externalObjectDirectory, armInputUploadFilename, armDropZoneStatus);
                    break;
                } else {
                    log.warn("ARM file {} not input upload file", armInputUploadFilename);
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, armDropZoneStatus);
                }
            }
        } else {
            log.info("Unable to find input upload file with prefix {}", prefix);
            ExternalObjectDirectoryEntity latestEod = externalObjectDirectoryRepository.findById(externalObjectDirectory.getId()).get();

            if (armProcessingResponseFilesStatus.equals(latestEod.getStatus())) {
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armDropZoneStatus);
            }
        }
    }

    private void readInputUploadFile(ExternalObjectDirectoryEntity externalObjectDirectory, String armInputUploadFilename,
                                     ObjectRecordStatusEntity armDropZoneStatus) {
        try {
            InputUploadFilenameProcessor inputUploadFilenameProcessor = new InputUploadFilenameProcessor(armInputUploadFilename);
            String responseFilesHashcode = inputUploadFilenameProcessor.getHashcode();
            log.debug("List response files starting with hashcode {}", responseFilesHashcode);
            List<String> responseBlobs = armDataManagementApi.listResponseBlobs(responseFilesHashcode);
            if (CollectionUtils.isNotEmpty(responseBlobs)) {
                processResponseBlobs(armInputUploadFilename, responseBlobs, externalObjectDirectory);
            } else {
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armDropZoneStatus);
            }
        } catch (IllegalArgumentException e) {
            // This occurs when the filename is not parsable
            log.error("Unable to process filename: {}", e.getMessage());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
        } catch (Exception e) {
            log.error("Unable to list responses: {}", e.getMessage());
            updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
        }
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private void processResponseBlobs(String armInputUploadFilename, List<String> responseBlobs, ExternalObjectDirectoryEntity externalObjectDirectory) {
        String createRecordFilename = null;
        String uploadFilename = null;
        String invalidLineFilename = null;

        for (String responseFile : responseBlobs) {
            if (responseFile.endsWith(generateSuffix(ARM_CREATE_RECORD_FILENAME_KEY))) {
                createRecordFilename = responseFile;
            } else if (responseFile.endsWith(generateSuffix(ARM_UPLOAD_FILE_FILENAME_KEY))) {
                uploadFilename = responseFile;
            } else if (responseFile.endsWith(generateSuffix(ARM_INVALID_LINE_FILENAME_KEY))) {
                invalidLineFilename = responseFile;
            }
        }

        if (nonNull(invalidLineFilename) && (nonNull(createRecordFilename) || nonNull(uploadFilename))) {
            processInvalidLineFile(armInputUploadFilename, responseBlobs, externalObjectDirectory,
                                   invalidLineFilename, createRecordFilename, uploadFilename);
        } else if (nonNull(createRecordFilename) && nonNull(uploadFilename)) {
            processUploadFile(armInputUploadFilename, responseBlobs, externalObjectDirectory, uploadFilename, createRecordFilename);
        } else {
            log.info("Unable to find response files for external object {}", externalObjectDirectory.getId());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armDropZoneStatus);
        }
    }

    private void processInvalidLineFile(String armInputUploadFilename, List<String> responseBlobs, ExternalObjectDirectoryEntity externalObjectDirectory,
                                        String invalidLineFilename, String createRecordFilename, String uploadFilename) {
        try {
            getAndLogCreateRecordFile(externalObjectDirectory, createRecordFilename);
            getAndLogUploadFile(externalObjectDirectory, uploadFilename);

            BinaryData invalidLineFileBinary = armDataManagementApi.getBlobData(invalidLineFilename);

            InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor = new InvalidLineFileFilenameProcessor(invalidLineFilename);
            ObjectRecordStatusEnum status = readInvalidLineFile(externalObjectDirectory, invalidLineFileBinary, invalidLineFileFilenameProcessor);
            if (ARM_RESPONSE_PROCESSING_FAILED.equals(status)
                || ARM_RESPONSE_MANIFEST_FAILED.equals(status)) {
                deleteResponseBlobs(armInputUploadFilename, responseBlobs, externalObjectDirectory);
            }
        } catch (IllegalArgumentException e) {
            // This occurs when the filename is not parsable
            log.error("Unable to process invalid line file: {} {}", invalidLineFilename, e.getMessage());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
        } catch (Exception e) {
            log.error("Failure with invalid line file: {}", invalidLineFilename, e);
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
        }
    }

    private void processUploadFile(String armInputUploadFilename, List<String> responseBlobs, ExternalObjectDirectoryEntity externalObjectDirectory,
                                   String uploadFilename, String createRecordFilename) {
        try {
            getAndLogCreateRecordFile(externalObjectDirectory, createRecordFilename);
            UploadFileFilenameProcessor uploadFileFilenameProcessor = new UploadFileFilenameProcessor(uploadFilename);

            BinaryData uploadFileBinary = armDataManagementApi.getBlobData(uploadFilename);
            ObjectRecordStatusEnum status = readUploadFile(externalObjectDirectory, uploadFileBinary, uploadFileFilenameProcessor);
            if (STORED.equals(status)
                || ARM_RPO_PENDING.equals(status)
                || ARM_RESPONSE_PROCESSING_FAILED.equals(status)
                || ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.equals(status)) {
                log.info("About to delete blob responses for EOD {}", externalObjectDirectory.getId());
                deleteResponseBlobs(armInputUploadFilename, responseBlobs, externalObjectDirectory);
            }
        } catch (IllegalArgumentException e) {
            // This occurs when the filename is not parsable
            log.error("Unable to process filename: {}", e.getMessage());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
        } catch (Exception e) {
            log.error("Failed to get upload file {}", uploadFilename, e);
            updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
        }
    }

    private void getAndLogCreateRecordFile(ExternalObjectDirectoryEntity externalObjectDirectory, String createRecordFilename) {
        if (nonNull(createRecordFilename)) {
            Path jsonPath = null;
            try {
                CreateRecordFilenameProcessor createRecordFilenameProcessor = new CreateRecordFilenameProcessor(createRecordFilename);
                BinaryData createRecordFileBinary = armDataManagementApi.getBlobData(createRecordFilenameProcessor.getCreateRecordFilenameAndPath());
                if (nonNull(createRecordFileBinary)) {
                    boolean appendUuidToWorkspace = true;
                    jsonPath = fileOperationService.saveBinaryDataToSpecifiedWorkspace(
                        createRecordFileBinary,
                        createRecordFilenameProcessor.getCreateRecordFilename(),
                        armDataManagementConfiguration.getTempBlobWorkspace(),
                        appendUuidToWorkspace
                    );

                    if (nonNull(jsonPath) && jsonPath.toFile().exists()) {
                        logResponseFileContents(jsonPath);
                    }
                }
            } catch (Exception e) {
                log.error("Unable to read create record file {} for EOD {}", createRecordFilename, externalObjectDirectory.getId(), e);
            } finally {
                cleanupTemporaryJsonFile(jsonPath);
            }
        }
    }

    private void getAndLogUploadFile(ExternalObjectDirectoryEntity externalObjectDirectory, String uploadFileFilename) {
        if (nonNull(uploadFileFilename)) {
            Path jsonPath = null;
            try {
                UploadFileFilenameProcessor uploadFileFilenameProcessor = new UploadFileFilenameProcessor(uploadFileFilename);
                BinaryData uploadFileBinary = armDataManagementApi.getBlobData(uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
                if (nonNull(uploadFileBinary)) {
                    boolean appendUuidToWorkspace = true;
                    jsonPath = fileOperationService.saveBinaryDataToSpecifiedWorkspace(
                        uploadFileBinary,
                        uploadFileFilenameProcessor.getUploadFileFilename(),
                        armDataManagementConfiguration.getTempBlobWorkspace(),
                        appendUuidToWorkspace
                    );

                    if (nonNull(jsonPath) && jsonPath.toFile().exists()) {
                        logResponseFileContents(jsonPath);
                    }
                }
            } catch (Exception e) {
                log.error("Unable to read create record file {} for EOD {}", uploadFileFilename, externalObjectDirectory.getId(), e);
            } finally {
                cleanupTemporaryJsonFile(jsonPath);
            }
        }
    }

    private void logResponseFileContents(Path jsonPath) {
        try {
            String contents = FileUtils.readFileToString(jsonPath.toFile(), UTF_8);
            log.info("Contents of ARM response file {} - \n{}",
                     jsonPath.toFile().getAbsoluteFile(),
                     contents);
        } catch (Exception e) {
            log.error("Unable to read ARM response file {}", jsonPath.toFile().getAbsoluteFile(), e);
        }
    }

    void deleteResponseBlobs(String armInputUploadFilename, List<String> responseBlobs, ExternalObjectDirectoryEntity externalObjectDirectory) {
        List<Boolean> deletedResponseBlobStatuses;
        deletedResponseBlobStatuses = responseBlobs.stream()
            .map(armDataManagementApi::deleteBlobData)
            .toList();
        if (deletedResponseBlobStatuses.size() == 2 && !deletedResponseBlobStatuses.contains(false)) {
            externalObjectDirectory.setResponseCleaned(
                armDataManagementApi.deleteBlobData(armInputUploadFilename));
        } else {
            log.warn("Unable to successfully delete the response files for EOD {} ", externalObjectDirectory.getId());
        }
    }

    private ObjectRecordStatusEnum readUploadFile(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                  BinaryData uploadFileBinary,
                                                  UploadFileFilenameProcessor uploadFileFilenameProcessor) {
        ObjectRecordStatusEnum objectRecordStatusEnum;

        if (nonNull(uploadFileBinary)) {
            Path jsonPath = null;
            try {
                boolean appendUuidToWorkspace = true;
                jsonPath = fileOperationService.saveBinaryDataToSpecifiedWorkspace(
                    uploadFileBinary,
                    uploadFileFilenameProcessor.getUploadFileFilename(),
                    armDataManagementConfiguration.getTempBlobWorkspace(),
                    appendUuidToWorkspace
                );

                if (nonNull(jsonPath) && jsonPath.toFile().exists()) {
                    logResponseFileContents(jsonPath);
                    ArmResponseUploadFileRecord armResponseUploadFileRecord = objectMapper.readValue(jsonPath.toFile(), ArmResponseUploadFileRecord.class);
                    objectRecordStatusEnum = processUploadFileObject(externalObjectDirectory, uploadFileFilenameProcessor, armResponseUploadFileRecord);
                } else {
                    log.warn("Failed to write upload file to temp workspace {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
                    objectRecordStatusEnum = updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
                }
            } catch (IOException e) {
                log.error("Unable to write upload file to temporary workspace {} - {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath(),
                          e.getMessage());
                objectRecordStatusEnum = updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
            } catch (Exception e) {
                log.error("Unable to process arm response upload file {} - {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath(), e.getMessage());
                objectRecordStatusEnum = ObjectRecordStatusEnum.valueOfId(
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus).getId());
            } finally {
                cleanupTemporaryJsonFile(jsonPath);
            }
        } else {
            objectRecordStatusEnum = updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
        }
        return objectRecordStatusEnum;
    }

    private ObjectRecordStatusEnum readInvalidLineFile(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                       BinaryData invalidLineFileBinary,
                                                       InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor) {
        ObjectRecordStatusEnum objectRecordStatusEnum;

        if (nonNull(invalidLineFileBinary)) {
            Path jsonPath = null;
            try {
                boolean appendUuidToWorkspace = true;
                jsonPath = fileOperationService.saveBinaryDataToSpecifiedWorkspace(
                    invalidLineFileBinary,
                    invalidLineFileFilenameProcessor.getInvalidLineFilename(),
                    armDataManagementConfiguration.getTempBlobWorkspace(),
                    appendUuidToWorkspace
                );

                if (jsonPath.toFile().exists()) {
                    logResponseFileContents(jsonPath);
                    ArmResponseInvalidLineRecord armResponseInvalidLineRecord = objectMapper.readValue(jsonPath.toFile(), ArmResponseInvalidLineRecord.class);
                    objectRecordStatusEnum = processInvalidLineFileObject(externalObjectDirectory, invalidLineFileFilenameProcessor,
                                                                          armResponseInvalidLineRecord);
                } else {
                    log.warn("Failed to write invalid line file to temp workspace {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath());
                    objectRecordStatusEnum = updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory,
                                                                                                       armResponseProcessingFailedStatus);
                }
            } catch (IOException e) {
                log.error("Unable to write invalid line file to temporary workspace {} - {}",
                          invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath(), e.getMessage());
                objectRecordStatusEnum = updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory,
                                                                                                   armResponseProcessingFailedStatus);
            } catch (Exception e) {
                log.error("Unable to process arm response invalid line file {} - {}",
                          invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath(), e.getMessage());
                objectRecordStatusEnum = ObjectRecordStatusEnum.valueOfId(
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus).getId());
            } finally {
                cleanupTemporaryJsonFile(jsonPath);
            }
        } else {
            objectRecordStatusEnum = updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armResponseProcessingFailedStatus);
        }
        return objectRecordStatusEnum;
    }

    @SuppressWarnings("PMD.AvoidReassigningParameters")
    private ObjectRecordStatusEnum processUploadFileObject(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                           UploadFileFilenameProcessor uploadFileFilenameProcessor,
                                                           ArmResponseUploadFileRecord armResponseUploadFileRecord) {
        if (nonNull(armResponseUploadFileRecord)) {
            //If the filename contains 1
            if (ARM_RESPONSE_SUCCESS_STATUS_CODE.equals(uploadFileFilenameProcessor.getStatus())) {
                processUploadFileDataSuccess(armResponseUploadFileRecord, externalObjectDirectory);
            } else {
                //Read the upload file and log the error code and description with EOD
                String errorDescription = org.apache.commons.lang3.StringUtils.isNotEmpty(armResponseUploadFileRecord.getExceptionDescription())
                    ? armResponseUploadFileRecord.getExceptionDescription() : "No error details found in response file";

                log.warn(
                    "ARM status is failed for external object id {}. ARM error description: {} ARM error status: {}",
                    externalObjectDirectory.getId(),
                    errorDescription,
                    armResponseUploadFileRecord.getErrorStatus()
                );
                externalObjectDirectory.setErrorCode(errorDescription);
                externalObjectDirectory = updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
            }
        } else {
            log.warn("Unable to read upload file {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
            externalObjectDirectory = updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
        }
        return ObjectRecordStatusEnum.valueOfId(externalObjectDirectory.getStatus().getId());
    }

    @SuppressWarnings("PMD.AvoidReassigningParameters")
    private ObjectRecordStatusEnum processInvalidLineFileObject(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor,
                                                                ArmResponseInvalidLineRecord armResponseInvalidLineRecord) {
        if (nonNull(armResponseInvalidLineRecord)) {
            //If the filename contains 0
            if (ARM_RESPONSE_INVALID_STATUS_CODE.equals(invalidLineFileFilenameProcessor.getStatus())) {
                //Read the invalid lines file and log the error code and description with EOD
                log.warn(
                    "ARM invalid line for external object id {}. ARM error description: {} ARM error status: {}",
                    externalObjectDirectory.getId(),
                    armResponseInvalidLineRecord.getExceptionDescription(),
                    armResponseInvalidLineRecord.getErrorStatus()
                );
                updateTransferAttempts(externalObjectDirectory);
                externalObjectDirectory.setErrorCode(armResponseInvalidLineRecord.getExceptionDescription());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseManifestFailedStatus);
            } else {
                String error = String.format("Incorrect status [%s] for invalid line file %s", invalidLineFileFilenameProcessor.getStatus(),
                                             invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath());
                log.warn(error);
                externalObjectDirectory.setErrorCode(error);
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
            }
        } else {
            log.warn("Unable to read invalid line file {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath());
            externalObjectDirectory = updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
        }
        return ObjectRecordStatusEnum.valueOfId(externalObjectDirectory.getStatus().getId());
    }

    private void cleanupTemporaryJsonFile(Path jsonPath) {
        if (nonNull(jsonPath)) {
            try {
                if (!Files.deleteIfExists(jsonPath)) {
                    log.warn("Deleting temporary file: {} failed", jsonPath.toFile());
                }
            } catch (Exception e) {
                log.error("Unable to delete temporary file: {} - {}", jsonPath.toFile(), e.getMessage());
            }
        }
    }

    private void processUploadFileDataSuccess(ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                              ExternalObjectDirectoryEntity externalObjectDirectory) {
        if (externalObjectDirectory.getChecksum() != null) {
            verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, externalObjectDirectory.getChecksum());
        } else {
            log.warn("Unable to verify checksum for external object {}", externalObjectDirectory.getId());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseChecksumVerificationFailedStatus());
        }
    }

    private void verifyChecksumAndUpdateStatus(ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                               ExternalObjectDirectoryEntity externalObjectDirectory,
                                               String objectChecksum) {
        if (objectChecksum.equalsIgnoreCase(armResponseUploadFileRecord.getMd5())) {
            externalObjectDirectory.setExternalFileId(armResponseUploadFileRecord.getA360FileId());
            externalObjectDirectory.setExternalRecordId(armResponseUploadFileRecord.getA360RecordId());
            externalObjectDirectory.setDataIngestionTs(OffsetDateTime.now());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armRpoPendingStatus);
        } else {
            log.warn("External object id {} checksum differs. Arm checksum: {} Object Checksum: {}",
                     externalObjectDirectory.getId(),
                     armResponseUploadFileRecord.getMd5(), objectChecksum
            );
            externalObjectDirectory.setErrorCode(armResponseUploadFileRecord.getErrorStatus());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseChecksumVerificationFailedStatus);
        }
    }

    private ObjectRecordStatusEnum updateExternalObjectDirectoryStatusAndVerificationAttempt(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                                             ObjectRecordStatusEntity objectRecordStatus) {
        if (externalObjectDirectory.getVerificationAttempts() < armDataManagementConfiguration.getMaxRetryAttempts()) {
            int verificationAttempts = externalObjectDirectory.getVerificationAttempts() + 1;
            externalObjectDirectory.setVerificationAttempts(verificationAttempts);
            return ObjectRecordStatusEnum.valueOfId(
                updateExternalObjectDirectoryStatus(externalObjectDirectory, objectRecordStatus)
                    .getStatus().getId());
        } else {
            return ObjectRecordStatusEnum.valueOfId(
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus)
                    .getStatus().getId());
        }
    }

    private void updateTransferAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        int currentNumberOfAttempts = externalObjectDirectoryEntity.getTransferAttempts();
        log.debug(
            "Updating failed transfer attempts from {} to {} for ID {}",
            currentNumberOfAttempts,
            currentNumberOfAttempts + 1,
            externalObjectDirectoryEntity.getId()
        );
        externalObjectDirectoryEntity.setTransferAttempts(currentNumberOfAttempts + 1);
    }

    private ExternalObjectDirectoryEntity updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                              ObjectRecordStatusEntity objectRecordStatus) {
        log.info(
            "ARM Push updating ARM status from {} to {} for ID {}",
            externalObjectDirectory.getStatus().getDescription(),
            objectRecordStatus.getDescription(),
            externalObjectDirectory.getId()
        );
        if (storedStatus.equals(objectRecordStatus)) {
            logApi.archiveToArmSuccessful(externalObjectDirectory.getId());
        } else if (armResponseProcessingFailedStatus.equals(objectRecordStatus)) {
            logApi.archiveToArmFailed(externalObjectDirectory.getId());
        } else if (armResponseManifestFailedStatus.equals(objectRecordStatus)
            && externalObjectDirectory.getVerificationAttempts() > armDataManagementConfiguration.getMaxRetryAttempts()) {
            logApi.archiveToArmFailed(externalObjectDirectory.getId());
        }
        externalObjectDirectory.setStatus(objectRecordStatus);
        externalObjectDirectory.setLastModifiedBy(userAccount);
        return externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
    }

    private static String generateSuffix(String filenameKey) {
        return ARM_FILENAME_SEPARATOR + filenameKey + ARM_RESPONSE_FILE_EXTENSION;
    }


}