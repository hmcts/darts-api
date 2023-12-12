package uk.gov.hmcts.darts.testutil;


import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class ArmTestUtil {

    private static final int DELETE_TIMEOUT = 60;
    public static final int ERROR_CODE_202 = 202;
    @Autowired
    private ArmDataManagementApi armDataManagementApi;

    @Autowired
    private ArmDataManagementDao armDataManagementDao;


    public void deleteBlobData(String containerName, String blobPathAndName) throws AzureDeleteBlobException {
        try {
            //String blobPathAndName = armDataManagementConfiguration.getArmSubmissionDropZone() + filename;
            BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
            BlobClient blobClient = armDataManagementDao.getBlobClient(containerClient, blobPathAndName);

            //BlobClient blobClient = dataManagementDao.getBlobClient(containerClient, blobId);
            Response<Void> response = blobClient.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE,
                                                                    null,
                                                                    Duration.of(DELETE_TIMEOUT,ChronoUnit.SECONDS),
                                                                    null);

            if (ERROR_CODE_202 != response.getStatusCode()) {
                throw new AzureDeleteBlobException("Failed to delete from container because of http code: " + response.getStatusCode());
            }

        } catch (RuntimeException e) {
            throw new AzureDeleteBlobException("Could not delete from container: " + containerName + " blobPathAndName: " + blobPathAndName, e);
        }
    }
}
