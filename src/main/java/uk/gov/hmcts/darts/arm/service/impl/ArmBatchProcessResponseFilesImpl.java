package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
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
import uk.gov.hmcts.darts.arm.service.ArmResponseFilesProcessor;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.InvalidLineFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_CREATE_RECORD_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_INVALID_LINE_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_RESPONSE_INVALID_STATUS_CODE;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_RESPONSE_SUCCESS_STATUS_CODE;
import static uk.gov.hmcts.darts.arm.util.ArchiveConstants.ArchiveResponseFileAttributes.ARM_UPLOAD_FILE_FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.ArmResponseFilesUtil.generateSuffix;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;


@Slf4j
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.CouplingBetweenObjects"})
public class ArmBatchProcessResponseFilesImpl implements ArmResponseFilesProcessor {

    private static final String UNABLE_TO_UPDATE_EOD = "Unable to update EOD";
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ArmDataManagementApi armDataManagementApi;
    private final FileOperationService fileOperationService;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;
    private final ObjectMapper objectMapper;
    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;
    private final ExternalObjectDirectoryService externalObjectDirectoryService;
    private final Integer batchSize;
    private final LogApi logApi;

    private UserAccountEntity userAccount;
    private String continuationToken;

    public ArmBatchProcessResponseFilesImpl(ExternalObjectDirectoryRepository externalObjectDirectoryRepository, ArmDataManagementApi armDataManagementApi,
                                            FileOperationService fileOperationService, ArmDataManagementConfiguration armDataManagementConfiguration,
                                            ObjectMapper objectMapper, UserIdentity userIdentity, CurrentTimeHelper currentTimeHelper,
                                            ExternalObjectDirectoryService externalObjectDirectoryService, Integer batchSize,
                                            LogApi logApi) {
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.armDataManagementApi = armDataManagementApi;
        this.fileOperationService = fileOperationService;
        this.armDataManagementConfiguration = armDataManagementConfiguration;
        this.objectMapper = objectMapper;
        this.userIdentity = userIdentity;
        this.currentTimeHelper = currentTimeHelper;
        this.externalObjectDirectoryService = externalObjectDirectoryService;
        this.batchSize = batchSize;
        this.logApi = logApi;
    }

