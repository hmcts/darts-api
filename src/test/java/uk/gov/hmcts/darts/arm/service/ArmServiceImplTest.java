package uk.gov.hmcts.darts.arm.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.service.impl.ArmServiceImpl;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ArmServiceImplTest {

    private static final String ARM_BLOB_CONTAINER_NAME = "arm_dummy_container";
    private static final String BLOB_FILENAME = "test_filename";
    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    private static final String TEST_DROP_ZONE = "dummy/dropzone/";
    @Mock
    private ArmDataManagementDao armDataManagementDao;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @InjectMocks
    private ArmServiceImpl armService;
    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);
    }

    @Test
    void testSaveBlobDataUsingFilename() {
        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_BINARY_STRING);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);

        String blobName = armService.saveBlobData(ARM_BLOB_CONTAINER_NAME, BLOB_FILENAME, BINARY_DATA);
        assertNotNull(blobName);
        assertEquals(BLOB_FILENAME, blobName);
    }

    @Test
    void testSaveBlobDataUsingBlobPathAndFilename() {
        String blobPathAndFilename = TEST_DROP_ZONE + BLOB_FILENAME;
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);
        String blobName = armService.saveBlobData(ARM_BLOB_CONTAINER_NAME, BINARY_DATA, blobPathAndFilename);
        assertNotNull(blobName);
        assertEquals(blobPathAndFilename, blobName);
    }

    @Test
    void testListSubmissionBlobs() {
        PagedIterable<BlobItem> pagedIterable = (PagedIterable<BlobItem>) mock(PagedIterable.class);
        when(blobContainerClient.listBlobsByHierarchy(any(), any(), any())).thenReturn(pagedIterable);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "1_1_1";
        List<String> blobs = armService.listSubmissionBlobs(ARM_BLOB_CONTAINER_NAME, prefix);
        assertNotNull(blobs);
    }

    @Test
    void testListResponseBlobs() {
        PagedIterable<BlobItem> pagedIterable = (PagedIterable<BlobItem>) mock(PagedIterable.class);
        when(blobContainerClient.listBlobsByHierarchy(any(), any(), any())).thenReturn(pagedIterable);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "1_1_1";
        List<String> blobs = armService.listResponseBlobs(ARM_BLOB_CONTAINER_NAME, prefix);
        assertNotNull(blobs);
    }

    @Test
    void testGetBlobData() {
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(Boolean.TRUE);
        when(blobClient.downloadContent()).thenReturn(BINARY_DATA);

        BinaryData binaryData = armService.getBlobData(ARM_BLOB_CONTAINER_NAME, "blobname");
        assertEquals(BINARY_DATA, binaryData);
    }

    @Test
    void testDeleteResponseBlobIsSuccessful() {
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);

        Response<Boolean> response = mock(Response.class);
        when(response.getStatusCode()).thenReturn(202);

        when(blobClient.deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, Duration.of(60, ChronoUnit.SECONDS), null)).thenReturn(response);

        boolean result = armService.deleteBlobData(ARM_BLOB_CONTAINER_NAME, "blobPathAndName");
        assertTrue(result);
    }

    @Test
    void testDeleteResponseBlobIsNotSuccessful() {
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);

        Response<Boolean> response = mock(Response.class);
        when(response.getStatusCode()).thenReturn(500);

        when(blobClient.deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, Duration.of(60, ChronoUnit.SECONDS), null)).thenReturn(response);

        boolean result = armService.deleteBlobData(ARM_BLOB_CONTAINER_NAME, "blobPathAndName");
        assertFalse(result);
    }

    @Test
    @Disabled
    void testListSubmissionBlobsWithMarker() {
        PagedIterable<BlobItem> pagedIterable = (PagedIterable<BlobItem>) mock(PagedIterable.class);
        when(blobContainerClient.listBlobsByHierarchy(any(), any(), any())).thenReturn(pagedIterable);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "1_1_1";
        ContinuationTokenBlobs continuationTokenBlobs = armService.listSubmissionBlobsWithMarker(
            ARM_BLOB_CONTAINER_NAME, prefix, 10, null);
        assertNotNull(continuationTokenBlobs);
    }

    public static <T> void mockIterable(Iterable<T> iterable, T... values) {
        Iterator<T> mockIterator = mock(Iterator.class);
        when(iterable.iterator()).thenReturn(mockIterator);

        if (values.length == 0) {
            when(mockIterator.hasNext()).thenReturn(false);
            return;
        } else if (values.length == 1) {
            when(mockIterator.hasNext()).thenReturn(true, false);
            when(mockIterator.next()).thenReturn(values[0]);
        } else {
            // build boolean array for hasNext()
            Boolean[] hasNextResponses = new Boolean[values.length];
            for (int i = 0; i < hasNextResponses.length - 1; i++) {
                hasNextResponses[i] = true;
            }
            hasNextResponses[hasNextResponses.length - 1] = false;
            when(mockIterator.hasNext()).thenReturn(true, hasNextResponses);
            T[] valuesMinusTheFirst = Arrays.copyOfRange(values, 1, values.length);
            when(mockIterator.next()).thenReturn(values[0], valuesMinusTheFirst);
        }
    }

    @Test
    @Disabled
    void testListResponseBlobsWithMarker() {
        PagedIterable<BlobItem> pagedIterable = (PagedIterable<BlobItem>) mock(PagedIterable.class);
        mockIterable(pagedIterable, new BlobItem());
        when(blobContainerClient.listBlobs(any(), any(), any())).thenReturn(pagedIterable);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "1_1_1";
        ContinuationTokenBlobs continuationTokenBlobs = armService.listResponseBlobsWithMarker(
            ARM_BLOB_CONTAINER_NAME, prefix, 10, null);
        assertNotNull(continuationTokenBlobs);
    }
}
