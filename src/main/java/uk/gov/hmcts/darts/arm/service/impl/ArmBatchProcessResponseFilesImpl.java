package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.ResponseFilenames;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInvalidLineRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.service.ArmBatchProcessResponseFiles;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.arm.util.files.BatchMetadataFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.InvalidLineFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_CREATE_RECORD_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_INVALID_LINE_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_RESPONSE_INVALID_STATUS_CODE;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_RESPONSE_SUCCESS_STATUS_CODE;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_UPLOAD_FILE_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArmConstants.ArmBatching.PROCESS_SINGLE_RECORD_BATCH_SIZE;
import static uk.gov.hmcts.darts.arm.util.ArmResponseFilesHelper.generateSuffix;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmBatchProcessResponseFilesImpl implements ArmBatchProcessResponseFiles {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ArmDataManagementApi armDataManagementApi;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ObjectMapper objectMapper;
    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;
    private final ArmResponseFilesProcessor armResponseFilesProcessor;


    private ObjectRecordStatusEntity armDropZoneStatus;
    private ObjectRecordStatusEntity armProcessingResponseFilesStatus;
    private ObjectRecordStatusEntity armResponseProcessingFailedStatus;
    private ObjectRecordStatusEntity armResponseManifestFailedStatus;
    private ObjectRecordStatusEntity storedStatus;
    private ObjectRecordStatusEntity armResponseChecksumVerificationFailedStatus;
    private UserAccountEntity userAccount;

    @Override
    public void batchProcessResponseFiles() {
        if (PROCESS_SINGLE_RECORD_BATCH_SIZE.equals(armDataManagementConfiguration.getBatchSize())) {
            armResponseFilesProcessor.processResponseFiles();
        } else {
            batchProcessResponseFilesFromAzure();
        }
    }

    private void batchProcessResponseFilesFromAzure() {
        initialisePreloadedObjects();
        ContinuationTokenBlobs continuationTokenBlobs = null;
        String prefix = armDataManagementConfiguration.getManifestFilePrefix();

        try {
            log.info("About to look for files starting with prefix: {}", prefix);
            String continuationToken = getContinuationToken();
            continuationTokenBlobs = armDataManagementApi.listResponseBlobsUsingMarker(prefix, continuationToken);
        } catch (Exception e) {
            log.error("Unable to find response file for prefix: {} - {}", prefix, e.getMessage());
        }
        if (nonNull(continuationTokenBlobs) && CollectionUtils.isNotEmpty(continuationTokenBlobs.getBlobNamesAndPaths())) {
            for (String manifestBlobFilenameAndPath : continuationTokenBlobs.getBlobNamesAndPaths()) {
                processManifestBlob(manifestBlobFilenameAndPath);
            }
        } else {
            log.warn("No response files found with prefix: {}", prefix);
        }
    }

    private void processManifestBlob(String manifestBlobFilenameAndPath) {
        log.debug("Found ARM manifest file {}", manifestBlobFilenameAndPath);
        try {
            BatchMetadataFilenameProcessor batchMetadataFilenameProcessor = new BatchMetadataFilenameProcessor(manifestBlobFilenameAndPath);
            List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = externalObjectDirectoryRepository
                .findAllByStatusAndManifestFile(armDropZoneStatus, batchMetadataFilenameProcessor.getBatchMetadataFilename());
            if (CollectionUtils.isNotEmpty(externalObjectDirectoryEntities)) {
                externalObjectDirectoryRepository.updateStatus(
                    armProcessingResponseFilesStatus,
                    userAccount,
                    externalObjectDirectoryEntities.stream().map(ExternalObjectDirectoryEntity::getId).toList(),
                    currentTimeHelper.currentOffsetDateTime());

            } else {
                log.warn("No external object directories found with filename: {}", batchMetadataFilenameProcessor.getBatchMetadataFilename());
            }

            processResponseFileByHashcode(batchMetadataFilenameProcessor);

            List<ExternalObjectDirectoryEntity> unprocessedExternalObjectDirectoryEntities = externalObjectDirectoryRepository
                .findAllByStatusAndManifestFile(armProcessingResponseFilesStatus, batchMetadataFilenameProcessor.getBatchMetadataFilename());
            if (CollectionUtils.isNotEmpty(unprocessedExternalObjectDirectoryEntities)) {
                externalObjectDirectoryRepository.updateStatus(
                    armProcessingResponseFilesStatus,
                    userAccount,
                    unprocessedExternalObjectDirectoryEntities.stream().map(ExternalObjectDirectoryEntity::getId).toList(),
                    currentTimeHelper.currentOffsetDateTime());
                for (ExternalObjectDirectoryEntity unprocessedExternalObjectDirectoryEntity : unprocessedExternalObjectDirectoryEntities) {
                    log.warn("Unable to process ARM responses for EOD {}", unprocessedExternalObjectDirectoryEntity.getId());
                }
            }
        } catch (IllegalArgumentException e) {
            log.error("Unable to process manifest filename {}", manifestBlobFilenameAndPath, e);
        } catch (Exception e) {
            log.error("Unable to process manifest", e);
        }
    }

    private void processResponseFileByHashcode(BatchMetadataFilenameProcessor batchMetadataFilenameProcessor) {
        try {
            List<String> responseFiles = armDataManagementApi.listResponseBlobs(batchMetadataFilenameProcessor.getUuidString());
            if (CollectionUtils.isNotEmpty(responseFiles)) {
                ResponseFilenames responseFilenames = getArmResponseFilenames(responseFiles);
                if (responseFilenames.containsResponses()) {
                    processUploadResponseFiles(responseFilenames.getUploadFileResponses());
                    processInvalidFiles(responseFilenames.getInvalidLineResponses());
                }
            }
        } catch (Exception e) {
            log.error("Unable to process responses for file {}", batchMetadataFilenameProcessor.getBatchMetadataFilenameAndPath(), e);
        }
    }

    private void processUploadResponseFiles(List<UploadFileFilenameProcessor> uploadFileResponses) {
        for (UploadFileFilenameProcessor uploadFileFilenameProcessor : uploadFileResponses) {
            try {
                BinaryData uploadFileBinary = armDataManagementApi.getBlobData(uploadFileFilenameProcessor.getUploadFileFilename());
                readUploadFile(uploadFileBinary, uploadFileFilenameProcessor);
            } catch (Exception e) {
                log.error("Unable to process upload file {}", uploadFileFilenameProcessor.getUploadFileFilename());
            }
        }
    }

    private void readUploadFile(BinaryData uploadFileBinary, UploadFileFilenameProcessor uploadFileFilenameProcessor) {

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
                    processUploadFileObject(uploadFileFilenameProcessor, armResponseUploadFileRecord);
                } else {
                    log.warn("Failed to write upload file to temp workspace {}", uploadFileFilenameProcessor.getUploadFileFilename());
                }
            } catch (IOException e) {
                log.error("Unable to write upload file to temporary workspace {} - {}", uploadFileFilenameProcessor.getUploadFileFilename(), e.getMessage());
            } catch (Exception e) {
                log.error("Unable to process arm response upload file {} - {}", uploadFileFilenameProcessor.getUploadFileFilename(), e.getMessage());
            } finally {
                cleanupTemporaryJsonFile(jsonPath);
            }
        } else {
            log.warn("Failed to read upload file {}", uploadFileFilenameProcessor.getUploadFileFilename());
        }
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

    private void processUploadFileObject(UploadFileFilenameProcessor uploadFileFilenameProcessor,
                                         ArmResponseUploadFileRecord armResponseUploadFileRecord) {
        if (nonNull(armResponseUploadFileRecord)) {
            ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntityById(armResponseUploadFileRecord.getA360RecordId());

            //If the filename contains 1
            if (ARM_RESPONSE_SUCCESS_STATUS_CODE.equals(uploadFileFilenameProcessor.getStatus())) {
                if (nonNull(externalObjectDirectory)) {
                    processUploadFileDataSuccess(externalObjectDirectory, armResponseUploadFileRecord);
                } else {
                    log.warn(
                        "Unable to process upload file {} with EOD record {}, file Id {}",
                        uploadFileFilenameProcessor.getUploadFileFilename(),
                        armResponseUploadFileRecord.getA360RecordId(),
                        armResponseUploadFileRecord.getA360FileId()
                    );
                }
            } else {
                //Read the upload file and log the error code and description with EOD
                String errorDescription = StringUtils.isNotEmpty(armResponseUploadFileRecord.getExceptionDescription())
                    ? armResponseUploadFileRecord.getExceptionDescription() : "No error details found in response file";

                log.warn(
                    "ARM status is failed for upload file {}. ARM error description: {} ARM error status: {} for record {}, file Id {}",
                    uploadFileFilenameProcessor.getUploadFileFilename(),
                    errorDescription,
                    armResponseUploadFileRecord.getErrorStatus(),
                    armResponseUploadFileRecord.getA360RecordId(),
                    armResponseUploadFileRecord.getA360FileId()
                );
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
            }
        } else {
            log.warn("Unable to read upload file {}", uploadFileFilenameProcessor.getUploadFileFilename());
        }
    }

    private void processUploadFileDataSuccess(ExternalObjectDirectoryEntity externalObjectDirectory,
                                              ArmResponseUploadFileRecord armResponseUploadFileRecord) {
        // Validate the upload file checksum against the external object directory tables object Media, TranscriptionDocument, AnnotationDocument
        // or CaseDocument
        if (nonNull(externalObjectDirectory.getMedia())) {
            MediaEntity media = externalObjectDirectory.getMedia();
            if (nonNull(media.getChecksum())) {
                verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, media.getChecksum());
            } else {
                log.warn("Unable to verify media checksum for external object {}", externalObjectDirectory.getId());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseChecksumVerificationFailedStatus);
            }
        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {
            verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory,
                                          externalObjectDirectory.getTranscriptionDocumentEntity().getChecksum());
        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {
            verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory,
                                          externalObjectDirectory.getAnnotationDocumentEntity().getChecksum());
        } else if (nonNull(externalObjectDirectory.getCaseDocument())) {
            verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory,
                                          externalObjectDirectory.getCaseDocument().getChecksum());
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

    private static ResponseFilenames getArmResponseFilenames(List<String> responseFiles) {
        ResponseFilenames responseFilenames = new ResponseFilenames();
        for (String responseFile : responseFiles) {
            try {
                if (responseFile.endsWith(generateSuffix(ARM_CREATE_RECORD_FILENAME_KEY))) {
                    CreateRecordFilenameProcessor createRecordFilenameProcessor = new CreateRecordFilenameProcessor(responseFile);
                    responseFilenames.getCreateRecordResponses().add(createRecordFilenameProcessor);
                } else if (responseFile.endsWith(generateSuffix(ARM_UPLOAD_FILE_FILENAME_KEY))) {
                    UploadFileFilenameProcessor uploadFileFilenameProcessor = new UploadFileFilenameProcessor(responseFile);
                    responseFilenames.getUploadFileResponses().add(uploadFileFilenameProcessor);
                } else if (responseFile.endsWith(generateSuffix(ARM_INVALID_LINE_FILENAME_KEY))) {
                    InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor = new InvalidLineFileFilenameProcessor(responseFile);
                    responseFilenames.getInvalidLineResponses().add(invalidLineFileFilenameProcessor);
                } else {
                    log.warn("Unknown ARM response file type {}", responseFile);
                }
            } catch (IllegalArgumentException e) {
                // This occurs when the filename is not parsable
                log.error("Invalid ARM response filename: {}", responseFile);
            }
        }
        return responseFilenames;
    }

    private void processInvalidFiles(List<InvalidLineFileFilenameProcessor> invalidLineResponses) {
        for (InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor : invalidLineResponses) {
            try {
                BinaryData invalidLineFileBinary = armDataManagementApi.getBlobData(invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
                readInvalidLineFile(invalidLineFileBinary, invalidLineFileFilenameProcessor);
            } catch (Exception e) {
                log.error("Unable to process ARM invalid line file {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
            }
        }
    }

    private void readInvalidLineFile(BinaryData invalidLineFileBinary, InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor) {
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
                    processInvalidLineFileObject(invalidLineFileFilenameProcessor, armResponseInvalidLineRecord);
                } else {
                    log.warn("Failed to write invalid line file to temp workspace {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
                }
            } catch (IOException e) {
                log.error("Unable to write invalid line file to temporary workspace {} - {}",
                          invalidLineFileFilenameProcessor.getInvalidLineFileFilename(), e.getMessage());

            } catch (Exception e) {
                log.error("Unable to process ARM response invalid line file {} - {}",
                          invalidLineFileFilenameProcessor.getInvalidLineFileFilename(), e.getMessage());

            } finally {
                cleanupTemporaryJsonFile(jsonPath);
            }
        } else {
            log.error("Unable to read ARM response invalid line file {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
        }
    }

    private void processInvalidLineFileObject(InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor,
                                              ArmResponseInvalidLineRecord armResponseInvalidLineRecord) {
        if (nonNull(armResponseInvalidLineRecord)) {
            ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntityById(armResponseInvalidLineRecord.getA360RecordId());

            //If the filename contains 0
            if (ARM_RESPONSE_INVALID_STATUS_CODE.equals(invalidLineFileFilenameProcessor.getStatus())) {
                if (nonNull(externalObjectDirectory)) {
                    //Read the invalid lines file and log the error code and description with EOD
                    log.warn(
                        "ARM invalid line for external object id {}. ARM error description: {} ARM error status: {}",
                        externalObjectDirectory.getId(),
                        armResponseInvalidLineRecord.getExceptionDescription(),
                        armResponseInvalidLineRecord.getErrorStatus()
                    );
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseManifestFailedStatus);
                } else {
                    log.warn(
                        "Unable to process invalid line file {} with EOD record {}",
                        invalidLineFileFilenameProcessor.getInvalidLineFileFilename(),
                        armResponseInvalidLineRecord.getA360RecordId()
                    );
                }

            } else {
                log.warn("Incorrect status [{}] for invalid line file {}", invalidLineFileFilenameProcessor.getStatus(),
                         invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, armResponseProcessingFailedStatus);
            }
        } else {
            log.warn("Unable to read invalid line file {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
        }
    }

    private String getContinuationToken() {
        return null;
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


    private ExternalObjectDirectoryEntity getExternalObjectDirectoryEntityById(String eodIdString) {
        if (StringUtils.isNotEmpty(eodIdString)) {
            try {
                Integer eodId = Integer.parseInt(eodIdString);
                if (nonNull(eodId)) {
                    Optional<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityOptional = externalObjectDirectoryRepository.findById(eodId);
                    if (externalObjectDirectoryEntityOptional.isPresent()) {
                        return externalObjectDirectoryEntityOptional.get();
                    }
                }
            } catch (NumberFormatException e) {
                log.error("Unable to get EOD from {} - {}", eodIdString, e.getMessage());
            }
        }
        return null;
    }

    private void updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                     ObjectRecordStatusEntity objectRecordStatus) {
        if (nonNull(externalObjectDirectory)) {
            log.info(
                "ARM Push updating ARM status from {} to {} for ID {}",
                externalObjectDirectory.getStatus().getDescription(),
                objectRecordStatus.getDescription(),
                externalObjectDirectory.getId()
            );
            externalObjectDirectory.setStatus(objectRecordStatus);
            externalObjectDirectory.setLastModifiedBy(userAccount);
            externalObjectDirectory.setLastModifiedDateTime(currentTimeHelper.currentOffsetDateTime());
            externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
        }
    }
}
