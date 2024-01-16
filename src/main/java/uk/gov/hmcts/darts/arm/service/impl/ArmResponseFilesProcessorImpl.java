package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveRecordOperationValues.ARM_FILENAME_SEPARATOR;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
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

    @Transactional
    @Override
    public void processResponseFiles() {
        startProcessingResponseFiles();
    }

    private void startProcessingResponseFiles() {
        // Fetch All records from external Object Directory table with external_location_type as 'ARM' and status with 'ARM dropzone'.
        ExternalLocationTypeEntity armLocation = externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.ARM.getId());

        ObjectRecordStatusEntity armDropZoneStatus = objectRecordStatusRepository.getReferenceById(ARM_DROP_ZONE.getId());
        ObjectRecordStatusEntity armProcessingResponseFilesStatus = objectRecordStatusRepository.getReferenceById(ARM_PROCESSING_RESPONSE_FILES.getId());

        List<ExternalObjectDirectoryEntity> dataSentToArm =
            externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(armLocation, armDropZoneStatus);

        for (ExternalObjectDirectoryEntity externalObjectDirectory: dataSentToArm) {
            updateExternalObjectDirectory(externalObjectDirectory, armProcessingResponseFilesStatus);
        }
        for (ExternalObjectDirectoryEntity externalObjectDirectory: dataSentToArm) {
            try {
                processInputUploadFile(externalObjectDirectory);
            } catch (Exception e) {
                log.error("Unable to process response files for external object directory {}", e.getMessage());
                updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
            }
        }
    }

    private void processInputUploadFile(ExternalObjectDirectoryEntity externalObjectDirectory) {
        ObjectRecordStatusEntity armDropZoneStatus = objectRecordStatusRepository.getReferenceById(ARM_DROP_ZONE.getId());
        // IU - Input Upload - This is the manifest file which gets renamed by ARM.
        // EODID_MEDID_ATTEMPTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp
        String prefix = getPrefix(externalObjectDirectory);
        Map<String, BlobItem> inputUploadBlobs = getInputUploadBlobs(externalObjectDirectory, prefix);
        if (nonNull(inputUploadBlobs) && !inputUploadBlobs.isEmpty()) {
            //String armInputUploadFilename = inputUploadBlobs.keySet().stream().findFirst().get();
            for (String armInputUploadFilename: inputUploadBlobs.keySet()) {
                log.debug("Found ARM input upload file {}", armInputUploadFilename);
                if (armInputUploadFilename.endsWith(generateSuffix(ARM_INPUT_UPLOAD_FILENAME_KEY))) {
                    readInputUploadFile(externalObjectDirectory, armInputUploadFilename, armDropZoneStatus);
                    break;
                } else {
                    updateExternalObjectDirectory(externalObjectDirectory, armDropZoneStatus);
                }
            }
        } else {
            log.info("Unable to find input file with prefix {}", prefix);
            updateExternalObjectDirectory(externalObjectDirectory, armDropZoneStatus);
        }
    }

    private void readInputUploadFile(ExternalObjectDirectoryEntity externalObjectDirectory, String armInputUploadFilename,
                                     ObjectRecordStatusEntity armDropZoneStatus) {
        try {
            InputUploadFilenameProcessor inputUploadFilenameProcessor = new InputUploadFilenameProcessor(armInputUploadFilename);
            String responseFilesHashcode = inputUploadFilenameProcessor.getHashcode();
            log.debug("List response files starting with hashcode {}", responseFilesHashcode);
            Map<String, BlobItem> responseBlobs = armDataManagementApi.listResponseBlobs(responseFilesHashcode);
            if (nonNull(responseBlobs) && !responseBlobs.isEmpty()) {
                processResponseBlobs(responseBlobs, externalObjectDirectory);
            } else {
                updateExternalObjectDirectory(externalObjectDirectory, armDropZoneStatus);
            }
        } catch (IllegalArgumentException e) {
            log.error("Unable to process filename: {}", e.getMessage());
            updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
        } catch (IOException e) {
            log.error("Unable to read response file: {}", e.getMessage());
            updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
        }
    }

    private Map<String, BlobItem> getInputUploadBlobs(ExternalObjectDirectoryEntity externalObjectDirectory, String prefix) {
        ObjectRecordStatusEntity armDropZoneStatus = objectRecordStatusRepository.getReferenceById(ARM_DROP_ZONE.getId());
        log.debug("Checking ARM for files containing name {}", prefix);
        Map<String, BlobItem> inputUploadBlobs = null;
        try {
            inputUploadBlobs = armDataManagementApi.listResponseBlobs(prefix);
        } catch (Exception e) {
            updateExternalObjectDirectory(externalObjectDirectory, armDropZoneStatus); //increment verification attempts
        }
        return inputUploadBlobs;
    }

    private void processResponseBlobs(Map<String, BlobItem> responseBlobs, ExternalObjectDirectoryEntity externalObjectDirectory) throws IOException {
        String createRecordFilename = null;
        String uploadFilename = null;
        ObjectRecordStatusEntity armResponseProcessingFailed = objectRecordStatusRepository.getReferenceById(ARM_RESPONSE_PROCESSING_FAILED.getId());
        ObjectRecordStatusEntity armDropZoneStatus = objectRecordStatusRepository.getReferenceById(ARM_DROP_ZONE.getId());

        for (String responseFile : responseBlobs.keySet()) {
            /*
               CR - Create Record - This is the create record file which represents record creation in ARM.
               -- 6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp
               UF - Upload File - This is the Upload file which represents the File which is ingested by ARM.
               -- 6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp */
            if (responseFile.endsWith(generateSuffix(ARM_CREATE_RECORD_FILENAME_KEY))) {
                createRecordFilename = responseFile;
            } else if (responseFile.endsWith(generateSuffix(ARM_UPLOAD_FILE_FILENAME_KEY))) {
                uploadFilename = responseFile;
            }
        }

        if (nonNull(createRecordFilename) && nonNull(uploadFilename)) {
            try {
                UploadFileFilenameProcessor uploadFileFilenameProcessor = new UploadFileFilenameProcessor(uploadFilename);
                boolean appendUuidToWorkspace = true;

                BinaryData uploadFileBinary = armDataManagementApi.getResponseBlobData(uploadFilename);
                if (nonNull(uploadFileBinary)) {
                    Path jsonPath = null;
                    try {
                        jsonPath = fileOperationService.saveBinaryDataToSpecifiedWorkspace(
                            uploadFileBinary,
                            uploadFilename,
                            armDataManagementConfiguration.getTempBlobWorkspace(),
                            appendUuidToWorkspace
                        );
                        if (jsonPath.toFile().exists()) {

                            ArmResponseUploadFileRecord armResponseUploadFileRecord = objectMapper.readValue(
                                jsonPath.toFile(),
                                ArmResponseUploadFileRecord.class
                            );
                            if (nonNull(armResponseUploadFileRecord)) {
                                if (ARM_RESPONSE_SUCCESS_STATUS_CODE.equals(uploadFileFilenameProcessor.getStatus())) {
                                    processUploadFileDataSuccess(armResponseUploadFileRecord, externalObjectDirectory);
                                } else {
                                    //Read the upload file and log the error code and description with EOD

                                    updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
                                    cleanupTemporaryJsonFile(jsonPath);
                                }
                            } else {
                                updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
                                cleanupTemporaryJsonFile(jsonPath);
                            }
                        } else {
                            log.error("Unable to write upload file to temp workspace{}", uploadFilename);
                            updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
                            cleanupTemporaryJsonFile(jsonPath);
                        }
                    } catch (Exception e) {
                        log.error("Unable to process arm response upload file {}", uploadFilename);
                        updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
                        cleanupTemporaryJsonFile(jsonPath);
                    }
                } else {
                    updateExternalObjectDirectoryStatusAndVerificationAttempt(externalObjectDirectory, armDropZoneStatus);
                }
            } catch (Exception e) {
                log.error("Failure with upload file {}", e.getMessage(), e);
                updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
            }
        } else {
            updateExternalObjectDirectory(externalObjectDirectory, armDropZoneStatus);
        }
    }

    private void cleanupTemporaryJsonFile(Path jsonPath) {
        if (nonNull(jsonPath) && jsonPath.toFile().exists()) {
            try {
                jsonPath.toFile().delete();
            } catch (Exception e) {
                log.error("Unable to delete temporary file: {}", jsonPath.toFile().toString());
            }
        }
    }

    private void processUploadFileDataSuccess(ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                              ExternalObjectDirectoryEntity externalObjectDirectory) {
        // Validate the checksum in external object directory table against the Media, TranscriptionDocument, or AnnotationDocument
        ObjectRecordStatusEntity armResponseProcessingFailed = objectRecordStatusRepository.getReferenceById(ARM_RESPONSE_PROCESSING_FAILED.getId());
        if (nonNull(externalObjectDirectory.getMedia())) {
            MediaEntity media = externalObjectDirectory.getMedia();
            if (nonNull(media.getChecksum())) {
                String objectChecksum = media.getChecksum();
                verifyChecksum(armResponseUploadFileRecord, externalObjectDirectory, objectChecksum);
            } else {
                log.warn("Unable to verify media checksum for external object {}", externalObjectDirectory.getId());
                updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
            }
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            TranscriptionDocumentEntity transcriptionDocument = externalObjectDirectory.getTranscriptionDocumentEntity();
            if (nonNull(transcriptionDocument.getChecksum())) {
                String objectChecksum = transcriptionDocument.getChecksum();
                verifyChecksum(armResponseUploadFileRecord, externalObjectDirectory, objectChecksum);
            } else {
                log.warn("Unable to verify transcription checksum for external object {}", externalObjectDirectory.getId());
                updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
            }
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            AnnotationDocumentEntity annotationDocument = externalObjectDirectory.getAnnotationDocumentEntity();
            if (nonNull(annotationDocument.getChecksum())) {
                String objectChecksum = annotationDocument.getChecksum();
                verifyChecksum(armResponseUploadFileRecord, externalObjectDirectory, objectChecksum);
            } else {
                log.warn("Unable to verify annotation checksum for external object {}", externalObjectDirectory.getId());
                updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
            }
        }
    }

    private void verifyChecksum(ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                ExternalObjectDirectoryEntity externalObjectDirectory,
                                String objectChecksum) {
        ObjectRecordStatusEntity storedStatus = objectRecordStatusRepository.getReferenceById(STORED.getId());
        ObjectRecordStatusEntity armResponseProcessingFailed = objectRecordStatusRepository.getReferenceById(ARM_RESPONSE_PROCESSING_FAILED.getId());
        if (objectChecksum.equals(armResponseUploadFileRecord.getMd5())) {
            String input = armResponseUploadFileRecord.getInput();
            if (StringUtils.isNotEmpty(input)) {
                String unescapedJson = StringEscapeUtils.escapeJson(input);
                try {
                    UploadNewFileRecord uploadNewFileRecord = objectMapper.readValue(unescapedJson, UploadNewFileRecord.class);
                    externalObjectDirectory.setExternalFileId(uploadNewFileRecord.getFileMetadata().getDzFilename());
                    externalObjectDirectory.setExternalRecordId(uploadNewFileRecord.getRelationId());
                    updateExternalObjectDirectory(externalObjectDirectory, storedStatus);
                } catch (JsonMappingException e) {
                    log.error("Unable to map the upload record file input field");
                    updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
                } catch (JsonProcessingException e) {
                    log.error("Unable to parse the upload record file ");
                    updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
                }
            } else {
                log.warn("Unable to get the upload record file input field");
                updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
            }
        } else {
            log.warn("External object id {} checksum differs. Arm checksum: {} Object Checksum: {}", externalObjectDirectory.getId(),
                     armResponseUploadFileRecord.getMd5(), objectChecksum);
            updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
        }

    }

    private void updateExternalObjectDirectoryStatusAndVerificationAttempt(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                                           ObjectRecordStatusEntity objectRecordStatus) {
        if (externalObjectDirectory.getVerificationAttempts() <= armDataManagementConfiguration.getMaxRetryAttempts()) {
            int verificationAttempts = externalObjectDirectory.getVerificationAttempts() + 1;
            externalObjectDirectory.setVerificationAttempts(verificationAttempts);
            updateExternalObjectDirectory(externalObjectDirectory, objectRecordStatus);
        } else {
            ObjectRecordStatusEntity armResponseProcessingFailed = objectRecordStatusRepository.getReferenceById(ARM_RESPONSE_PROCESSING_FAILED.getId());
            updateExternalObjectDirectory(externalObjectDirectory, armResponseProcessingFailed);
        }
    }

    private void updateExternalObjectDirectory(ExternalObjectDirectoryEntity externalObjectDirectory, ObjectRecordStatusEntity objectRecordStatus) {
        externalObjectDirectory.setStatus(objectRecordStatus);
        externalObjectDirectory.setLastModifiedBy(userIdentity.getUserAccount());
        externalObjectDirectory.setLastModifiedDateTime(OffsetDateTime.now());
        externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
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