    @Override
    public void processResponseFiles() {
        userAccount = userIdentity.getUserAccount();
        ContinuationTokenBlobs continuationTokenBlobs = null;
        String prefix = armDataManagementConfiguration.getManifestFilePrefix();
        int maxContinuationBatchSize = armDataManagementConfiguration.getMaxContinuationBatchSize();

        try {
            log.info("About to look for files starting with prefix: {}", prefix);
            String continuationToken = null;
            for (int pageSize = 0; pageSize < batchSize; pageSize += maxContinuationBatchSize) {

                ContinuationTokenBlobs continuationTokenData = armDataManagementApi.listResponseBlobsUsingMarker(prefix, maxContinuationBatchSize, continuationToken);
                if (nonNull(continuationTokenData)) {
                    if (isNull(continuationToken)) {
                        continuationTokenBlobs = continuationTokenData;
                    } else {
                        continuationTokenBlobs.getBlobNamesAndPaths().addAll(continuationTokenData.getBlobNamesAndPaths());
                    }
                    continuationToken = continuationTokenData.getContinuationToken();
                }
                // if no more data found break out of loop
                if (isNull(continuationToken)) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Unable to find response file for prefix: {} - {}", prefix, e.getMessage());
        }
        if (nonNull(continuationTokenBlobs) && CollectionUtils.isNotEmpty(continuationTokenBlobs.getBlobNamesAndPaths())) {
            for (String inputUploadBlob : continuationTokenBlobs.getBlobNamesAndPaths()) {
                Instant start = Instant.now();
                log.info("ARM PERFORMANCE PULL START for manifest {} started at {}", inputUploadBlob, start);
                processInputUploadBlob(inputUploadBlob);
                Instant finish = Instant.now();
                long timeElapsed = Duration.between(start, finish).toMillis();
                log.info("ARM PERFORMANCE PULL END for manifest {} ended at {}", inputUploadBlob, finish);
                log.info("ARM PERFORMANCE PULL ELAPSED TIME for manifest {} took {} ms", inputUploadBlob, timeElapsed);
            }
        } else {
            log.warn("No response files found with prefix: {}", prefix);
        }
    }

    private void processInputUploadBlob(String inputUploadBlob) {
        log.debug("Found ARM Input Upload file {}", inputUploadBlob);
        try {
            BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor = new BatchInputUploadFileFilenameProcessor(inputUploadBlob);
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

            processResponseFileByHashcode(batchUploadFileFilenameProcessor, manifestName);
            deleteResponseBlobsByManifestName(batchUploadFileFilenameProcessor, manifestName);
            resetArmStatusForUnprocessedEods(manifestName);
        } catch (IllegalArgumentException e) {
            log.error("Unable to process manifest filename {}", inputUploadBlob, e);
        } catch (Exception e) {
            log.error("Unable to process manifest", e);
        }
    }

    private void deleteResponseBlobsByManifestName(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor,
                                                   String manifestName) {
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = externalObjectDirectoryRepository.findByManifestFile(manifestName);
        if (CollectionUtils.isNotEmpty(externalObjectDirectoryEntities)) {
            List<ExternalObjectDirectoryEntity> completedExternalObjectDirectoryEntities = new ArrayList<>();
            for (ExternalObjectDirectoryEntity eod : externalObjectDirectoryEntities) {
                if (isResponseCompletedAndCleaned(eod)) {
                    completedExternalObjectDirectoryEntities.add(eod);
                }
            }
            if (externalObjectDirectoryEntities.size() == completedExternalObjectDirectoryEntities.size()) {
                log.info("About to delete ARM input upload file {}", batchUploadFileFilenameProcessor.getBatchMetadataFilename());
                armDataManagementApi.deleteBlobData(batchUploadFileFilenameProcessor.getBatchMetadataFilenameAndPath());
            } else {
                log.warn("Unable to delete ARM batch input upload file {} as referenced data is not complete - total {} vs completed {}",
                         batchUploadFileFilenameProcessor.getBatchMetadataFilename(),
                         externalObjectDirectoryEntities.size(), completedExternalObjectDirectoryEntities.size());
            }
        }
    }

    private boolean isResponseCompletedAndCleaned(ExternalObjectDirectoryEntity externalObjectDirectory) {
        return externalObjectDirectory.isResponseCleaned()
            && isCompletedStatus(externalObjectDirectory.getStatus());
    }

    private boolean isCompletedStatus(ObjectRecordStatusEntity status) {
        if (nonNull(status)) {
            ObjectRecordStatusEnum statusEnum = ObjectRecordStatusEnum.valueOfId(status.getId());
            return STORED.equals(statusEnum)
                || ARM_RESPONSE_PROCESSING_FAILED.equals(statusEnum)
                || ARM_RESPONSE_MANIFEST_FAILED.equals(statusEnum)
                || ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.equals(statusEnum);
        }
        return false;
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

    private void processResponseFileByHashcode(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor, String manifestName) {
        try {
            List<String> responseFiles = armDataManagementApi.listResponseBlobs(batchUploadFileFilenameProcessor.getHashcode());
            if (CollectionUtils.isNotEmpty(responseFiles)) {
                ResponseFilenames responseFilenames = getArmResponseFilenames(responseFiles, manifestName);
                ArmBatchResponses armBatchResponses = new ArmBatchResponses();
                if (responseFilenames.containsResponses()) {
                    //Put response files into their respective groups based on the contents relation id (EOD id)
                    processCreateRecordResponseFiles(responseFilenames.getCreateRecordResponses(), armBatchResponses);
                    processUploadResponseFiles(responseFilenames.getUploadFileResponses(), armBatchResponses);
                    processInvalidFiles(responseFilenames.getInvalidLineResponses(), armBatchResponses);
                    //Process the final results
                    processBatchResponseFiles(armBatchResponses);
                }
            } else {
                log.info("Unable to find response files starting with {} for manifest {}", batchUploadFileFilenameProcessor.getHashcode(), manifestName);
            }
        } catch (Exception e) {
            log.error("Unable to process responses for file {}", batchUploadFileFilenameProcessor.getBatchMetadataFilenameAndPath(), e);
        }
    }

    private void processBatchResponseFiles(ArmBatchResponses armBatchResponses) {
        armBatchResponses.getArmBatchResponseMap().values().forEach(
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
                BinaryData createRecordBinary = armDataManagementApi.getBlobData(createRecordFilenameProcessor.getCreateRecordFilenameAndPath());
                readCreateRecordFile(createRecordBinary, createRecordFilenameProcessor, armBatchResponses);
            } catch (Exception e) {
                log.error("Unable to process ARM create record response file {}", createRecordFilenameProcessor.getCreateRecordFilenameAndPath());
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
                    logResponseFileContents(jsonPath);
                    ArmResponseCreateRecord armResponseCreateRecord = objectMapper.readValue(jsonPath.toFile(), ArmResponseCreateRecord.class);
                    UploadNewFileRecord uploadNewFileRecord = readInputJson(armResponseCreateRecord.getInput());
                    if (nonNull(uploadNewFileRecord)) {
                        armBatchResponses.addResponseBatchData(Integer.valueOf(uploadNewFileRecord.getRelationId()),
                                                               armResponseCreateRecord, createRecordFilenameProcessor);
                    } else {
                        log.warn("Failed to obtain relation id from create record");
                    }
                } else {
                    log.warn("Failed to write create record file to temp workspace {}", createRecordFilenameProcessor.getCreateRecordFilenameAndPath());
                }
            } catch (IOException e) {
                log.error("Unable to write create record file to temporary workspace {} - {}", createRecordFilenameProcessor.getCreateRecordFilenameAndPath(),
                          e.getMessage());
            } catch (Exception e) {
                log.error("Unable to process arm response create record file {}", createRecordFilenameProcessor.getCreateRecordFilenameAndPath(), e);
            } finally {
                cleanupTemporaryJsonFile(jsonPath);
            }
        } else {
            log.warn("Failed to read create record file {}", createRecordFilenameProcessor.getCreateRecordFilenameAndPath());
        }

    }

