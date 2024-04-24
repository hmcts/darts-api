package uk.gov.hmcts.darts.testutil;


import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.valueOf;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArmTestUtil {

    private final ArmDataManagementDao armDataManagementDao;

    @Value("${darts.storage.blob.delete.timeout}")
    private int deleteTimeoutInSeconds;

    public void deleteBlobData(String containerName, String blobPathAndName) {
        try {
            BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
            BlobClient blobClient = armDataManagementDao.getBlobClient(containerClient, blobPathAndName);

            Response<Boolean> response = blobClient.deleteIfExistsWithResponse(
                DeleteSnapshotsOptionType.INCLUDE,
                null,
                Duration.of(deleteTimeoutInSeconds, ChronoUnit.SECONDS),
                null
            );

            HttpStatus httpStatus = valueOf(response.getStatusCode());
            if (httpStatus.is2xxSuccessful() || NOT_FOUND.equals(httpStatus)) {
                log.info("deleteBlobData for containerName={}, blobPathAndName={}, httpStatus={}",
                         containerName, blobPathAndName, httpStatus);
            } else {
                String message = String.format("Failed to delete from storage container=%s, blobId=%s, httpStatus=%s",
                                               containerName, blobPathAndName, httpStatus);
                throw new AzureDeleteBlobException(message);
            }

        } catch (Exception e) {
            log.error("Could not delete from storage container={}, blobPathAndName={}", containerName, blobPathAndName, e);
        }
    }
}
