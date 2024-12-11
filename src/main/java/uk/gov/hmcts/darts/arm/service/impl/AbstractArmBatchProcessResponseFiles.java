package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInputUploadFileRecord;
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
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MISSING_RESPONSE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Slf4j
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.CouplingBetweenObjects"})
@RequiredArgsConstructor
public abstract class AbstractArmBatchProcessResponseFiles implements ArmResponseFilesProcessor {

    protected static final String UNABLE_TO_UPDATE_EOD = "Unable to update EOD";
    protected static final String CREATE_RECORD = "create_record";
    protected final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    protected final ArmDataManagementApi armDataManagementApi;
    protected final FileOperationService fileOperationService;
    protected final ArmDataManagementConfiguration armDataManagementConfiguration;
    protected final ObjectMapper objectMapper;
    protected final UserIdentity userIdentity;
    protected final CurrentTimeHelper timeHelper;
    protected final ExternalObjectDirectoryService externalObjectDirectoryService;
    protected final LogApi logApi;

    @Override
    public void processResponseFiles(int batchSize) {
        UserAccountEntity userAccount = userIdentity.getUserAccount();
        ArrayList<String> inputUploadResponseFiles = new ArrayList<>();
        String prefix = getManifestFilePrefix();
        int maxContinuationBatchSize = armDataManagementConfiguration.getMaxContinuationBatchSize();

        try {
            log.info("About to look for IU files starting with prefix: {}", prefix);
            String continuationToken = null;
            // Iterate through the continuation token until no more data is found. First time round continuation token is null
            // which sets up the session and when there are no more results the result from listResponseBlobsUsingMarker will be null
            for (int pageSize = 0; pageSize < batchSize; pageSize += maxContinuationBatchSize) {

                ContinuationTokenBlobs continuationTokenData =
                    armDataManagementApi.listResponseBlobsUsingMarker(prefix, maxContinuationBatchSize, continuationToken);
                if (nonNull(continuationTokenData)) {
                    inputUploadResponseFiles.addAll(continuationTokenData.getBlobNamesAndPaths());
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
        if (CollectionUtils.isNotEmpty(inputUploadResponseFiles)) {
            for (String inputUploadBlob : inputUploadResponseFiles) {
                Instant start = Instant.now();
                log.info("ARM PERFORMANCE PULL START for manifest {} started at {}", inputUploadBlob, start);
                processInputUploadBlob(inputUploadBlob, userAccount);
                Instant finish = Instant.now();
                long timeElapsed = Duration.between(start, finish).toMillis();
                log.info("ARM PERFORMANCE PULL END for manifest {} ended at {}", inputUploadBlob, finish);
                log.info("ARM PERFORMANCE PULL ELAPSED TIME for manifest {} took {} ms", inputUploadBlob, timeElapsed);
            }
        } else {
            log.warn("No response files found with prefix: {}", prefix);
        }
    }

    private void processInputUploadBlob(String inputUploadBlob, UserAccountEntity userAccount) {
        log.debug("Found ARM Input Upload file {}", inputUploadBlob);
        try {
            BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor = new BatchInputUploadFileFilenameProcessor(inputUploadBlob);
            String manifestName = generateManifestName(batchUploadFileFilenameProcessor.getUuidString());

            String inputUploadFileRecordStr = armDataManagementApi.getBlobData(inputUploadBlob).toString();
            log.info("Contents of ARM Input Upload file: '{}' '{}", inputUploadBlob, inputUploadFileRecordStr);
            ArmResponseInputUploadFileRecord inputUploadFileRecord = objectMapper.readValue(inputUploadFileRecordStr, ArmResponseInputUploadFileRecord.class);
            List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = externalObjectDirectoryRepository
                .findAllByStatusAndManifestFile(EodHelper.armDropZoneStatus(), manifestName);

            if (CollectionUtils.isNotEmpty(externalObjectDirectoryEntities)) {
                OffsetDateTime timestamp = inputUploadFileRecord.getTimestamp();
                List<ExternalObjectDirectoryEntity> editedExternalObjectDirectoryEntities = externalObjectDirectoryEntities.stream()
                    .filter(eod -> eod.getInputUploadProcessedTs() == null)
                    .peek(eod -> eod.setInputUploadProcessedTs(timestamp))
                    .toList();
                if (CollectionUtils.isNotEmpty(editedExternalObjectDirectoryEntities)) {
                    externalObjectDirectoryRepository.saveAll(editedExternalObjectDirectoryEntities);
                }
                externalObjectDirectoryService.updateStatus(
                    EodHelper.armProcessingResponseFilesStatus(),
                    userAccount,
                    externalObjectDirectoryEntities.stream().map(ExternalObjectDirectoryEntity::getId).toList(),
                    timeHelper.currentOffsetDateTime());
            } else {
                log.warn("No external object directories found with filename: {}", manifestName);
            }

            processResponseFileByHashcode(batchUploadFileFilenameProcessor, manifestName, userAccount);
            deleteResponseBlobsByManifestName(batchUploadFileFilenameProcessor, manifestName);
            resetArmStatusForUnprocessedEods(manifestName, userAccount);
        } catch (IllegalArgumentException e) {
            log.error("Unable to process manifest filename {}", inputUploadBlob, e);
            deleteResponseBlobs(List.of(inputUploadBlob));
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
        } else {
            // If no EODs are found for the manifest, delete the input upload blob and any linked response files as they are dangling
            deleteDanglingResponses(batchUploadFileFilenameProcessor);
        }

    }

    // Delete the response files if they are not linked to any EODs
    private void deleteDanglingResponses(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor) {
        List<String> responseFiles = new ArrayList<>();
        try {
            responseFiles.addAll(armDataManagementApi.listResponseBlobs(batchUploadFileFilenameProcessor.getHashcode()));
        } catch (Exception e) {
            log.error("Unable to find dangling response files for hashcode {}", batchUploadFileFilenameProcessor.getHashcode(), e);
        }

        if (CollectionUtils.isNotEmpty(responseFiles)) {
            List<Boolean> deletedResponseBlobStatuses = deleteResponseBlobs(responseFiles);

            if (deletedResponseBlobStatuses.contains(false)) {
                log.warn("Unable to delete dangling ARM batch input upload file {} as referenced data is not all deleted",
                         batchUploadFileFilenameProcessor.getBatchMetadataFilename());
            } else {
                log.info("About to delete dangling ARM input upload file {}", batchUploadFileFilenameProcessor.getBatchMetadataFilename());
                armDataManagementApi.deleteBlobData(batchUploadFileFilenameProcessor.getBatchMetadataFilenameAndPath());
            }
        } else {
            log.info("Unable to delete dangling ARM input upload file {}", batchUploadFileFilenameProcessor.getBatchMetadataFilename());
            armDataManagementApi.deleteBlobData(batchUploadFileFilenameProcessor.getBatchMetadataFilenameAndPath());
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
                || ARM_RPO_PENDING.equals(statusEnum)
                || ARM_RESPONSE_PROCESSING_FAILED.equals(statusEnum)
                || ARM_RESPONSE_MANIFEST_FAILED.equals(statusEnum)
                || ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.equals(statusEnum);
        }
        return false;
    }

    private String generateManifestName(String uuid) {
        var fileNameFormat = "%s_%s.%s";
        return String.format(fileNameFormat,
                             getManifestFilePrefix(),
                             uuid,
                             armDataManagementConfiguration.getFileExtension()
        );
    }

    private void resetArmStatusForUnprocessedEods(String manifestName, UserAccountEntity userAccount) {
        List<ExternalObjectDirectoryEntity> unprocessedExternalObjectDirectoryEntities = externalObjectDirectoryRepository
            .findAllByStatusAndManifestFile(EodHelper.armProcessingResponseFilesStatus(), manifestName);
        if (CollectionUtils.isNotEmpty(unprocessedExternalObjectDirectoryEntities)) {
            externalObjectDirectoryService.updateStatus(
                EodHelper.armDropZoneStatus(),
                userAccount,
                unprocessedExternalObjectDirectoryEntities.stream().map(ExternalObjectDirectoryEntity::getId).toList(),
                timeHelper.currentOffsetDateTime());
            for (ExternalObjectDirectoryEntity unprocessedExternalObjectDirectoryEntity : unprocessedExternalObjectDirectoryEntities) {
                log.warn("Unable to process ARM responses for EOD {}", unprocessedExternalObjectDirectoryEntity.getId());
            }
        }
    }

    private void processResponseFileByHashcode(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor, String manifestName,
                                               UserAccountEntity userAccount) {
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
                    processBatchResponseFiles(batchUploadFileFilenameProcessor, armBatchResponses, userAccount);
                }
            } else {
                log.info("Unable to find response files starting with {} for manifest {}", batchUploadFileFilenameProcessor.getHashcode(), manifestName);
            }
        } catch (Exception e) {
            log.error("Unable to process responses for file {}", batchUploadFileFilenameProcessor.getBatchMetadataFilenameAndPath(), e);
        }
    }

