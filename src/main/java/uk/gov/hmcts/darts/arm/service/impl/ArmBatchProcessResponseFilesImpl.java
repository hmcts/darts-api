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
import uk.gov.hmcts.darts.arm.model.blobs.ArmBatchResponses;
import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseCreateRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInvalidLineRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.service.ArmBatchProcessResponseFiles;
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.util.files.BatchUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.InvalidLineFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_CREATE_RECORD_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_INVALID_LINE_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_RESPONSE_INVALID_STATUS_CODE;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_RESPONSE_SUCCESS_STATUS_CODE;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_UPLOAD_FILE_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArmConstants.ArmBatching.PROCESS_SINGLE_RECORD_BATCH_SIZE;
import static uk.gov.hmcts.darts.arm.util.ArmResponseFilesHelper.generateSuffix;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmBatchProcessResponseFilesImpl implements ArmBatchProcessResponseFiles {

    public static final String UNABLE_TO_UPDATE_EOD = "Unable to update EOD";
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ArmDataManagementApi armDataManagementApi;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ObjectMapper objectMapper;
    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;
    private final ArmResponseFilesProcessor armResponseFilesProcessor;
    private final ExternalObjectDirectoryService externalObjectDirectoryService;
    private final MediaRepository mediaRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final AnnotationDocumentRepository annotationDocumentRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private UserAccountEntity userAccount;

    @Override
    public void batchProcessResponseFiles() {
        userAccount = userIdentity.getUserAccount();
        Integer batchSize = armDataManagementConfiguration.getBatchSize();
        if (PROCESS_SINGLE_RECORD_BATCH_SIZE.equals(batchSize)) {
            armResponseFilesProcessor.processResponseFiles();
        } else if (batchSize > PROCESS_SINGLE_RECORD_BATCH_SIZE) {
            batchProcessResponseFilesFromAzure();
        } else {
            log.warn("Invalid batch size {}. Unable to process ARM pull responses");
        }
    }

    private void batchProcessResponseFilesFromAzure() {
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
            for (String inputUploadBlob : continuationTokenBlobs.getBlobNamesAndPaths()) {
                processInputUploadBlob(inputUploadBlob);
            }
        } else {
            log.warn("No response files found with prefix: {}", prefix);
        }
    }

    private void processInputUploadBlob(String inputUploadBlob) {
        log.debug("Found ARM Input Upload file {}", inputUploadBlob);
        try {
            BatchUploadFileFilenameProcessor batchUploadFileFilenameProcessor = new BatchUploadFileFilenameProcessor(inputUploadBlob);
            String manifestName = generateManifestName(batchUploadFileFilenameProcessor.getUuidString());
            List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = externalObjectDirectoryRepository
                .findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestName);
            if (CollectionUtils.isNotEmpty(externalObjectDirectoryEntities)) {
                externalObjectDirectoryService.updateStatus(
                    EodHelper.armProcessingResponseFilesStatus(),
                    userAccount,
                    externalObjectDirectoryEntities.stream().map(ExternalObjectDirectoryEntity::getId).toList(),
                    currentTimeHelper.currentOffsetDateTime());

            } else {
                log.warn("No external object directories found with filename: {}", batchUploadFileFilenameProcessor.getBatchMetadataFilename());
            }

            processResponseFileByHashcode(batchUploadFileFilenameProcessor);

            resetArmStatusForUnprocessedEods(manifestName);
        } catch (IllegalArgumentException e) {
            log.error("Unable to process manifest filename {}", inputUploadBlob, e);
        } catch (Exception e) {
            log.error("Unable to process manifest", e);
        }
    }

    private String generateManifestName(String uuid) {
        var fileNameFormat = "%s_%s.%s";
        return String.format(fileNameFormat,
                             armDataManagementConfiguration.getManifestFilePrefix(),
                             uuid,
                             armDataManagementConfiguration.getFileExtension()
        );
    }

    private void resetArmStatusForUnprocessedEods(String manifestName) {
        List<ExternalObjectDirectoryEntity> unprocessedExternalObjectDirectoryEntities = externalObjectDirectoryRepository
            .findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestName);
        if (CollectionUtils.isNotEmpty(unprocessedExternalObjectDirectoryEntities)) {
            externalObjectDirectoryService.updateStatus(
                EodHelper.armDropZoneStatus(),
                userAccount,
                unprocessedExternalObjectDirectoryEntities.stream().map(ExternalObjectDirectoryEntity::getId).toList(),
                currentTimeHelper.currentOffsetDateTime());
            for (ExternalObjectDirectoryEntity unprocessedExternalObjectDirectoryEntity : unprocessedExternalObjectDirectoryEntities) {
                log.warn("Unable to process ARM responses for EOD {}", unprocessedExternalObjectDirectoryEntity.getId());
            }
        }
    }

    private void processResponseFileByHashcode(BatchUploadFileFilenameProcessor batchUploadFileFilenameProcessor) {
        try {
            List<String> responseFiles = armDataManagementApi.listResponseBlobs(batchUploadFileFilenameProcessor.getHashcode());
            if (CollectionUtils.isNotEmpty(responseFiles)) {
                ResponseFilenames responseFilenames = getArmResponseFilenames(responseFiles);
                ArmBatchResponses armBatchResponses = new ArmBatchResponses();
                if (responseFilenames.containsResponses()) {
                    //Put response files into their respective groups based on the contents relation id (EOD id)
                    processCreateRecordResponseFiles(responseFilenames.getCreateRecordResponses(), armBatchResponses);
                    processUploadResponseFiles(responseFilenames.getUploadFileResponses(), armBatchResponses);
                    processInvalidFiles(responseFilenames.getInvalidLineResponses(), armBatchResponses);
                    //Process the final results
                    processResponseFiles(armBatchResponses);
                }
            }
        } catch (Exception e) {
            log.error("Unable to process responses for file {}", batchUploadFileFilenameProcessor.getBatchMetadataFilenameAndPath(), e);
        }
    }

    private void processResponseFiles(ArmBatchResponses armBatchResponses) {
        armBatchResponses.getArmBatchResponses().values().forEach(
            armResponseBatchData -> {
                if (nonNull(armResponseBatchData.getInvalidLineFileFilenameProcessor())
                    && (nonNull(armResponseBatchData.getCreateRecordFilenameProcessor())
                    || nonNull(armResponseBatchData.getArmResponseUploadFileRecord()))) {

                    processInvalidLineFileObject(armResponseBatchData.getExternalObjectDirectoryId(),
                                                 armResponseBatchData.getInvalidLineFileFilenameProcessor(),
                                                 armResponseBatchData.getArmResponseInvalidLineRecord());
                    deleteResponseBlobs(armResponseBatchData);
                } else if (nonNull(armResponseBatchData.getCreateRecordFilenameProcessor())
                    && nonNull(armResponseBatchData.getUploadFileFilenameProcessor())) {

                    processUploadFileObject(armResponseBatchData.getExternalObjectDirectoryId(),
                                            armResponseBatchData.getUploadFileFilenameProcessor(),
                                            armResponseBatchData.getArmResponseUploadFileRecord());
                    deleteResponseBlobs(armResponseBatchData);
                } else {
                    log.info("Unable to find response files for external object {}", armResponseBatchData.getExternalObjectDirectoryId());
                    try {
                        ExternalObjectDirectoryEntity externalObjectDirectory =
                            getExternalObjectDirectoryEntity(armResponseBatchData.getExternalObjectDirectoryId());

                        updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armDropZoneStatus());
                    } catch (Exception e) {
                        log.error(UNABLE_TO_UPDATE_EOD, e);
                    }
                }
            }
        );
    }

    private void processCreateRecordResponseFiles(List<CreateRecordFilenameProcessor> createRecordResponses,
                                                  ArmBatchResponses armBatchResponses) {
        for (CreateRecordFilenameProcessor createRecordFilenameProcessor : createRecordResponses) {
            try {
                BinaryData createRecordBinary = armDataManagementApi.getBlobData(createRecordFilenameProcessor.getCreateRecordFilename());
                readCreateRecordFile(createRecordBinary, createRecordFilenameProcessor, armBatchResponses);
            } catch (Exception e) {
                log.error("Unable to process upload file {}", createRecordFilenameProcessor.getCreateRecordFilename());
            }
        }
    }

    private void readCreateRecordFile(BinaryData createRecordBinary, CreateRecordFilenameProcessor createRecordFilenameProcessor,
                                      ArmBatchResponses armBatchResponses) {
        if (nonNull(createRecordBinary)) {
            Path jsonPath = null;
            try {
                boolean appendUuidToWorkspace = true;
                jsonPath = fileOperationService.saveBinaryDataToSpecifiedWorkspace(
                    createRecordBinary,
                    createRecordFilenameProcessor.getCreateRecordFilename(),
                    armDataManagementConfiguration.getTempBlobWorkspace(),
                    appendUuidToWorkspace
                );

                if (nonNull(jsonPath) && jsonPath.toFile().exists()) {
                    ArmResponseCreateRecord armResponseCreateRecord = objectMapper.readValue(jsonPath.toFile(), ArmResponseCreateRecord.class);
                    UploadNewFileRecord uploadNewFileRecord = readInputJson(armResponseCreateRecord.getInput());
                    if (nonNull(uploadNewFileRecord)) {
                        armBatchResponses.addResponseBatchData(Integer.valueOf(uploadNewFileRecord.getRelationId()),
                                                               armResponseCreateRecord, createRecordFilenameProcessor);
                    } else {
                        log.warn("Failed to obtain relation id from create record");
                    }
                } else {
                    log.warn("Failed to write create record file to temp workspace {}", createRecordFilenameProcessor.getCreateRecordFilename());
                }
            } catch (IOException e) {
                log.error("Unable to write create record file to temporary workspace {} - {}", createRecordFilenameProcessor.getCreateRecordFilename(),
                          e.getMessage());
            } catch (Exception e) {
                log.error("Unable to process arm response create record file {} - {}", createRecordFilenameProcessor.getCreateRecordFilename(), e.getMessage());
            } finally {
                cleanupTemporaryJsonFile(jsonPath);
            }
        } else {
            log.warn("Failed to read create record file {}", createRecordFilenameProcessor.getCreateRecordFilename());
        }

    }

    private void processUploadResponseFiles(List<UploadFileFilenameProcessor> uploadFileResponses, ArmBatchResponses armBatchResponses) {
        for (UploadFileFilenameProcessor uploadFileFilenameProcessor : uploadFileResponses) {
            try {
                BinaryData uploadFileBinary = armDataManagementApi.getBlobData(uploadFileFilenameProcessor.getUploadFileFilename());
                readUploadFile(uploadFileBinary, uploadFileFilenameProcessor, armBatchResponses);
            } catch (Exception e) {
                log.error("Unable to process upload file {}", uploadFileFilenameProcessor.getUploadFileFilename());
            }
        }
    }

    private void readUploadFile(BinaryData uploadFileBinary, UploadFileFilenameProcessor uploadFileFilenameProcessor,
                                ArmBatchResponses armBatchResponses) {

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
                    UploadNewFileRecord uploadNewFileRecord = readInputJson(armResponseUploadFileRecord.getInput());
                    if (nonNull(uploadNewFileRecord)) {
                        armBatchResponses.addResponseBatchData(Integer.valueOf(uploadNewFileRecord.getRelationId()),
                                                               armResponseUploadFileRecord, uploadFileFilenameProcessor);
                    }
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

    private void processUploadFileObject(int externalObjectDirectoryId, UploadFileFilenameProcessor uploadFileFilenameProcessor,
                                         ArmResponseUploadFileRecord armResponseUploadFileRecord) {
        try {
            ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntity(externalObjectDirectoryId);
            if (nonNull(armResponseUploadFileRecord)) {

                //If the filename contains 1
                if (ARM_RESPONSE_SUCCESS_STATUS_CODE.equals(uploadFileFilenameProcessor.getStatus())) {
                    if (nonNull(externalObjectDirectory)) {
                        processUploadFileDataSuccess(externalObjectDirectory, armResponseUploadFileRecord);
                    } else {
                        log.warn(
                            "Unable to process upload file {} with EOD record {}, file Id {}", uploadFileFilenameProcessor.getUploadFileFilename(),
                            armResponseUploadFileRecord.getA360RecordId(), armResponseUploadFileRecord.getA360FileId());
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
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus());
                }
            } else {
                log.warn("Unable to read upload file {}", uploadFileFilenameProcessor.getUploadFileFilename());
            }
        } catch (Exception e) {
            log.error(UNABLE_TO_UPDATE_EOD, e);
        }
    }


    private void processUploadFileDataSuccess(ExternalObjectDirectoryEntity externalObjectDirectory,
                                              ArmResponseUploadFileRecord armResponseUploadFileRecord) {
        // Validate the upload file checksum against the external object directory tables object Media, TranscriptionDocument, AnnotationDocument
        // or CaseDocument
        if (nonNull(externalObjectDirectory.getMedia())) {

            int mediaId = externalObjectDirectory.getMedia().getId();
            MediaEntity mediaEntity = mediaRepository.findById(mediaId).orElseThrow();
            if (nonNull(mediaEntity.getChecksum())) {
                verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, mediaEntity.getChecksum());
            } else {
                log.warn("Unable to verify media checksum for external object {}", externalObjectDirectory.getId());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseChecksumVerificationFailedStatus());
            }

        } else if (nonNull(externalObjectDirectory.getTranscriptionDocumentEntity())) {

            int transcriptionDocumentId = externalObjectDirectory.getTranscriptionDocumentEntity().getId();
            TranscriptionDocumentEntity transcriptionDocumentEntity = transcriptionDocumentRepository.findById(transcriptionDocumentId).orElseThrow();
            verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, transcriptionDocumentEntity.getChecksum());

        } else if (nonNull(externalObjectDirectory.getAnnotationDocumentEntity())) {

            int annotationDocumentId = externalObjectDirectory.getAnnotationDocumentEntity().getId();
            AnnotationDocumentEntity annotationDocumentEntity = annotationDocumentRepository.findById(annotationDocumentId).orElseThrow();
            verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, annotationDocumentEntity.getChecksum());

        } else if (nonNull(externalObjectDirectory.getCaseDocument())) {

            int caseDocumentId = externalObjectDirectory.getCaseDocument().getId();
            CaseDocumentEntity caseDocumentEntity = caseDocumentRepository.findById(caseDocumentId).orElseThrow();
            verifyChecksumAndUpdateStatus(armResponseUploadFileRecord, externalObjectDirectory, caseDocumentEntity.getChecksum());

        } else {
            updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus());
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
                updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.storedStatus());
            }
        } else {
            log.warn("External object id {} checksum differs. Arm checksum: {} Object Checksum: {}",
                     externalObjectDirectory.getId(),
                     armResponseUploadFileRecord.getMd5(), objectChecksum);
            externalObjectDirectory.setErrorCode(armResponseUploadFileRecord.getErrorStatus());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseChecksumVerificationFailedStatus());
        }
    }

    private UploadNewFileRecord readInputJson(String input) {
        UploadNewFileRecord uploadNewFileRecord = null;
        if (StringUtils.isNotEmpty(input)) {
            String unescapedJson = StringEscapeUtils.unescapeJson(input);
            try {
                uploadNewFileRecord = objectMapper.readValue(unescapedJson, UploadNewFileRecord.class);
            } catch (JsonMappingException e) {
                log.error("Unable to map the input field {}", e.getMessage());
            } catch (JsonProcessingException e) {
                log.error("Unable to parse the upload new file record {}", e.getMessage());
            }
        } else {
            log.warn("Unable to parse the input field upload new file record");
        }
        return uploadNewFileRecord;
    }

    private UploadNewFileRecord readInputJson(ExternalObjectDirectoryEntity externalObjectDirectory, String input) {
        UploadNewFileRecord uploadNewFileRecord = null;
        if (StringUtils.isNotEmpty(input)) {
            String unescapedJson = StringEscapeUtils.unescapeJson(input);
            try {
                uploadNewFileRecord = objectMapper.readValue(unescapedJson, UploadNewFileRecord.class);
            } catch (JsonMappingException e) {
                log.error("Unable to map the upload record file input field");
                updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus());
            } catch (JsonProcessingException e) {
                log.error("Unable to parse the upload record file ");
                updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus());
            }
        } else {
            log.warn("Unable to get the upload record file input field");
            updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus());
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

    private void processInvalidFiles(List<InvalidLineFileFilenameProcessor> invalidLineResponses, ArmBatchResponses armBatchResponses) {
        for (InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor : invalidLineResponses) {
            try {
                BinaryData invalidLineFileBinary = armDataManagementApi.getBlobData(invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
                readInvalidLineFile(invalidLineFileBinary, invalidLineFileFilenameProcessor, armBatchResponses);
            } catch (Exception e) {
                log.error("Unable to process ARM invalid line file {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
            }
        }
    }

    private void readInvalidLineFile(BinaryData invalidLineFileBinary, InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor,
                                     ArmBatchResponses armBatchResponses) {
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
                    UploadNewFileRecord uploadNewFileRecord = readInputJson(armResponseInvalidLineRecord.getInput());
                    if (nonNull(uploadNewFileRecord)) {
                        armBatchResponses.addResponseBatchData(Integer.valueOf(uploadNewFileRecord.getRelationId()),
                                                               armResponseInvalidLineRecord, invalidLineFileFilenameProcessor);
                    }
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

    private void processInvalidLineFileObject(int externalObjectDirectoryId, InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor,
                                              ArmResponseInvalidLineRecord armResponseInvalidLineRecord) {
        try {
            ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntity(externalObjectDirectoryId);
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
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseManifestFailedStatus());

                } else {
                    log.warn("Incorrect status [{}] for invalid line file {}", invalidLineFileFilenameProcessor.getStatus(),
                             invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus());
                }
            } else {
                log.warn("Unable to read invalid line file {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilename());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus());
            }
        } catch (Exception e) {
            log.error(UNABLE_TO_UPDATE_EOD, e);
        }
    }

    void deleteResponseBlobs(ArmResponseBatchData armResponseBatchData) {
        List<Boolean> deletedResponseBlobStatuses = new ArrayList<>();
        List<String> responseBlobsToBeDeleted = getResponseBlobsToBeDeleted(armResponseBatchData);
        ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntity(armResponseBatchData.getExternalObjectDirectoryId());
        if (nonNull(externalObjectDirectory) && responseBlobsToBeDeleted.size() == 2) {
            ObjectRecordStatusEnum status = ObjectRecordStatusEnum.valueOfId(externalObjectDirectory.getStatus().getId());
            if (STORED.equals(status) || ARM_RESPONSE_PROCESSING_FAILED.equals(status) || ARM_RESPONSE_MANIFEST_FAILED.equals(status)) {

                deletedResponseBlobStatuses = responseBlobsToBeDeleted.stream()
                    .map(armDataManagementApi::deleteBlobData)
                    .toList();

                if (deletedResponseBlobStatuses.size() == 2 && !deletedResponseBlobStatuses.contains(false)) {
                    externalObjectDirectory.setResponseCleaned(true);
                    externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
                } else {
                    log.warn("Unable to successfully delete the response files for EOD {} ", externalObjectDirectory.getId());
                }
            }
        }
    }

    private static List<String> getResponseBlobsToBeDeleted(ArmResponseBatchData armResponseBatchData) {
        List<String> responseBlobsToBeDeleted = new ArrayList<>();
        if (nonNull(armResponseBatchData.getCreateRecordFilenameProcessor())) {
            responseBlobsToBeDeleted.add(armResponseBatchData.getCreateRecordFilenameProcessor().getCreateRecordFilename());
        }
        if (nonNull(armResponseBatchData.getUploadFileFilenameProcessor())) {
            responseBlobsToBeDeleted.add(armResponseBatchData.getUploadFileFilenameProcessor().getUploadFileFilename());
        }
        if (nonNull(armResponseBatchData.getInvalidLineFileFilenameProcessor())) {
            responseBlobsToBeDeleted.add(armResponseBatchData.getInvalidLineFileFilenameProcessor().getInvalidLineFileFilename());
        }
        return responseBlobsToBeDeleted;
    }


    private String getContinuationToken() {
        return null;
    }

    private ExternalObjectDirectoryEntity getExternalObjectDirectoryEntity(Integer eodId) {
        return externalObjectDirectoryService.eagerLoadExternalObjectDirectory(eodId).orElseThrow();
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
