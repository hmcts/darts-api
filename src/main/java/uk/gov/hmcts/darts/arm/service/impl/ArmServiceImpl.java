package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.ListBlobsOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Profile("!intTest")
@RequiredArgsConstructor
public class ArmServiceImpl implements ArmService {

    public static final String FILE_PATH_DELIMITER = "/";
    public static final int STATUS_CODE_202 = 202;
    protected static final long TIMEOUT = 60;

    private final ArmDataManagementDao armDataManagementDao;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    @Override
    public String saveBlobData(String containerName, String filename, BinaryData binaryData) {

        String blobPathAndName = armDataManagementConfiguration.getFolders().getSubmission() + filename;
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        BlobClient client = armDataManagementDao.getBlobClient(containerClient, blobPathAndName);
        client.upload(binaryData);
        return filename;
    }

    @Override
    public String saveBlobData(String containerName, BinaryData binaryData, String blobPathAndName) {

        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        BlobClient client = armDataManagementDao.getBlobClient(containerClient, blobPathAndName);
        client.upload(binaryData);
        return blobPathAndName;
    }

    public List<String> listSubmissionBlobs(String containerName, String filename) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getFolders().getSubmission() + filename;

        return listBlobs(containerClient, prefix);
    }

    public List<String> listCollectedBlobs(String containerName, String filename) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getFolders().getCollected() + filename;

        return listBlobs(containerClient, prefix);
    }

    public List<String> listResponseBlobs(String containerName, String filename) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getFolders().getResponse() + filename;

        return listBlobs(containerClient, prefix);
    }

    public List<String> listBlobs(BlobContainerClient blobContainerClient, String prefix) {
        List<String> files = new ArrayList<>();
        log.debug("About to list files for {}", prefix);
        listBlobsHierarchicalListing(blobContainerClient, FILE_PATH_DELIMITER, prefix).forEach(blob -> {
            if (Boolean.TRUE.equals(blob.isPrefix())) {
                log.info("Virtual directory prefix: {}}", FILE_PATH_DELIMITER + blob.getName());
                listBlobsHierarchicalListing(blobContainerClient, FILE_PATH_DELIMITER, blob.getName());
            } else {
                log.info("Blob name: {}}", blob.getName());
                files.add(blob.getName());
            }
        });
        return files;
    }

    public PagedIterable<BlobItem> listBlobsHierarchicalListing(BlobContainerClient blobContainerClient,
                                                                String delimiter,
                                                                String prefix /* ="" */) {

        ListBlobsOptions options = new ListBlobsOptions().setPrefix(prefix);
        Duration timeout = Duration.of(TIMEOUT, ChronoUnit.SECONDS);
        return blobContainerClient.listBlobsByHierarchy(delimiter, options, timeout);
    }

    public BinaryData getBlobData(String containerName, String blobName) {

        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        BlobClient blobClient = armDataManagementDao.getBlobClient(containerClient, blobName);
        if (Boolean.FALSE.equals(blobClient.exists())) {
            log.error("Blob {} does not exist in {} container", blobName, containerName);
            return null;
        }
        return blobClient.downloadContent();
    }

    public boolean deleteResponseBlob(String containerName, String filename) {
        String blobname = armDataManagementConfiguration.getFolders().getSubmission() + filename;
        return deleteBlobData(containerName, blobname);
    }

    public boolean deleteBlobData(String containerName, String blobPathAndName) {
        try {
            BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
            BlobClient blobClient = armDataManagementDao.getBlobClient(containerClient, blobPathAndName);

            Response<Boolean> response = blobClient.deleteIfExistsWithResponse(
                DeleteSnapshotsOptionType.INCLUDE,
                null,
                Duration.of(TIMEOUT, ChronoUnit.SECONDS),
                null
            );

            log.debug("Attempted to delete blob data for blob path {}, Returned status code {}", blobPathAndName, response.getStatusCode());
            if (STATUS_CODE_202 != response.getStatusCode()) {
                throw new AzureDeleteBlobException("Failed to delete blob " + blobPathAndName + " because of status code: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Could not delete from container: " + containerName + " blobPathAndName: " + blobPathAndName, e.getMessage(), e);
            return false;
        }
        return true;
    }
}
