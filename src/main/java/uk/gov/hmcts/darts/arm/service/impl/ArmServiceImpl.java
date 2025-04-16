package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.PagedResponse;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.ListBlobsOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.valueOf;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
public class ArmServiceImpl implements ArmService {

    private static final String FILE_PATH_DELIMITER = "/";
    private static final long TIMEOUT = 60;
    private static final String DEFAULT_CONTINUATION_TOKEN_DURATION = "PT60M";

    private final ArmDataManagementDao armDataManagementDao;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    private final Duration continuationTokenDuration;

    public ArmServiceImpl(ArmDataManagementDao armDataManagementDao, ArmDataManagementConfiguration armDataManagementConfiguration) {
        this.armDataManagementDao = armDataManagementDao;
        this.armDataManagementConfiguration = armDataManagementConfiguration;
        if (nonNull(armDataManagementConfiguration.getContinuationTokenDuration())) {
            continuationTokenDuration = Duration.parse(armDataManagementConfiguration.getContinuationTokenDuration());
        } else {
            continuationTokenDuration = Duration.parse(DEFAULT_CONTINUATION_TOKEN_DURATION);
        }
    }

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

    @Override
    public List<String> listSubmissionBlobs(String containerName, String filename) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getFolders().getSubmission() + filename;

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
    @Override
    public List<String> listResponseBlobs(String containerName, String filename) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getFolders().getResponse() + filename;

        return listBlobs(containerClient, prefix);
    }

    private List<String> listBlobs(BlobContainerClient blobContainerClient, String prefix) {
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

    private PagedIterable<BlobItem> listBlobsHierarchicalListing(BlobContainerClient blobContainerClient,
                                                                 String delimiter,
                                                                 String prefix) {

        ListBlobsOptions options = new ListBlobsOptions().setPrefix(prefix);
        Duration timeout = Duration.of(TIMEOUT, ChronoUnit.SECONDS);
        return blobContainerClient.listBlobsByHierarchy(delimiter, options, timeout);
    }

    @Override
    public List<String> listSubmissionBlobsUsingBatch(String containerName, String filename, Integer batchSize) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getFolders().getSubmission() + filename;
        return listBlobsUsingBatch(containerClient, prefix, batchSize);
    }

    @Override
    public List<String> listResponseBlobsUsingBatch(String containerName, String filename, Integer batchSize) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getFolders().getResponse() + filename;
        return listBlobsUsingBatch(containerClient, prefix, batchSize);
    }

    @SuppressWarnings({"PMD.CloseResource"})
    private List<String> listBlobsUsingBatch(BlobContainerClient blobContainerClient, String blobPathAndName, Integer batchSize) {
        List<String> files = new ArrayList<>();
        log.debug("About to list files for {} with batch size {}", blobPathAndName, batchSize);
        ListBlobsOptions options = new ListBlobsOptions()
            .setPrefix(blobPathAndName)
            .setMaxResultsPerPage(batchSize);
        Duration timeout = Duration.of(TIMEOUT, ChronoUnit.SECONDS);

        int pageNumber = 0;
        Iterable<PagedResponse<BlobItem>> blobPages = blobContainerClient.listBlobs(options, timeout).iterableByPage();
        for (PagedResponse<BlobItem> page : blobPages) {
            log.debug("Page {}", ++pageNumber);
            page.getElements().forEach(withCounter((counter, blob) -> {
                String blobName = blob.getName();
                files.add(blobName);
                log.debug("{} found blob {}", counter, blobName);
            }));
            if (pageNumber == 1) {
                break;
            }
        }
        log.info("Total blobs found {}", files.size());
        return files;
    }

    public static <T> Consumer<T> withCounter(BiConsumer<Integer, T> consumer) {
        AtomicInteger counter = new AtomicInteger(0);
        return item -> consumer.accept(counter.getAndIncrement(), item);
    }

    @Override
    public ContinuationTokenBlobs listSubmissionBlobsWithMarker(String containerName, String filename, Integer batchSize,
                                                                String continuationToken) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getFolders().getSubmission() + filename;

        return listBlobsWithMarker(containerClient, prefix, batchSize, continuationToken);
    }

    @Override
    public ContinuationTokenBlobs listResponseBlobsWithMarker(String containerName, String filename, Integer batchSize,
                                                              String continuationToken) {
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        String prefix = armDataManagementConfiguration.getFolders().getResponse() + filename;

        return listBlobsWithMarker(containerClient, prefix, batchSize, continuationToken);
    }


    private ContinuationTokenBlobs listBlobsWithMarker(BlobContainerClient blobContainerClient,
                                                       String blobPathAndName,
                                                       Integer batchSize,
                                                       String continuationToken) {
        log.debug("About to list files for {} batchSize {} with continuationToken {}", blobPathAndName, batchSize, continuationToken);
        ListBlobsOptions options = new ListBlobsOptions()
            .setPrefix(blobPathAndName)
            .setMaxResultsPerPage(batchSize)
            .setDetails(new BlobListDetails().setRetrieveDeletedBlobs(false));

        ContinuationTokenBlobs continuationTokenBlobs = ContinuationTokenBlobs.builder().build();
        try {
            Iterator<PagedResponse<BlobItem>> response = blobContainerClient.listBlobs(options, continuationToken, continuationTokenDuration)
                .iterableByPage()
                .iterator();

            if (response.hasNext()) {
                extractBlobsUsingContinuationToken(blobPathAndName, response, continuationTokenBlobs);
            }
        } catch (Exception e) {
            log.error("Unable to list blobs with marker for prefix {}", blobPathAndName, e);
        }
        return continuationTokenBlobs;
    }

    private static void extractBlobsUsingContinuationToken(String blobPathAndName, Iterator<PagedResponse<BlobItem>> response,
                                                           ContinuationTokenBlobs continuationTokenBlobs) {
        List<BlobItem> blobs;
        List<String> blobsWithPaths;
        try (PagedResponse<BlobItem> pagedResponse = response.next()) {
            blobs = pagedResponse.getValue();
            blobsWithPaths = new ArrayList<>();
            // Along with page results, get a continuation token
            // which enables the client to "pick up where it left off"
            continuationTokenBlobs.setContinuationToken(pagedResponse.getContinuationToken());

            blobs.forEach(blob -> blobsWithPaths.add(blob.getName()));
            continuationTokenBlobs.setBlobNamesAndPaths(blobsWithPaths);
            log.debug("ContinuationToken {} found {} blobs for path {}",
                      continuationTokenBlobs.getContinuationToken(), blobsWithPaths.size(), blobPathAndName);
        } catch (NoSuchElementException | IOException ioe) {
            log.error("Unable to get next response for blob {}", blobPathAndName, ioe);
        }
    }

    @Override
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
    @SuppressWarnings({"PMD.ExceptionAsFlowControl"})
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

            if (httpStatus.is2xxSuccessful() || NOT_FOUND.equals(httpStatus)) {
                log.info("Successfully deleted blob data for containerName={}, blobPathAndName={}, httpStatus={}",
                         containerName, blobPathAndName, httpStatus);
                return true;
            } else {
                String message = String.format("Failed to delete from storage container=%s, blobPathAndName=%s, httpStatus=%s",
                                               containerName, blobPathAndName, httpStatus);
                throw new AzureDeleteBlobException(message);
            }

        } catch (Exception e) {
            log.error("Could not delete from storage container={}, blobPathAndName {}", containerName, blobPathAndName, e);
            return false;
        }
    }

}