    private void processUploadResponseFiles(List<UploadFileFilenameProcessor> uploadFileResponses, ArmBatchResponses armBatchResponses) {
        for (UploadFileFilenameProcessor uploadFileFilenameProcessor : uploadFileResponses) {
            try {
                BinaryData uploadFileBinary = armDataManagementApi.getBlobData(uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
                readUploadFile(uploadFileBinary, uploadFileFilenameProcessor, armBatchResponses);
            } catch (Exception e) {
                log.error("Unable to process ARM response upload file {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath(), e);
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
                    logResponseFileContents(jsonPath);
                    ArmResponseUploadFileRecord armResponseUploadFileRecord = objectMapper.readValue(jsonPath.toFile(), ArmResponseUploadFileRecord.class);
                    UploadNewFileRecord uploadNewFileRecord = readInputJson(armResponseUploadFileRecord.getInput());
                    if (nonNull(uploadNewFileRecord)) {
                        armBatchResponses.addResponseBatchData(Integer.valueOf(uploadNewFileRecord.getRelationId()),
                                                               armResponseUploadFileRecord, uploadFileFilenameProcessor);
                    }
                } else {
                    log.warn("Failed to write upload file to temp workspace {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
                }
            } catch (IOException e) {
                log.error("Unable to write upload file to temporary workspace {} - {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath(),
                          e.getMessage());
            } catch (Exception e) {
                log.error("Unable to process arm response upload file {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath(), e);
            } finally {
                cleanupTemporaryJsonFile(jsonPath);
            }
        } else {
            log.warn("Failed to read upload file {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
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
                            "Unable to process upload file {} with EOD record {}, file Id {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath(),
                            armResponseUploadFileRecord.getA360RecordId(), armResponseUploadFileRecord.getA360FileId());
                    }
                } else {
                    //Read the upload file and log the error code and description with EOD
                    String errorDescription = StringUtils.isNotEmpty(armResponseUploadFileRecord.getExceptionDescription())
                        ? armResponseUploadFileRecord.getExceptionDescription() : "No error details found in response file";

                    log.warn(
                        "ARM status reports failed for upload file {}. ARM error description: {} ARM error status: {} for record {}, file Id {}",
                        uploadFileFilenameProcessor.getUploadFileFilenameAndPath(),
                        errorDescription,
                        armResponseUploadFileRecord.getErrorStatus(),
                        armResponseUploadFileRecord.getA360RecordId(),
                        armResponseUploadFileRecord.getA360FileId()
                    );
                    if (nonNull(externalObjectDirectory)) {
                        externalObjectDirectory.setErrorCode(errorDescription);
                    }
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus());
                }
            } else {
                log.warn("Unable to read upload file {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
            }
        } catch (Exception e) {
            log.error(UNABLE_TO_UPDATE_EOD, e);
        }
    }