    protected void processBatchResponseFiles(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor,
                                             ArmBatchResponses armBatchResponses,
                                             UserAccountEntity userAccount) {
        armBatchResponses.getArmBatchResponseMap().values().forEach(
            armResponseBatchData -> {
                //If there is only 1 invalid line file (invalid line processor is added at the same time as the invalid line file so only 1 needs to be checked)
                //and either a "create_record" or "upload_new_file", process the files
                if ((CollectionUtils.isNotEmpty(armResponseBatchData.getInvalidLineFileFilenameProcessors())
                    && armResponseBatchData.getInvalidLineFileFilenameProcessors().size() == 1)
                    && (nonNull(armResponseBatchData.getCreateRecordFilenameProcessor())
                    || nonNull(armResponseBatchData.getArmResponseUploadFileRecord()))) {
                    preProcessResponseFilesActions(armResponseBatchData.getExternalObjectDirectoryId());

                    processInvalidLineFile(armResponseBatchData.getExternalObjectDirectoryId(),
                                           armResponseBatchData.getInvalidLineFileFilenameProcessors().getFirst(),
                                           armResponseBatchData.getArmResponseInvalidLineRecords().getFirst(),
                                           armResponseBatchData.getCreateRecordFilenameProcessor(),
                                           armResponseBatchData.getUploadFileFilenameProcessor(),
                                           userAccount);
                    deleteResponseBlobs(armResponseBatchData);
                } else if (CollectionUtils.isNotEmpty(armResponseBatchData.getInvalidLineFileFilenameProcessors())
                    && (armResponseBatchData.getInvalidLineFileFilenameProcessors().size() > 1)) {

                    preProcessResponseFilesActions(armResponseBatchData.getExternalObjectDirectoryId());
                    processMultipleInvalidLineFiles(armResponseBatchData, userAccount);
                    deleteResponseBlobs(armResponseBatchData);

                } else if (nonNull(armResponseBatchData.getCreateRecordFilenameProcessor())
                    && nonNull(armResponseBatchData.getUploadFileFilenameProcessor())) {
                    preProcessResponseFilesActions(armResponseBatchData.getExternalObjectDirectoryId());

                    processUploadFileObject(batchUploadFileFilenameProcessor, armResponseBatchData, userAccount);
                    deleteResponseBlobs(armResponseBatchData);
                } else {
                    log.info("Unable to find response files for external object {}", armResponseBatchData.getExternalObjectDirectoryId());
                    logResponsesFound(armResponseBatchData);
                    try {
                        ExternalObjectDirectoryEntity externalObjectDirectory =
                            getExternalObjectDirectoryEntity(armResponseBatchData.getExternalObjectDirectoryId());

                        OffsetDateTime minInputUploadProcessedTime = timeHelper.currentOffsetDateTime().minus(
                            armDataManagementConfiguration.getArmMissingResponseDuration());

                        if (externalObjectDirectory.getInputUploadProcessedTs() != null
                            && externalObjectDirectory.getInputUploadProcessedTs().isBefore(minInputUploadProcessedTime)) {
                            markEodAsResponseProcessingFailed(externalObjectDirectory, userAccount);
                        } else {
                            updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armDropZoneStatus(), userAccount);
                        }
                    } catch (Exception e) {
                        log.error(UNABLE_TO_UPDATE_EOD, e);
                    }
                }
            }
        );
    }

    protected void markEodAsResponseProcessingFailed(ExternalObjectDirectoryEntity externalObjectDirectory, UserAccountEntity userAccount) {
        updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus(), userAccount);
    }

    private void logResponsesFound(ArmResponseBatchData armResponseBatchData) {

        int eodId = armResponseBatchData.getExternalObjectDirectoryId();
        List<InvalidLineFileFilenameProcessor> invalidLineFileFilenameProcessors = armResponseBatchData.getInvalidLineFileFilenameProcessors();
        CreateRecordFilenameProcessor createRecordFilenameProcessor = armResponseBatchData.getCreateRecordFilenameProcessor();
        UploadFileFilenameProcessor uploadFileFilenameProcessor = armResponseBatchData.getUploadFileFilenameProcessor();

        log.info("Found ARM responses for external object directory ID {} with {} invalid line files, {} create record files and {} upload files",
                 eodId,
                 invalidLineFileFilenameProcessors.size(),
                 nonNull(createRecordFilenameProcessor) ? 1 : 0,
                 nonNull(uploadFileFilenameProcessor) ? 1 : 0);
        if (nonNull(createRecordFilenameProcessor)) {
            log.info("Found eod {} with create record file: {}", eodId,
                     createRecordFilenameProcessor.getCreateRecordFilenameAndPath());
        }
        if (nonNull(uploadFileFilenameProcessor)) {
            log.info("Found eod {} with upload file: {}", eodId,
                     uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
        }
        if (CollectionUtils.isNotEmpty(invalidLineFileFilenameProcessors)) {
            invalidLineFileFilenameProcessors.forEach(
                invalidLineFileFilenameProcessor ->
                    log.info("Found eod {} with invalid line file: {}", eodId,
                             invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath())
            );
        }
    }

    private void processMultipleInvalidLineFiles(ArmResponseBatchData armResponseBatchData, UserAccountEntity userAccount) {
        try {
            ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntity(armResponseBatchData.getExternalObjectDirectoryId());
            var invalidLineFileFilenameProcessor1 = armResponseBatchData.getInvalidLineFileFilenameProcessors().getFirst();
            var invalidLineFileFilenameProcessor2 = armResponseBatchData.getInvalidLineFileFilenameProcessors().getLast();

            if (nonNull(externalObjectDirectory)) {
                if (armResponseBatchData.getArmResponseInvalidLineRecords().size() == 2) {
                    var invalidLineRecord1 = armResponseBatchData.getArmResponseInvalidLineRecords().getFirst();
                    var invalidLineRecord2 = armResponseBatchData.getArmResponseInvalidLineRecords().getLast();

                    if (nonNull(invalidLineRecord1) && nonNull(invalidLineRecord2)) {
                        // Read the invalid lines file and log the error code and description with EOD
                        log.warn(
                            "Multiple ARM invalid line files for external object id {}. Invalid line 1 ARM error description: {} ARM error status: {}"
                                + " Invalid line 1 ARM error description: {} ARM error status: {}",
                            externalObjectDirectory.getId(),
                            invalidLineRecord1.getExceptionDescription(),
                            invalidLineRecord1.getErrorStatus(),
                            invalidLineRecord2.getExceptionDescription(),
                            invalidLineRecord2.getErrorStatus()
                        );
                        updateVerificationAttempts(externalObjectDirectory);
                        setInvalidLineErrorDescription(invalidLineRecord1, invalidLineRecord2, externalObjectDirectory);

                        updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseManifestFailedStatus(), userAccount);

                    } else {
                        log.warn("Unable to read invalid line files {} - {}", invalidLineFileFilenameProcessor1.getInvalidLineFileFilenameAndPath(),
                                 invalidLineFileFilenameProcessor2.getInvalidLineFileFilenameAndPath());
                        updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus(), userAccount);
                    }
                } else {
                    log.warn("Invalid line file count is greater than 2 for external object directory ID {}", externalObjectDirectory.getId());
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus(), userAccount);
                    List<String> invalidResponseFiles = new ArrayList<>();
                    armResponseBatchData.getInvalidLineFileFilenameProcessors().forEach(
                        invalidLineFileFilenameProcessor -> invalidResponseFiles.add(invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath())
                    );
                    deleteResponseBlobs(invalidResponseFiles);
                }
            } else {

                log.warn("Unable to find external object directory with ID {} for ARM batch responses with first IL file {}, second IL file {}",
                         armResponseBatchData.getExternalObjectDirectoryId(),
                         invalidLineFileFilenameProcessor1.getInvalidLineFileFilenameAndPath(),
                         invalidLineFileFilenameProcessor2.getInvalidLineFileFilenameAndPath());
                List<String> invalidResponseFiles = new ArrayList<>();
                armResponseBatchData.getInvalidLineFileFilenameProcessors().forEach(
                    invalidLineFileFilenameProcessor -> invalidResponseFiles.add(invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath())
                );
                deleteResponseBlobs(invalidResponseFiles);
            }
        } catch (Exception e) {
            log.error("Unable to update invalid line responses", e);
        }

    }

    private void setInvalidLineErrorDescription(ArmResponseInvalidLineRecord record1, ArmResponseInvalidLineRecord record2,
                                                ExternalObjectDirectoryEntity eod) {

        String operation1 = getOperation(record1);
        String operation2 = getOperation(record2);

        StringBuilder errorDescription = new StringBuilder();
        if (CREATE_RECORD.equalsIgnoreCase(operation2)) {
            appendErrorDescription(errorDescription, operation2, record2);
            appendErrorDescription(errorDescription, operation1, record1);
        } else {
            appendErrorDescription(errorDescription, operation1, record1);
            appendErrorDescription(errorDescription, operation2, record2);
        }
        eod.setErrorCode(errorDescription.toString());
    }

    private String getOperation(ArmResponseInvalidLineRecord record) {
        UploadNewFileRecord uploadRecord = readInputJson(record.getInput());
        String operation = null;
        if (nonNull(uploadRecord)) {
            operation = uploadRecord.getOperation();
        }
        return StringUtils.isNotEmpty(operation) ? operation : "UNKNOWN";
    }

    private void appendErrorDescription(StringBuilder errorDescription, String operation, ArmResponseInvalidLineRecord record) {
        errorDescription
            .append("Operation: ")
            .append(operation)
            .append(" - ")
            .append(record.getExceptionDescription())
            .append("; ");
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
                    ArmResponseCreateRecord armResponseCreateRecord = getResponseCreateRecordOrDelete(jsonPath);
                    UploadNewFileRecord uploadNewFileRecord = readInputJson(armResponseCreateRecord.getInput());
                    if (nonNull(uploadNewFileRecord)) {
                        if (StringUtils.isNotEmpty(uploadNewFileRecord.getRelationId())) {
                            armBatchResponses.addResponseBatchData(Integer.valueOf(uploadNewFileRecord.getRelationId()),
                                                                   armResponseCreateRecord, createRecordFilenameProcessor);
                        } else {
                            log.warn("Unable to get EOD id (relation id) from uploadNewFileRecord {} create record {}",
                                     armResponseCreateRecord.getInput(), createRecordFilenameProcessor.getCreateRecordFilenameAndPath());
                        }
                    } else {
                        log.warn("Failed to obtain EOD id (relation id) from create record file  {}",
                                 createRecordFilenameProcessor.getCreateRecordFilenameAndPath());
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

    private ArmResponseCreateRecord getResponseCreateRecordOrDelete(Path jsonPath) throws IOException {
        try {
            return objectMapper.readValue(jsonPath.toFile(), ArmResponseCreateRecord.class);
        } catch (Exception e) {
            log.error("Unable to read ARM response create record file {} - About to delete ", jsonPath.toFile().getAbsoluteFile(), e);
            deleteResponseBlobs(List.of(jsonPath.toFile().getAbsolutePath()));
            throw e;
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
                    ArmResponseUploadFileRecord armResponseUploadFileRecord = getResponseUploadFileRecordOrDelete(jsonPath);
                    UploadNewFileRecord uploadNewFileRecord = readInputJson(armResponseUploadFileRecord.getInput());
                    if (nonNull(uploadNewFileRecord)) {
                        if (StringUtils.isNotEmpty(uploadNewFileRecord.getRelationId())) {
                            armBatchResponses.addResponseBatchData(Integer.valueOf(uploadNewFileRecord.getRelationId()),
                                                                   armResponseUploadFileRecord, uploadFileFilenameProcessor);
                        } else {
                            log.warn("Unable to get EOD id (relation id) from uploadNewFileRecord {} upload file {}",
                                     armResponseUploadFileRecord.getInput(), uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
                        }

                    } else {
                        log.warn("Failed to obtain EOD id (relation id) from upload file  {}",
                                 uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
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

    private ArmResponseUploadFileRecord getResponseUploadFileRecordOrDelete(Path jsonPath) throws IOException {
        try {
            return objectMapper.readValue(jsonPath.toFile(), ArmResponseUploadFileRecord.class);
        } catch (Exception e) {
            log.error("Unable to read ARM response upload file {} - About to delete ", jsonPath.toFile().getAbsoluteFile(), e);
            deleteResponseBlobs(List.of(jsonPath.toFile().getAbsolutePath()));
            throw e;
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

    private void processUploadFileObject(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor,
                                         ArmResponseBatchData armResponseBatchData,
                                         UserAccountEntity userAccount) {

        int externalObjectDirectoryId = armResponseBatchData.getExternalObjectDirectoryId();
        UploadFileFilenameProcessor uploadFileFilenameProcessor = armResponseBatchData.getUploadFileFilenameProcessor();
        ArmResponseUploadFileRecord armResponseUploadFileRecord = armResponseBatchData.getArmResponseUploadFileRecord();
        CreateRecordFilenameProcessor createRecordFilenameProcessor = armResponseBatchData.getCreateRecordFilenameProcessor();

        try {
            ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntity(externalObjectDirectoryId);
            if (nonNull(armResponseUploadFileRecord)) {
                if (nonNull(externalObjectDirectory)) {
                    //If the filename contains 1
                    if (ARM_RESPONSE_SUCCESS_STATUS_CODE.equals(uploadFileFilenameProcessor.getStatus())) {

                        processUploadFileDataSuccess(batchUploadFileFilenameProcessor,
                                                     armResponseBatchData,
                                                     externalObjectDirectory,
                                                     armResponseUploadFileRecord,
                                                     userAccount);

                    } else {
                        processUploadFileDataFailure(armResponseUploadFileRecord, uploadFileFilenameProcessor, externalObjectDirectory, userAccount);
                    }
                } else {
                    processNoEodFoundFromSuccessResponses(externalObjectDirectoryId, uploadFileFilenameProcessor, armResponseUploadFileRecord,
                                                          createRecordFilenameProcessor);
                }
            } else {
                log.warn("Unable to read upload file {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
            }
        } catch (Exception e) {
            log.error(UNABLE_TO_UPDATE_EOD, e);
        }
    }

    private void processNoEodFoundFromSuccessResponses(int externalObjectDirectoryId, UploadFileFilenameProcessor uploadFileFilenameProcessor,
                                                       ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                                       CreateRecordFilenameProcessor createRecordFilenameProcessor) {
        log.warn("Unable to find external object directory with ID {} for ARM batch responses with CR file {}, UF file {}",
                 externalObjectDirectoryId,
                 createRecordFilenameProcessor.getCreateRecordFilenameAndPath(),
                 uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
        List<String> validResponseFiles = List.of(createRecordFilenameProcessor.getCreateRecordFilenameAndPath(),
                                                  uploadFileFilenameProcessor.getUploadFileFilenameAndPath());
        deleteResponseBlobs(validResponseFiles);
        log.warn(
            "Unable to process upload file {} with EOD record {}, IU file {}", uploadFileFilenameProcessor.getUploadFileFilenameAndPath(),
            armResponseUploadFileRecord.getA360RecordId(), armResponseUploadFileRecord.getA360FileId());
    }


    private void processUploadFileDataSuccess(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor,
                                              ArmResponseBatchData armResponseBatchData,
                                              ExternalObjectDirectoryEntity externalObjectDirectory,
                                              ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                              UserAccountEntity userAccount) {
        if (externalObjectDirectory.getChecksum() != null) {
            verifyChecksumAndUpdateStatus(batchUploadFileFilenameProcessor,
                                          armResponseBatchData,
                                          armResponseUploadFileRecord,
                                          externalObjectDirectory,
                                          externalObjectDirectory.getChecksum(),
                                          userAccount);
        } else {
            log.warn("Unable to verify checksum for external object {}", externalObjectDirectory.getId());
            updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseChecksumVerificationFailedStatus(), userAccount);
        }
    }

    private void verifyChecksumAndUpdateStatus(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor,
                                               ArmResponseBatchData armResponseBatchData,
                                               ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                               ExternalObjectDirectoryEntity externalObjectDirectory,
                                               String objectChecksum,
                                               UserAccountEntity userAccount) {
        if (objectChecksum.equalsIgnoreCase(armResponseUploadFileRecord.getMd5())) {
            onUploadFileChecksumValidationSuccess(batchUploadFileFilenameProcessor,
                                                  armResponseBatchData,
                                                  armResponseUploadFileRecord,
                                                  externalObjectDirectory,
                                                  objectChecksum,
                                                  userAccount);
        } else {
            onUploadFileChecksumValidationFailure(armResponseUploadFileRecord, externalObjectDirectory, objectChecksum, userAccount);
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
            } catch (Exception e) {
                log.error("Failed to parse the input field {}", e.getMessage());
            }
        } else {
            log.warn("Unable to parse the input field as it is null or empty");
        }
        return uploadNewFileRecord;
    }

    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
    private ResponseFilenames getArmResponseFilenames(List<String> responseFiles, String manifestName) {
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
                deleteResponseBlobs(List.of(responseFile));
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
                    ArmResponseInvalidLineRecord armResponseInvalidLineRecord = getResponseInvalidLineRecordOrDelete(jsonPath);
                    String input = armResponseInvalidLineRecord.getInput();
                    UploadNewFileRecord uploadNewFileRecord = readInputJson(input);
                    if (nonNull(uploadNewFileRecord)) {
                        if (StringUtils.isNotEmpty(uploadNewFileRecord.getRelationId())) {
                            armBatchResponses.addResponseBatchData(Integer.valueOf(uploadNewFileRecord.getRelationId()),
                                                                   armResponseInvalidLineRecord, invalidLineFileFilenameProcessor);

                        } else {
                            log.warn("Unable to get EOD id (relation id) from uploadNewFileRecord {} invalid line file {}",
                                     armResponseInvalidLineRecord.getInput(), invalidLineFileFilenameProcessor.getInvalidLineFilename());
                        }
                    } else {
                        log.warn("Failed to obtain EOD id (relation id) from invalid line record {} from file {}", input,
                                 invalidLineFileFilenameProcessor.getInvalidLineFilename());
                    }
                } else {
                    log.warn("Failed to write invalid line file to temp workspace {}", invalidLineFileFilenameProcessor.getInvalidLineFilename());
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

    private ArmResponseInvalidLineRecord getResponseInvalidLineRecordOrDelete(Path jsonPath) throws IOException {
        try {
            return objectMapper.readValue(jsonPath.toFile(), ArmResponseInvalidLineRecord.class);
        } catch (Exception e) {
            log.error("Unable to read ARM response invalid line file {} - About to delete ", jsonPath.toFile().getAbsoluteFile(), e);
            deleteResponseBlobs(List.of(jsonPath.toFile().getAbsolutePath()));
            throw e;
        }
    }

    private void processInvalidLineFile(int externalObjectDirectoryId, InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor,
                                        ArmResponseInvalidLineRecord armResponseInvalidLineRecord,
                                        CreateRecordFilenameProcessor createRecordFilenameProcessor,
                                        UploadFileFilenameProcessor uploadFileFilenameProcessor,
                                        UserAccountEntity userAccount) {
        try {
            ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntity(externalObjectDirectoryId);
            if (nonNull(externalObjectDirectory)) {
                if (nonNull(armResponseInvalidLineRecord)) {
                    //If the filename contains 0
                    if (ARM_RESPONSE_INVALID_STATUS_CODE.equals(invalidLineFileFilenameProcessor.getStatus())) {
                        processInvalidLineFileActions(armResponseInvalidLineRecord, externalObjectDirectory, userAccount);
                    } else {
                        String error = String.format("Incorrect status [%s] for invalid line file %s", invalidLineFileFilenameProcessor.getStatus(),
                                                     invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath());
                        log.warn(error);
                        externalObjectDirectory.setErrorCode(error);
                        updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus(), userAccount);
                    }
                } else {
                    log.warn("Unable to read invalid line file {}", invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath());
                    updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus(), userAccount);
                }
            } else {

                String armResponseFile = getOtherFailedArmResponseFile(createRecordFilenameProcessor, uploadFileFilenameProcessor);

                log.warn("Unable to find external object directory with ID {} for ARM batch responses with IL file {}, other response file{}",
                         externalObjectDirectoryId,
                         invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath(),
                         armResponseFile);
                List<String> invalidResponseFiles = getInvalidResponseFiles(invalidLineFileFilenameProcessor, armResponseFile);
                deleteResponseBlobs(invalidResponseFiles);
            }
        } catch (Exception e) {
            log.error("Unable to update invalid line responses", e);
        }
    }

    private List<String> getInvalidResponseFiles(InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor, String armResponseFile) {
        List<String> invalidResponseFiles = new ArrayList<>();
        invalidResponseFiles.add(invalidLineFileFilenameProcessor.getInvalidLineFileFilenameAndPath());
        if (StringUtils.isNotEmpty(armResponseFile)) {
            invalidResponseFiles.add(armResponseFile);
        }
        return invalidResponseFiles;
    }

    private String getOtherFailedArmResponseFile(CreateRecordFilenameProcessor createRecordFilenameProcessor,
                                                 UploadFileFilenameProcessor uploadFileFilenameProcessor) {
        String armResponseFile = "";
        if (nonNull(createRecordFilenameProcessor) && nonNull(createRecordFilenameProcessor.getCreateRecordFilenameAndPath())) {
            armResponseFile = createRecordFilenameProcessor.getCreateRecordFilenameAndPath();
        } else if (nonNull(uploadFileFilenameProcessor) && nonNull(uploadFileFilenameProcessor.getUploadFileFilenameAndPath())) {
            armResponseFile = uploadFileFilenameProcessor.getUploadFileFilenameAndPath();
        }
        return armResponseFile;
    }

    void deleteResponseBlobs(ArmResponseBatchData armResponseBatchData) {
        List<String> responseBlobsToBeDeleted = getResponseBlobsToBeDeleted(armResponseBatchData);
        ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectoryEntity(armResponseBatchData.getExternalObjectDirectoryId());
        if (nonNull(externalObjectDirectory) && responseBlobsToBeDeleted.size() == 2) {
            ObjectRecordStatusEnum status = ObjectRecordStatusEnum.valueOfId(externalObjectDirectory.getStatus().getId());
            if (STORED.equals(status)
                || ARM_RESPONSE_PROCESSING_FAILED.equals(status)
                || ARM_RPO_PENDING.equals(status)
                || ARM_RESPONSE_MANIFEST_FAILED.equals(status)
                || ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.equals(status)) {
                log.info("About to  delete ARM responses for EOD {}", externalObjectDirectory.getId());
                List<Boolean> deletedResponseBlobStatuses = deleteResponseBlobs(responseBlobsToBeDeleted);

                if (deletedResponseBlobStatuses.size() == 2 && !deletedResponseBlobStatuses.contains(false)) {
                    externalObjectDirectory.setResponseCleaned(true);
                    externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
                } else {
                    log.warn("Unable to successfully delete the response files for EOD {} ", externalObjectDirectory.getId());
                }
            }
        }
    }

    private List<Boolean> deleteResponseBlobs(List<String> responseBlobsToBeDeleted) {
        return responseBlobsToBeDeleted.stream()
            .map(armDataManagementApi::deleteBlobData)
            .toList();
    }

    private List<String> getResponseBlobsToBeDeleted(ArmResponseBatchData armResponseBatchData) {
        List<String> responseBlobsToBeDeleted = new ArrayList<>();
        if (nonNull(armResponseBatchData.getCreateRecordFilenameProcessor())) {
            responseBlobsToBeDeleted.add(armResponseBatchData.getCreateRecordFilenameProcessor().getCreateRecordFilenameAndPath());
        }
        if (nonNull(armResponseBatchData.getUploadFileFilenameProcessor())) {
            responseBlobsToBeDeleted.add(armResponseBatchData.getUploadFileFilenameProcessor().getUploadFileFilenameAndPath());
        }
        if (CollectionUtils.isNotEmpty(armResponseBatchData.getInvalidLineFileFilenameProcessors())) {
            armResponseBatchData.getInvalidLineFileFilenameProcessors().forEach(
                processor -> responseBlobsToBeDeleted.add(processor.getInvalidLineFileFilenameAndPath()));
        }
        return responseBlobsToBeDeleted;
    }

    protected ExternalObjectDirectoryEntity getExternalObjectDirectoryEntity(Integer eodId) {
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

    private void updateVerificationAttempts(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        int currentNumberOfAttempts = externalObjectDirectoryEntity.getVerificationAttempts();
        log.debug(
            "Updating failed verification attempts from {} to {} for ID {}",
            currentNumberOfAttempts,
            currentNumberOfAttempts + 1,
            externalObjectDirectoryEntity.getId()
        );
        externalObjectDirectoryEntity.setVerificationAttempts(currentNumberOfAttempts + 1);
    }

    protected void updateExternalObjectDirectoryStatus(ExternalObjectDirectoryEntity externalObjectDirectory,
                                                       ObjectRecordStatusEntity objectRecordStatus,
                                                       UserAccountEntity userAccount) {
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
                && externalObjectDirectory.getVerificationAttempts() > armDataManagementConfiguration.getMaxRetryAttempts()) {
                logApi.archiveToArmFailed(externalObjectDirectory.getId());
            } else if (ARM_MISSING_RESPONSE.equals(status)) {
                //Log used for DynaTrace monitoring
                logApi.logArmMissingResponse(armDataManagementConfiguration.getArmMissingResponseDuration(), externalObjectDirectory.getId());
            }
            externalObjectDirectory.setStatus(objectRecordStatus);
            externalObjectDirectory.setLastModifiedBy(userAccount);
            externalObjectDirectory.setLastModifiedDateTime(timeHelper.currentOffsetDateTime());
            externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectory);
        } else {
            log.warn("EOD is null");
        }
    }

    protected abstract String getManifestFilePrefix();

    protected void preProcessResponseFilesActions(int armEodId) {
        // in the DARTS to ARM flow no pre-processing actions are needed
    }

    protected void onUploadFileChecksumValidationSuccess(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor,
                                                         ArmResponseBatchData armResponseBatchData,
                                                         ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                                         ExternalObjectDirectoryEntity externalObjectDirectory,
                                                         String objectChecksum,
                                                         UserAccountEntity userAccount) {
        externalObjectDirectory.setExternalFileId(armResponseUploadFileRecord.getA360FileId());
        externalObjectDirectory.setExternalRecordId(armResponseUploadFileRecord.getA360RecordId());
        externalObjectDirectory.setDataIngestionTs(OffsetDateTime.now());
        updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armRpoPendingStatus(), userAccount);
    }

    protected void onUploadFileChecksumValidationFailure(ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                                         ExternalObjectDirectoryEntity externalObjectDirectory,
                                                         String objectChecksum,
                                                         UserAccountEntity userAccount) {
        log.warn("External object id {} checksum differs. Arm checksum: {} Object Checksum: {}",
                 externalObjectDirectory.getId(),
                 armResponseUploadFileRecord.getMd5(), objectChecksum);
        externalObjectDirectory.setErrorCode(armResponseUploadFileRecord.getErrorStatus());
        updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseChecksumVerificationFailedStatus(), userAccount);
    }

    protected void processUploadFileDataFailure(ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                                UploadFileFilenameProcessor uploadFileFilenameProcessor,
                                                ExternalObjectDirectoryEntity externalObjectDirectory,
                                                UserAccountEntity userAccount) {
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
        externalObjectDirectory.setErrorCode(errorDescription);
        updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseProcessingFailedStatus(), userAccount);
    }

    protected void processInvalidLineFileActions(ArmResponseInvalidLineRecord armResponseInvalidLineRecord,
                                                 ExternalObjectDirectoryEntity externalObjectDirectory,
                                                 UserAccountEntity userAccount) {
        // Read the invalid lines file and log the error code and description with EOD
        log.warn(
            "ARM invalid line for external object id {}. ARM error description: {} ARM error status: {}",
            externalObjectDirectory.getId(),
            armResponseInvalidLineRecord.getExceptionDescription(),
            armResponseInvalidLineRecord.getErrorStatus()
        );
        updateVerificationAttempts(externalObjectDirectory);
        String operation = getOperation(armResponseInvalidLineRecord);

        StringBuilder errorDescription = new StringBuilder();
        appendErrorDescription(errorDescription, operation, armResponseInvalidLineRecord);

        externalObjectDirectory.setErrorCode(errorDescription.toString());
        updateExternalObjectDirectoryStatus(externalObjectDirectory, EodHelper.armResponseManifestFailedStatus(), userAccount);
    }

}