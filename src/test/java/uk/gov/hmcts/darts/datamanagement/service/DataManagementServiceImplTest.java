package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.dao.DataManagementDao;
import uk.gov.hmcts.darts.datamanagement.service.impl.DataManagementServiceImpl;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataManagementServiceImplTest {

    public static final String BLOB_CONTAINER_NAME = "dummy_container";
    public static final UUID BLOB_ID = UUID.randomUUID();
    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    @Mock
    private DataManagementDao dataManagementDao;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    @InjectMocks
    private DataManagementServiceImpl dataManagementService;
    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);
    }

    @Test
    void testGetBlobData() {
        when(dataManagementDao.getBlobContainerClient(BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(dataManagementDao.getBlobClient(blobContainerClient, BLOB_ID)).thenReturn(blobClient);
        when(blobClient.downloadContent()).thenReturn(BINARY_DATA);
        BinaryData blobData = dataManagementService.getBlobData(BLOB_CONTAINER_NAME, BLOB_ID);
        assertNotNull(blobData);
        assertEquals(BINARY_DATA, blobData);
    }

    @Test
    void testSaveBlobData() {
        when(dataManagementDao.getBlobContainerClient(BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(dataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);
        UUID blobId = dataManagementService.saveBlobData(BLOB_CONTAINER_NAME, BINARY_DATA);
        assertNotNull(blobId);
    }

    @Test
    void testDeleteBlobData() {
        when(dataManagementConfiguration.getDeleteTimeout()).thenReturn(20);
        when(dataManagementDao.getBlobContainerClient(BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(dataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);
        when(blobClient.deleteWithResponse(any(), any(), any(), any())).thenReturn(null);

        dataManagementService.deleteBlobData(BLOB_CONTAINER_NAME, BLOB_ID);

        verify(blobClient, times(1)).deleteWithResponse(any(), any(), any(), any());
    }
}
