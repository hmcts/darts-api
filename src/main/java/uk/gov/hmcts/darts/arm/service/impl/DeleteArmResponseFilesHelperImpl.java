package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.service.DeleteArmResponseFilesHelper;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Service
@Slf4j
@AllArgsConstructor
public class DeleteArmResponseFilesHelperImpl implements DeleteArmResponseFilesHelper {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ArmDataManagementApi armDataManagementApi;
    private final ExternalObjectDirectoryService externalObjectDirectoryService;

    public void deleteResponseBlobsByManifestName(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor,
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

    /**
     * Delete the response files if they are not linked to any EODs.
     *
     * @param batchUploadFileFilenameProcessor the batch input upload file processor
     */
    public void deleteDanglingResponses(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor) {
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

    public List<Boolean> deleteResponseBlobs(List<String> responseBlobsToBeDeleted) {
        return responseBlobsToBeDeleted.stream()
            .map(armDataManagementApi::deleteBlobData)
            .toList();
    }

    public void deleteResponseBlobs(ArmResponseBatchData armResponseBatchData) {
        List<String> responseBlobsToBeDeleted = getResponseBlobsToBeDeleted(armResponseBatchData);
        ExternalObjectDirectoryEntity externalObjectDirectory = getExternalObjectDirectory(armResponseBatchData.getExternalObjectDirectoryId());
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

    public List<String> getResponseBlobsToBeDeleted(ArmResponseBatchData armResponseBatchData) {
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

    private ExternalObjectDirectoryEntity getExternalObjectDirectory(Integer eodId) {
        ExternalObjectDirectoryEntity externalObjectDirectory = null;
        try {
            externalObjectDirectory = externalObjectDirectoryService.eagerLoadExternalObjectDirectory(eodId).orElseThrow();
        } catch (Exception e) {
            log.error("Delete ARM responses - Unable to find external object directory with ID {}", eodId, e);
        }
        return externalObjectDirectory;
    }
}
