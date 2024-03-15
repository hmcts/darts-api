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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.valueOf;

@Service
@Slf4j
@Profile("!intTest")
@RequiredArgsConstructor
public class ArmServiceImpl implements ArmService {

    private static final String FILE_PATH_DELIMITER = "/";
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

    /**
     * Returns a list of the blobs in the response dropzone containing the specified filename with full path.
     *
     * @param containerName name of container
     * @param filename      name of file to look for
     * @return list of the blobs in the response dropzone containing the specified filename with full path
     *     e.g. returns: dropzone/DARTS/response/123_456_1_2d50a0bbde794e0ea9f4918aafeaccde_1_iu.rsp
     */
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
                log.info("Virtual directory prefix: {}", FILE_PATH_DELIMITER + blob.getName());
                listBlobsHierarchicalListing(blobContainerClient, FILE_PATH_DELIMITER, blob.getName());
            } else {
                log.info("Blob name: {}", blob.getName());
                files.add(blob.getName());
            }
        });
        return files;
    }

    public PagedIterable<BlobItem> listBlobsHierarchicalListing(BlobContainerClient blobContainerClient,
                                                                String delimiter,
                                                                String prefix) {

        ListBlobsOptions options = new ListBlobsOptions().setPrefix(prefix);
        Duration timeout = Duration.of(TIMEOUT, ChronoUnit.SECONDS);
        return blobContainerClient.listBlobsByHierarchy(delimiter, options, timeout);
    }

    public BinaryData getBlobData(String containerName, String blobPathAndName) {

        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        BlobClient blobClient = armDataManagementDao.getBlobClient(containerClient, blobPathAndName);
        if (Boolean.FALSE.equals(blobClient.exists())) {
            log.error("Blob {} does not exist in {} container", blobPathAndName, containerName);
            return null;
        }
        return blobClient.downloadContent();
    }

    @Override
    public boolean deleteBlobData(String containerName, String blobPathAndName) {
        try {
            BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
            BlobClient blobClient = armDataManagementDao.getBlobClient(containerClient, blobPathAndName);

            log.info("About to delete blob data for containerName={}, blobPathAndName={}",
                     containerName, blobPathAndName);
            Response<Boolean> response = blobClient.deleteIfExistsWithResponse(
                DeleteSnapshotsOptionType.INCLUDE,
                null,
                Duration.of(TIMEOUT, ChronoUnit.SECONDS),
                null
            );

            HttpStatus httpStatus = valueOf(response.getStatusCode());
            log.debug("Attempted to delete blob data for containerName={}, blobPathAndName={}, httpStatus={}",
                      containerName, blobPathAndName, httpStatus);
            if (httpStatus.is2xxSuccessful() || NOT_FOUND.equals(httpStatus)) {
                return true;
            } else {
                String message = String.format("Failed to delete from storage container=%s, blobPathAndName=%s, httpStatus=%s",
                                               containerName, blobPathAndName, httpStatus);
                throw new AzureDeleteBlobException(message);
            }

        } catch (Exception e) {
            log.error("Could not delete from storage container={}, blobPathAndName {}",
                      containerName, blobPathAndName, e);
            return false;
        }
    }

}