    private void processUploadFileDataSuccess(ExternalObjectDirectoryEntity externalObjectDirectory,
                                              ArmResponseUploadFileRecord armResponseUploadFileRecord) {
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
            updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.storedStatus());
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

    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
    private static ResponseFilenames getArmResponseFilenames(List<String> responseFiles, String manifestName) {
        ResponseFilenames responseFilenames = new ResponseFilenames();
        for (String responseFile : responseFiles) {
            try {
                if (responseFile.endsWith(generateSuffix(ARM_CREATE_RECORD_FILENAME_KEY))) {
                    log.debug("Found ARM create record response file {} for manifest {}", responseFile, manifestName);
                    CreateRecordFilenameProcessor createRecordFilenameProcessor = new CreateRecordFilenameProcessor(responseFile);
                    responseFilenames.getCreateRecordResponses().add(createRecordFilenameProcessor);
                } else if (responseFile.endsWith(generateSuffix(ARM_UPLOAD_FILE_FILENAME_KEY))) {
                    log.debug("Found ARM upload file response file {} for manifest {}", responseFile, manifestName);
                    UploadFileFilenameProcessor uploadFileFilenameProcessor = new UploadFileFilenameProcessor(responseFile);
                    responseFilenames.getUploadFileResponses().add(uploadFileFilenameProcessor);
                } else if (responseFile.endsWith(generateSuffix(ARM_INVALID_LINE_FILENAME_KEY))) {
                    log.debug("Found ARM invalid line response file {} for manifest {}", responseFile, manifestName);
                    InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor = new InvalidLineFileFilenameProcessor(responseFile);
                    responseFilenames.getInvalidLineResponses().add(invalidLineFileFilenameProcessor);
                } else {
                    log.warn("Unknown ARM response file type {} for manifest {}", responseFile, manifestName);
                }
            } catch (IllegalArgumentException e) {
                // This occurs when the filename is not parsable
                log.error("Invalid ARM response filename: {} for manifest {}", responseFile, manifestName);
            }
        }
        return responseFilenames;
    }

