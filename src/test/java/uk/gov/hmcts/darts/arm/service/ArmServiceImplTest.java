package uk.gov.hmcts.darts.arm.service;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.arm.service.impl.ArmServiceImpl;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        when(blobContainerClient.listBlobs(any(), any(), any())).thenReturn(pagedIterable);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "1_1_1";
        Map<String, BlobItem> blobs = armService.listSubmissionBlobs(ARM_BLOB_CONTAINER_NAME, prefix);
        assertNotNull(blobs);
    }

    @Test
    void testListCollectedBlobs() {
        PagedIterable<BlobItem> pagedIterable = (PagedIterable<BlobItem>) mock(PagedIterable.class);
        when(blobContainerClient.listBlobsByHierarchy(any(), any(), any())).thenReturn(pagedIterable);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "1_1_1";
        Map<String, BlobItem> blobs = armService.listCollectedBlobs(ARM_BLOB_CONTAINER_NAME, prefix);
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
        Map<String, BlobItem> blobs = armService.listResponseBlobs(ARM_BLOB_CONTAINER_NAME, prefix);
        assertNotNull(blobs);
    }

    @Test
    void testGetBlobData() {
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.downloadContent()).thenReturn(BINARY_DATA);

        BinaryData binaryData = armService.getBlobData(ARM_BLOB_CONTAINER_NAME, "blobname");
        assertEquals(BINARY_DATA, binaryData);
    }

}
