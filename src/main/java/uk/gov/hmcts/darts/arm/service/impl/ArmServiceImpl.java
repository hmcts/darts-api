package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.arm.service.ArmService;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Profile("!intTest")
@RequiredArgsConstructor
public class ArmServiceImpl implements ArmService {

    public static final String FILE_PATH_DELIMITER = "/";
    private static final long TIMEOUT = 60;
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


    public Map<String, BlobItem> listCollectedBlobs(String containerName, String filename) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getArmCollectedDropZone() + filename;

        return listBlobs(containerClient, prefix);
    }

    public Map<String, BlobItem> listResponseBlobs(String containerName, String filename) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getArmSubmissionDropZone() + filename;

        return listBlobs(containerClient, prefix);
    }

    public Map<String, BlobItem> listBlobs(BlobContainerClient blobContainerClient, String prefix) {
        Map<String, BlobItem> blobs = new HashMap<>();
        listBlobsHierarchicalListing(blobContainerClient, FILE_PATH_DELIMITER, prefix).forEach(blob -> {
            if (blob.isPrefix()) {
                log.info("Virtual directory prefix: {}}", FILE_PATH_DELIMITER + blob.getName());
                listBlobsHierarchicalListing(blobContainerClient, FILE_PATH_DELIMITER, blob.getName());
            } else {
                log.info("Blob name: {}}", blob.getName());
                blobs.put(blob.getName(), blob);
            }
        });
        return blobs;
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
        if (!blobClient.exists()) {
            log.error("Blob {} does not exist in {} container", blobName, containerName);
            return null;
        }
        return blobClient.downloadContent();
    }
}