    private void processInvalidFiles(List<InvalidLineFileFilenameProcessor> invalidLineResponses, ArmBatchResponses armBatchResponses) {
        for (InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor : invalidLineResponses) {
            try {
                BinaryData invalidLineFileBinary = armDataManagementApi.getBlobData(invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath());
                readInvalidLineFile(invalidLineFileBinary, invalidLineFileFilenameProcessor, armBatchResponses);
            } catch (Exception e) {
                log.error("Unable to process ARM invalid line file {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath(), e);
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
                    invalidLineFileFilenameProcessor.getInvalidLineFilename(),
                    armDataManagementConfiguration.getTempBlobWorkspace(),
                    appendUuidToWorkspace
                );

                if (jsonPath.toFile().exists()) {
                    logResponseFileContents(jsonPath);
                    ArmResponseInvalidLineRecord armResponseInvalidLineRecord = objectMapper.readValue(jsonPath.toFile(), ArmResponseInvalidLineRecord.class);
                    UploadNewFileRecord uploadNewFileRecord = readInputJson(armResponseInvalidLineRecord.getInput());
                    if (nonNull(uploadNewFileRecord)) {
                        armBatchResponses.addResponseBatchData(Integer.valueOf(uploadNewFileRecord.getRelationId()),
                                                               armResponseInvalidLineRecord, invalidLineFileFilenameProcessor);
                    }
                } else {
                    log.warn("Failed to write invalid line file to temp workspace {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath());
                }
            } catch (IOException e) {
                log.error("Unable to write invalid line file to temporary workspace {} - {}",
                          invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath(), e.getMessage());

            } catch (Exception e) {
                log.error("Unable to process ARM response invalid line file {}",
                          invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath(), e);

            } finally {
                cleanupTemporaryJsonFile(jsonPath);
            }
        } else {
            log.error("Unable to read ARM response invalid line file {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath());
        }
    }

    private void processInvalidLineFileObject(int externalObjectDirectoryId, InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor,
                                              ArmResponseInvalidLineRecord armResponseInvalidLineRecord) {
        try {
            ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntity(externalObjectDirectoryId);
            if (nonNull(externalObjectDirectory) && nonNull(armResponseInvalidLineRecord)) {

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
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseManifestFailedStatus());
                } else {
                    String error = String.format("Incorrect status [%s] for invalid line file %s", invalidLineFileFilenameProcessor.getStatus(),
                                                 invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath());
                    log.warn(error);
                    externalObjectDirectory.setErrorCode(error);
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus());
                }
            } else {
                log.warn("Unable to read invalid line file {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath());
                updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus());
            }
        } catch (Exception e) {
            log.error(UNABLE_TO_UPDATE_EOD, e);
        }
    }

    void deleteResponseBlobs(ArmResponseBatchData armResponseBatchData) {
        List<String> responseBlobsToBeDeleted = getResponseBlobsToBeDeleted(armResponseBatchData);
        ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntity(armResponseBatchData.getExternalObjectDirectoryId());
        if (nonNull(externalObjectDirectory) && responseBlobsToBeDeleted.size() == 2) {
            ObjectRecordStatusEnum status = ObjectRecordStatusEnum.valueOfId(externalObjectDirectory.getStatus().getId());
            if (STORED.equals(status)
                || ARM_RESPONSE_PROCESSING_FAILED.equals(status)
                || ARM_RESPONSE_MANIFEST_FAILED.equals(status)
                || ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.equals(status)) {
                log.info("About to  delete ARM responses for EOD {}", externalObjectDirectory.getId());
                List<Boolean> deletedResponseBlobStatuses = responseBlobsToBeDeleted.stream()
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
            responseBlobsToBeDeleted.add(armResponseBatchData.getCreateRecordFilenameProcessor().getCreateRecordFilenameAndPath());
        }
        if (nonNull(armResponseBatchData.getUploadFileFilenameProcessor())) {
            responseBlobsToBeDeleted.add(armResponseBatchData.getUploadFileFilenameProcessor().getUploadFileFilenameAndPath());
        }
        if (nonNull(armResponseBatchData.getInvalidLineFileFilenameProcessor())) {
            responseBlobsToBeDeleted.add(armResponseBatchData.getInvalidLineFileFilenameProcessor().getInvalidLineFileFilenameAndPath());
        }
        return responseBlobsToBeDeleted;
    }

    private ExternalObjectDirectoryEntity getExternalObjectDirectoryEntity(Integer eodId) {
        ExternalObjectDirectoryEntity externalObjectDirectory = null;
        try {
            Optional<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityOptional =
                externalObjectDirectoryService.eagerLoadExternalObjectDirectory(eodId);
            if (externalObjectDirectoryEntityOptional.isPresent()) {
                externalObjectDirectory = externalObjectDirectoryEntityOptional.get();
            } else {
                log.warn("Unable to find external object directory with ID {}", eodId);
            }
        } catch (Exception e) {
            log.error("Unable to find external object directory with ID {}", eodId, e);
        }
        return externalObjectDirectory;
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

    private void updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                     ObjectRecordStatusEntity objectRecordStatus) {
        if (nonNull(externalObjectDirectory)) {
            log.info(
                "ARM Push updating ARM status from {} to {} for ID {}",
                externalObjectDirectory.getStatus().getId(),
                objectRecordStatus.getId(),
                externalObjectDirectory.getId()
            );
            ObjectRecordStatusEnum status = ObjectRecordStatusEnum.valueOfId(objectRecordStatus.getId());
            if (STORED.equals(status)) {
                logApi.archiveToArmSuccessful(externalObjectDirectory.getId());
            } else if (ARM_RESPONSE_MANIFEST_FAILED.equals(status)
                || ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.equals(status)) {
                logApi.archiveToArmFailed(externalObjectDirectory.getId());
            } else if (ARM_RESPONSE_PROCESSING_FAILED.equals(status)
                && externalObjectDirectory.getTransferAttempts() > armDataManagementConfiguration.getMaxRetryAttempts()) {
                logApi.archiveToArmFailed(externalObjectDirectory.getId());
            }
            externalObjectDirectory.setStatus(objectRecordStatus);
            externalObjectDirectory.setLastModifiedBy(userAccount);
            externalObjectDirectory.setLastModifiedDateTime(currentTimeHelper.currentOffsetDateTime());
            externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
        } else {
            log.warn("EOD is null");
        }
    }
}
