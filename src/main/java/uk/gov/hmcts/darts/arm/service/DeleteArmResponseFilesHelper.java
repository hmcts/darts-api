package uk.gov.hmcts.darts.arm.service;

import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;

import java.util.List;

public interface DeleteArmResponseFilesHelper {

    void deleteResponseBlobsByManifestName(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor, String manifestName);

    void deleteDanglingResponses(BatchInputUploadFileFilenameProcessor batchUploadFileFilenameProcessor);

    List<Boolean> deleteResponseBlobs(List<String> responseBlobsToBeDeleted);

    void deleteResponseBlobs(ArmResponseBatchData armResponseBatchData);

    List<String> getResponseBlobsToBeDeleted(ArmResponseBatchData armResponseBatchData);
    
}
