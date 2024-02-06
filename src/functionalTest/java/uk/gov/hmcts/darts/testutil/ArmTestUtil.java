package uk.gov.hmcts.darts.testutil;


import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Service
@AllArgsConstructor
@Slf4j
public class ArmTestUtil {

    public static final int STATUS_CODE_202 = 202;
    private static final int DELETE_TIMEOUT = 60;
    private final ArmDataManagementDao armDataManagementDao;


    public void deleteBlobData(String containerName, String blobPathAndName) {
        try {
            BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
            BlobClient blobClient = armDataManagementDao.getBlobClient(containerClient, blobPathAndName);

            Response<Boolean> response = blobClient.deleteIfExistsWithResponse(
                  DeleteSnapshotsOptionType.INCLUDE,
                  null,
                  Duration.of(DELETE_TIMEOUT, ChronoUnit.SECONDS),
                  null
            );

            log.info("deleteBlobData for container {}, Blob path {}, Returned status code {}", containerName, response.getStatusCode());
            if (STATUS_CODE_202 != response.getStatusCode()) {
                throw new AzureDeleteBlobException("Failed to delete from container because of http code: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Could not delete from container: " + containerName + " blobPathAndName: " + blobPathAndName, e.getMessage(), e);
        }
    }
}
