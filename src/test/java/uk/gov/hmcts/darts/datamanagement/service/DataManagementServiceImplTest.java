package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.datamanagement.dao.DataManagementDao;
import uk.gov.hmcts.darts.datamanagement.service.impl.DataManagementServiceImpl;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class DataManagementServiceImplTest {

    @InjectMocks
    private DataManagementServiceImpl dataManagementService;
    @Mock
    private DataManagementDao dataManagementDao;
    public static final String BLOB_CONTAINER_NAME = "dummy_container";
    public static final UUID BLOB_ID = UUID.randomUUID();
    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = Mockito.mock(BlobContainerClient.class);
        blobClient = Mockito.mock(BlobClient.class);
    }

    @Test
    void testGetAudioBlobData() {
        Mockito.when(dataManagementDao.getBlobContainerClient(BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        Mockito.when(dataManagementDao.getBlobClient(blobContainerClient, BLOB_ID)).thenReturn(blobClient);
        Mockito.when(blobClient.downloadContent()).thenReturn(BINARY_DATA);
        BinaryData blobData = dataManagementService.getAudioBlobData(BLOB_CONTAINER_NAME, BLOB_ID);
        assertNotNull(blobData);
        assertEquals(BINARY_DATA, blobData);
    }

    @Test
    void testSaveAudioBlobData() {
        Mockito.when(dataManagementDao.getBlobContainerClient(BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        Mockito.when(dataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);
        UUID blobId = dataManagementService.saveAudioBlobData(BLOB_CONTAINER_NAME, BINARY_DATA);
        assertNotNull(blobId);
    }
}
