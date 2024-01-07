package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.PagedResponse;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.arm.service.ArmService;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@Profile("!intTest")
@RequiredArgsConstructor
public class ArmServiceImpl implements ArmService {

    public static final String FILE_PATH_DELIMITER = "/";
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



    public Map<String, BlobItem> listCollectedBlobs(String containerName, String filename) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getArmCollectedDropZone() + filename;

        return listBlobs(containerClient, prefix);
    }

    public Map<String, BlobItem> listResponseBlobs(String containerName, String filename) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getArmResponseDropZone() + filename;

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

    public Iterable<PagedResponse<BlobItem>> listBlobs(BlobContainerClient blobContainerClient,
                                                       boolean retrieveDeletedBlobs,
                                                       int maxResultsPerPage) {
        ListBlobsOptions options = new ListBlobsOptions()
            .setMaxResultsPerPage(maxResultsPerPage)
            .setDetails(new BlobListDetails()
                            .setRetrieveDeletedBlobs(retrieveDeletedBlobs));

        return blobContainerClient.listBlobs(options, null).iterableByPage();
    }

    public PagedIterable<BlobItem> listBlobsHierarchicalListing(BlobContainerClient blobContainerClient,
                                                                String delimiter,
                                                                String prefix /* ="" */) {

        ListBlobsOptions options = new ListBlobsOptions()
            .setPrefix(prefix);

        return blobContainerClient.listBlobsByHierarchy(delimiter, options, null);
    }

    public Iterable<PagedResponse<BlobItem>> listBlobsFlat(String containerName, String folder) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        return listBlobs(containerClient, false, 3);
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
