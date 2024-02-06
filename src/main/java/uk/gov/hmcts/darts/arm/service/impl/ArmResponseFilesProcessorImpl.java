package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.arm.util.files.InputUploadFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_RESPONSE_PROCESSING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmResponseFilesProcessorImpl implements ArmResponseFilesProcessor {

    public static final String ARM_RESPONSE_FILE_EXTENSION = ".rsp";
    public static final String ARM_INPUT_UPLOAD_FILENAME_KEY = "iu";
    public static final String ARM_CREATE_RECORD_FILENAME_KEY = "cr";
    public static final String ARM_UPLOAD_FILE_FILENAME_KEY = "uf";
    public static final String ARM_RESPONSE_SUCCESS_STATUS_CODE = "1";

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final ArmDataManagementApi armDataManagementApi;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ObjectMapper objectMapper;
    private final UserIdentity userIdentity;

    private ObjectRecordStatusEntity armDropZoneStatus;
    private ObjectRecordStatusEntity armProcessingResponseFilesStatus;
    private ObjectRecordStatusEntity armResponseProcessingFailed;
    private ObjectRecordStatusEntity storedStatus;
    private ObjectRecordStatusEntity checksumFailedStatus;
    private UserAccountEntity userAccount;

    private static String generateSuffix(String filenameKey) {
        return ARM_FILENAME_SEPARATOR + filenameKey + ARM_RESPONSE_FILE_EXTENSION;
    }

    private static String getPrefix(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return externalObjectDirectory.getId().toString()
              + ARM_FILENAME_SEPARATOR
              + getObjectTypeId(externalObjectDirectory)
              + ARM_FILENAME_SEPARATOR
              + externalObjectDirectory.getTransferAttempts();
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

    @Override
    public void processResponseFiles() {
        initialisePreloadedObjects();
        // Fetch All records from external Object Directory table with external_location_type as 'ARM' and status with 'ARM dropzone'.
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());

        List<ExternalObjectDirectoryEntity> dataSentToArm =
              externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(armLocation, armDropZoneStatus);
        if (!CollectionUtils.isEmpty(dataSentToArm)) {
            log.info("ARM Response process found : {} records to be processed", dataSentToArm.size());
            for (ExternalObjectDirectoryEntity externalObjectDirectory : dataSentToArm) {
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armProcessingResponseFilesStatus);
            }
            int row = 1;
            for (ExternalObjectDirectoryEntity externalObjectDirectory : dataSentToArm) {
                log.info("ARM Response process about to process {} of {} rows", row++, dataSentToArm.size());
                try {
                    processInputUploadFile(externalObjectDirectory);
                } catch (Exception e) {
                    log.error("Unable to process response files for external object directory {}", e.getMessage());
                    updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
                }
            }
        } else {
            log.info("ARM Response process unable to find any records to process");
        }
    }

    @SuppressWarnings("java:S3655")
    private void initialisePreloadedObjects() {
        armDropZoneStatus = objectRecordStatusRepository.findById(ARM_DROP_ZONE.getId()).get();
        armProcessingResponseFilesStatus = objectRecordStatusRepository.findById(ARM_PROCESSING_RESPONSE_FILES.getId()).get();
        armResponseProcessingFailed = objectRecordStatusRepository.findById(FAILURE_ARM_RESPONSE_PROCESSING.getId()).get();
        storedStatus = objectRecordStatusRepository.findById(STORED.getId()).get();
        checksumFailedStatus = objectRecordStatusRepository.findById(FAILURE_CHECKSUM_FAILED.getId()).get();

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
        if (!CollectionUtils.isEmpty(inputUploadBlobs)) {
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

            ExternalObjectDirectoryEntity latestEod = externalObjectDirectoryRepository.getReferenceById(externalObjectDirectory.getId());

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
            if (!CollectionUtils.isEmpty(responseBlobs)) {
                processResponseBlobs(responseBlobs, externalObjectDirectory);
            } else {
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armDropZoneStatus);
            }
        } catch (IllegalArgumentException e) {
            // This occurs when the filename is not parsable
            log.error("Unable to process filename: {}", e.getMessage());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
        } catch (Exception e) {
            log.error("Unable to list responses: {}", e.getMessage());
            updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
        }
    }

    private void processResponseBlobs(List<String> responseBlobs, ExternalObjectDirectoryEntity externalObjectDirectory) {
        String createRecordFilename = null;
        String uploadFilename = null;

        for (String responseFile : responseBlobs) {
            if (responseFile.endsWith(generateSuffix(ARM_CREATE_RECORD_FILENAME_KEY))) {
                createRecordFilename = responseFile;
            } else if (responseFile.endsWith(generateSuffix(ARM_UPLOAD_FILE_FILENAME_KEY))) {
                uploadFilename = responseFile;
            }
        }

        if (nonNull(createRecordFilename) && nonNull(uploadFilename)) {
            try {
                UploadFileFilenameProcessor uploadFileFilenameProcessor = new UploadFileFilenameProcessor(uploadFilename);

                BinaryData uploadFileBinary = armDataManagementApi.getBlobData(uploadFilename);
                readUploadFile(externalObjectDirectory, uploadFileBinary, uploadFileFilenameProcessor);
            } catch (IllegalArgumentException e) {
                // This occurs when the filename is not parsable
                log.error("Unable to process filename: {}", e.getMessage());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
            } catch (Exception e) {
                log.error("Failure with to get upload file {}", e.getMessage(), e);
                updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
            }
        } else {
            log.info("Unable to find response files for external object {}", externalObjectDirectory.getId());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armDropZoneStatus);
        }
    }

    private void readUploadFile(ExternalObjectDirectoryEntity externalObjectDirectory,
          BinaryData uploadFileBinary,
          UploadFileFilenameProcessor uploadFileFilenameProcessor) {
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
                    processUploadFileObject(externalObjectDirectory, uploadFileFilenameProcessor, armResponseUploadFileRecord);
                } else {
                    log.warn("Failed to write upload file to temp workspace {}", uploadFileFilenameProcessor.getUploadFileFilename());
                    updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
                }
            } catch (IOException e) {
                log.error("Unable to write upload file to temporary workspace {} - {}", uploadFileFilenameProcessor.getUploadFileFilename(), e.getMessage());
                updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
            } catch (Exception e) {
                log.error("Unable to process arm response upload file {}", uploadFileFilenameProcessor.getUploadFileFilename());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
            } finally {
                cleanupTemporaryJsonFile(jsonPath);
            }
        } else {
            updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
        }
    }

    private void processUploadFileObject(ExternalObjectDirectoryEntity externalObjectDirectory,
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
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
            }
        } else {
            log.warn("Unable to read upload file {}", uploadFileFilenameProcessor.getUploadFileFilename());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
        }
    }

    private void cleanupTemporaryJsonFile(Path jsonPath) {
        if (nonNull(jsonPath)) {
            try {
                if (!Files.deleteIfExists(jsonPath)) {
                    log.warn("Deleting temporary file: {} failed", jsonPath.toFile());
                }
            } catch (Exception e) {
                log.error("Unable to delete temporary file: {}", jsonPath.toFile());
            }
        }
    }

    private void processUploadFileDataSuccess(ArmResponseUploadFileRecord armResponseUploadFileRecord,
          ExternalObjectDirectoryEntity externalObjectDirectory) {
        // Validate the checksum in external object directory table against the Media, TranscriptionDocument, or AnnotationDocument
        if (nonNull(externalObjectDirectory.getMedia())) {
            MediaEntity media = externalObjectDirectory.getMedia();
            if (nonNull(media.getChecksum())) {
                String objectChecksum = media.getChecksum();
                verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, objectChecksum);
            } else {
                log.warn("Unable to verify media checksum for external object {}", externalObjectDirectory.getId());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
            }
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            TranscriptionDocumentEntity transcriptionDocument = externalObjectDirectory.getTranscriptionDocumentEntity();
            if (nonNull(transcriptionDocument.getChecksum())) {
                String objectChecksum = transcriptionDocument.getChecksum();
                verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, objectChecksum);
            } else {
                log.warn("Unable to verify transcription checksum for external object {}", externalObjectDirectory.getId());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
            }
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            AnnotationDocumentEntity annotationDocument = externalObjectDirectory.getAnnotationDocumentEntity();
            if (nonNull(annotationDocument.getChecksum())) {
                String objectChecksum = annotationDocument.getChecksum();
                verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, objectChecksum);
            } else {
                log.warn("Unable to verify annotation checksum for external object {}", externalObjectDirectory.getId());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
            }
        } else {
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
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
            updateExternalObjectDirectoryStatus(externalObjectDirectory, checksumFailedStatus);
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
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
            } catch (JsonProcessingException e) {
                log.error("Unable to parse the upload record file ");
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
            }
        } else {
            log.warn("Unable to get the upload record file input field");
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
        }
        return uploadNewFileRecord;
    }

    private void updateExternalObjectDirectoryStatusAndVerificationAttempt(ExternalObjectDirectoryEntity externalObjectDirectory,
          ObjectRecordStatusEntity objectRecordStatus) {
        if (externalObjectDirectory.getVerificationAttempts() < armDataManagementConfiguration.getMaxRetryAttempts()) {
            int verificationAttempts = externalObjectDirectory.getVerificationAttempts() + 1;
            externalObjectDirectory.setVerificationAttempts(verificationAttempts);
            updateExternalObjectDirectoryStatus(externalObjectDirectory, objectRecordStatus);
        } else {
            ObjectRecordStatusEntity armResponseProcessingFailed = objectRecordStatusRepository.getReferenceById(FAILURE_ARM_RESPONSE_PROCESSING.getId());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailed);
        }
    }

    private void updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity externalObjectDirectory, ObjectRecordStatusEntity objectRecordStatus) {
        log.info(
              "ARM Push updating ARM status from {} to {} for ID {}",
              externalObjectDirectory.getStatus().getDescription(),
              objectRecordStatus.getDescription(),
              externalObjectDirectory.getId()
        );
        externalObjectDirectory.setStatus(objectRecordStatus);
        externalObjectDirectory.setLastModifiedBy(userAccount);
        externalObjectDirectory.setLastModifiedDateTime(OffsetDateTime.now());
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
    }


}
