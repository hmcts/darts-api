package uk.gov.hmcts.darts.datamanagement.dao;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.dao.impl.DataManagementDaoImpl;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class DataManagementDaoImplTest {

    @InjectMocks
    private DataManagementDaoImpl dataManagementDaoImpl;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;

    public static final String BLOB_CONTAINER_NAME = "dummy_container";
    public static final UUID BLOB_ID = UUID.randomUUID();
    private static final String CONNECTION_STRING = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;" +
        "AccountKey=KBHBeksoGMGw;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;";
    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = Mockito.mock(BlobContainerClient.class);
        blobClient = Mockito.mock(BlobClient.class);
    }

    @Test
    void testGetBlobContainerClient() {
        Mockito.when(dataManagementConfiguration.getBlobStorageAccountConnectionString()).thenReturn(CONNECTION_STRING);
        BlobContainerClient blobContainerClient = dataManagementDaoImpl.getBlobContainerClient(BLOB_CONTAINER_NAME);
        assertNotNull(blobContainerClient);
    }

    @Test
    void testGetBlobClient() {
        Mockito.when(blobContainerClient.getBlobClient(String.valueOf(BLOB_ID))).thenReturn(blobClient);
        BlobClient blobClient = dataManagementDaoImpl.getBlobClient(blobContainerClient, BLOB_ID);
        assertNotNull(blobClient);
    }
}
