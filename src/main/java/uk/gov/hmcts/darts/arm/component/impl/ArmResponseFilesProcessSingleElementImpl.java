package uk.gov.hmcts.darts.arm.component.impl;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInvalidLineRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.util.files.InputUploadFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.InvalidLineFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmResponseFilesProcessSingleElementImpl implements ArmResponseFilesProcessSingleElement {

    public static final String ARM_RESPONSE_FILE_EXTENSION = ".rsp";
    public static final String ARM_INPUT_UPLOAD_FILENAME_KEY = "iu";
    public static final String ARM_CREATE_RECORD_FILENAME_KEY = "cr";
    public static final String ARM_UPLOAD_FILE_FILENAME_KEY = "uf";
    public static final String ARM_INVALID_LINE_FILENAME_KEY = "il";
    public static final String ARM_RESPONSE_SUCCESS_STATUS_CODE = "1";
    public static final String ARM_RESPONSE_INVALID_STATUS_CODE = "0";

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ArmDataManagementApi armDataManagementApi;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ObjectMapper objectMapper;
    private final UserIdentity userIdentity;

    private ObjectRecordStatusEntity armDropZoneStatus;
    private ObjectRecordStatusEntity armProcessingResponseFilesStatus;
    private ObjectRecordStatusEntity armResponseProcessingFailedStatus;
    private ObjectRecordStatusEntity armResponseManifestFailedStatus;
    private ObjectRecordStatusEntity storedStatus;
    private ObjectRecordStatusEntity armResponseChecksumVerificationFailedStatus;
    private UserAccountEntity userAccount;

    @Transactional
    public void processResponseFilesFor(Integer externalObjectDirectoryId) {
        initialisePreloadedObjects();
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = externalObjectDirectoryRepository.findById(externalObjectDirectoryId).get();
        try {
            processInputUploadFile(externalObjectDirectoryEntity);
        } catch (Exception e) {
            log.error("Unable to process response files for external object directory.", e);
            updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectoryEntity, armDropZoneStatus);
        }
    }

    @SuppressWarnings("java:S3655")
    private void initialisePreloadedObjects() {
        storedStatus = objectRecordStatusRepository.findById(STORED.getId()).get();
        armDropZoneStatus = objectRecordStatusRepository.findById(ARM_DROP_ZONE.getId()).get();
        armProcessingResponseFilesStatus = objectRecordStatusRepository.findById(ARM_PROCESSING_RESPONSE_FILES.getId()).get();
        armResponseManifestFailedStatus = objectRecordStatusRepository.findById(ARM_RESPONSE_MANIFEST_FAILED.getId()).get();
        armResponseProcessingFailedStatus = objectRecordStatusRepository.findById(ARM_RESPONSE_PROCESSING_FAILED.getId()).get();
        armResponseChecksumVerificationFailedStatus = objectRecordStatusRepository.findById(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId()).get();

        userAccount = userIdentity.getUserAccount();
    }

    private void processInputUploadFile(ExternalObjectDirectoryEntity externalObjectDirectory) {
        // IU - Input Upload - This is the manifest file which gets renamed by ARM.
        // EODID_MEDID_ATTEMPTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp
        String prefix = getPrefix(externalObjectDirectory);
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
            processInvalidLineFile(armInputUploadFilename, responseBlobs, externalObjectDirectory, invalidLineFilename);
        } else if (nonNull(createRecordFilename) && nonNull(uploadFilename)) {
            processUploadFile(armInputUploadFilename, responseBlobs, externalObjectDirectory, uploadFilename);
        } else {
            log.info("Unable to find response files for external object {}", externalObjectDirectory.getId());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armDropZoneStatus);
        }
    }

    private void processInvalidLineFile(String armInputUploadFilename, List<String> responseBlobs, ExternalObjectDirectoryEntity externalObjectDirectory,
                                        String invalidLineFilename) {
        try {
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
                                   String uploadFilename) {
        try {
            UploadFileFilenameProcessor uploadFileFilenameProcessor = new UploadFileFilenameProcessor(uploadFilename);

            BinaryData uploadFileBinary = armDataManagementApi.getBlobData(uploadFilename);
            ObjectRecordStatusEnum status = readUploadFile(externalObjectDirectory, uploadFileBinary, uploadFileFilenameProcessor);
            if (STORED.equals(status)
                || ARM_RESPONSE_PROCESSING_FAILED.equals(status)
                || ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.equals(status)) {
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

    void deleteResponseBlobs(String armInputUploadFilename, List<String> responseBlobs, ExternalObjectDirectoryEntity externalObjectDirectory) {
        List<Boolean> deletedResponseBlobStatuses;
        deletedResponseBlobStatuses = responseBlobs.stream()
            .map(armDataManagementApi::deleteBlobData)
            .toList();
        if (deletedResponseBlobStatuses.size() == 2 && !deletedResponseBlobStatuses.contains(false)) {
            externalObjectDirectory.setResponseCleaned(
                armDataManagementApi.deleteBlobData(armInputUploadFilename));
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

                if (jsonPath.toFile().exists()) {
                    ArmResponseUploadFileRecord armResponseUploadFileRecord = objectMapper.readValue(jsonPath.toFile(), ArmResponseUploadFileRecord.class);
                    objectRecordStatusEnum = processUploadFileObject(externalObjectDirectory, uploadFileFilenameProcessor, armResponseUploadFileRecord);
                } else {
                    log.warn("Failed to write upload file to temp workspace {}", uploadFileFilenameProcessor.getUploadFileFilename());
                    objectRecordStatusEnum = updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
                }
            } catch (IOException e) {
                log.error("Unable to write upload file to temporary workspace {} - {}", uploadFileFilenameProcessor.getUploadFileFilename(), e.getMessage());
                objectRecordStatusEnum = updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
            } catch (Exception e) {
                log.error("Unable to process arm response upload file {} - {}", uploadFileFilenameProcessor.getUploadFileFilename(), e.getMessage());
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
                    invalidLineFileFilenameProcessor.getInvalidLineFileFilename(),
                    armDataManagementConfiguration.getTempBlobWorkspace(),
                    appendUuidToWorkspace
                );

                if (jsonPath.toFile().exists()) {
                    ArmResponseInvalidLineRecord armResponseInvalidLineRecord = objectMapper.readValue(jsonPath.toFile(), ArmResponseInvalidLineRecord.class);
                    objectRecordStatusEnum = processInvalidLineFileObject(externalObjectDirectory, invalidLineFileFilenameProcessor,
                                                                          armResponseInvalidLineRecord);
                } else {
                    log.warn("Failed to write invalid line file to temp workspace {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
                    objectRecordStatusEnum = updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory,
                                                                                                       armResponseProcessingFailedStatus);
                }
            } catch (IOException e) {
                log.error("Unable to write invalid line file to temporary workspace {} - {}",
                          invalidLineFileFilenameProcessor.getInvalidLineFileFilename(), e.getMessage());
                objectRecordStatusEnum = updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory,
                                                                                                   armResponseProcessingFailedStatus);
            } catch (Exception e) {
                log.error("Unable to process arm response invalid line file {} - {}",
                          invalidLineFileFilenameProcessor.getInvalidLineFileFilename(), e.getMessage());
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

    private ObjectRecordStatusEnum processUploadFileObject(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                           UploadFileFilenameProcessor uploadFileFilenameProcessor,
                                                           ArmResponseUploadFileRecord armResponseUploadFileRecord) {
        if (nonNull(armResponseUploadFileRecord)) {
            //If the filename contains 1
            if (ARM_RESPONSE_SUCCESS_STATUS_CODE.equals(uploadFileFilenameProcessor.getStatus())) {
                processUploadFileDataSuccess(armResponseUploadFileRecord, externalObjectDirectory);
            } else {
                //Read the upload file and log the error code and description with EOD
                log.warn(
                    "ARM status is failed for external object id {}. ARM error description: {} ARM error status: {}",
                    externalObjectDirectory.getId(),
                    armResponseUploadFileRecord.getExceptionDescription(),
                    armResponseUploadFileRecord.getErrorStatus()
                );
                externalObjectDirectory = updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
            }
        } else {
            log.warn("Unable to read upload file {}", uploadFileFilenameProcessor.getUploadFileFilename());
            externalObjectDirectory = updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
        }
        return ObjectRecordStatusEnum.valueOfId(externalObjectDirectory.getStatus().getId());
    }

    private ObjectRecordStatusEnum processInvalidLineFileObject(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor,
                                                                ArmResponseInvalidLineRecord armResponseInvalidLineRecord) {
        if (nonNull(armResponseInvalidLineRecord)) {
            //If the filename contains 0
            if (ARM_RESPONSE_INVALID_STATUS_CODE.equals(invalidLineFileFilenameProcessor.getStatus())) {
                log.warn(
                    "ARM invalid line for external object id {}. ARM error description: {} ARM error status: {}",
                    externalObjectDirectory.getId(),
                    armResponseInvalidLineRecord.getExceptionDescription(),
                    armResponseInvalidLineRecord.getErrorStatus()
                );
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseManifestFailedStatus);
            } else {
                log.warn("Incorrect status [{}] for invalid line file {}", invalidLineFileFilenameProcessor.getStatus(),
                         invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
            }
        } else {
            log.warn("Unable to read invalid line file {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
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
        // Validate the checksum in external object directory table against the Media, TranscriptionDocument, or AnnotationDocument
        if (nonNull(externalObjectDirectory.getMedia())) {
            MediaEntity media = externalObjectDirectory.getMedia();

            if (nonNull(media.getChecksum())) {
                verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, media.getChecksum());
            } else {
                log.warn("Unable to verify media checksum for external object {}", externalObjectDirectory.getId());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseChecksumVerificationFailedStatus);
            }
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            TranscriptionDocumentEntity transcriptionDocument = externalObjectDirectory.getTranscriptionDocumentEntity();
            if (nonNull(transcriptionDocument.getChecksum())) {
                verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, transcriptionDocument.getChecksum());
            } else {
                log.warn("Unable to verify transcription document checksum for external object {}", externalObjectDirectory.getId());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseChecksumVerificationFailedStatus);
            }
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            AnnotationDocumentEntity annotationDocument = externalObjectDirectory.getAnnotationDocumentEntity();
            if (nonNull(annotationDocument.getChecksum())) {
                verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, annotationDocument.getChecksum());
            } else {
                log.warn("Unable to verify annotation document checksum for external object {}", externalObjectDirectory.getId());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseChecksumVerificationFailedStatus);
            }
        } else {
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
        }
    }

    private void verifyChecksumAndUpdateStatus(ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                               ExternalObjectDirectoryEntity externalObjectDirectory,
                                               String objectChecksum) {
        if (objectChecksum.equals(armResponseUploadFileRecord.getMd5())) {
            UploadNewFileRecord uploadNewFileRecord = readInputJson(externalObjectDirectory, armResponseUploadFileRecord.getInput());
            if (nonNull(uploadNewFileRecord)) {
                externalObjectDirectory.setExternalFileId(uploadNewFileRecord.getFileMetadata().getDzFilename());
                externalObjectDirectory.setExternalRecordId(uploadNewFileRecord.getRelationId());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, storedStatus);
            }
        } else {
            log.warn("External object id {} checksum differs. Arm checksum: {} Object Checksum: {}",
                     externalObjectDirectory.getId(),
                     armResponseUploadFileRecord.getMd5(), objectChecksum
            );
            externalObjectDirectory.setErrorCode(armResponseUploadFileRecord.getErrorStatus());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseChecksumVerificationFailedStatus);
        }

    }

    private UploadNewFileRecord readInputJson(ExternalObjectDirectoryEntity externalObjectDirectory, String input) {
        UploadNewFileRecord uploadNewFileRecord = null;
        if (StringUtils.isNotEmpty(input)) {
            String unescapedJson = StringEscapeUtils.unescapeJson(input);
            try {
                uploadNewFileRecord = objectMapper.readValue(unescapedJson, UploadNewFileRecord.class);
            } catch (JsonMappingException e) {
                log.error("Unable to map the upload record file input field");
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
            } catch (JsonProcessingException e) {
                log.error("Unable to parse the upload record file ");
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
            }
        } else {
            log.warn("Unable to get the upload record file input field");
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
        }
        return uploadNewFileRecord;
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

    private ExternalObjectDirectoryEntity updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                              ObjectRecordStatusEntity objectRecordStatus) {
        log.info(
            "ARM Push updating ARM status from {} to {} for ID {}",
            externalObjectDirectory.getStatus().getDescription(),
            objectRecordStatus.getDescription(),
            externalObjectDirectory.getId()
        );
        externalObjectDirectory.setStatus(objectRecordStatus);
        externalObjectDirectory.setLastModifiedBy(userAccount);
        return externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
    }

    private static String generateSuffix(String filenameKey) {
        return ARM_FILENAME_SEPARATOR + filenameKey + ARM_RESPONSE_FILE_EXTENSION;
    }

    private static String getPrefix(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return new StringBuilder(externalObjectDirectory.getId().toString())
            .append(ARM_FILENAME_SEPARATOR)
            .append(getObjectTypeId(externalObjectDirectory))
            .append(ARM_FILENAME_SEPARATOR)
            .append(externalObjectDirectory.getTransferAttempts()).toString();
    }

    private static String getObjectTypeId(ExternalObjectDirectoryEntity externalObjectDirectory) {
        String objectTypeId = "";
        if (nonNull(externalObjectDirectory.getMedia())) {
            objectTypeId = externalObjectDirectory.getMedia().getId().toString();
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            objectTypeId = externalObjectDirectory.getTranscriptionDocumentEntity().getId().toString();
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            objectTypeId = externalObjectDirectory.getAnnotationDocumentEntity().getId().toString();
        }
        return objectTypeId;
    }


}
