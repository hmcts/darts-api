package uk.gov.hmcts.darts.common.datamanagement.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DataManagementAzureClientFactoryImpl;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataManagementAzureClientFactoryImplTest {

    @InjectMocks
    private DataManagementAzureClientFactoryImpl dataManagementFactoryImpl;

    public static final String BLOB_CONTAINER_NAME = "dummy_container";
    public static final String BLOB_ID = UUID.randomUUID().toString();
    private static final String CONNECTION_STRING = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;" +
        "AccountKey=KBHBeksoGMGw;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;";

    private static final String ALTERNATE_CONNECTION_STRING = "DefaultEndpointsProtocol=http;AccountName=devstoreaccount2;" +
            "AccountKey=KBHBeksoGMGw;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;";
    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    private BlobServiceClient blobServiceClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = Mockito.mock(BlobContainerClient.class);
        blobClient = Mockito.mock(BlobClient.class);
    }

    @Test
    void testGetBlobContainerClient() {
        BlobContainerClient blobContainerClient = dataManagementFactoryImpl.getBlobContainerClient(
                BLOB_CONTAINER_NAME, dataManagementFactoryImpl.getBlobServiceClient(CONNECTION_STRING));
        assertNotNull(blobContainerClient);

        Assertions.assertSame(blobContainerClient,
                              dataManagementFactoryImpl.getBlobContainerClient(BLOB_CONTAINER_NAME,
                                                                               dataManagementFactoryImpl.getBlobServiceClient(CONNECTION_STRING)));
    }

    @Test
    void testGetServiceClientCaching() {
        BlobServiceClient serviceClient = dataManagementFactoryImpl.getBlobServiceClient(CONNECTION_STRING);
        BlobServiceClient serviceClient1 = dataManagementFactoryImpl.getBlobServiceClient(CONNECTION_STRING);
        BlobServiceClient serviceClient2 = dataManagementFactoryImpl.getBlobServiceClient(ALTERNATE_CONNECTION_STRING);

        Assertions.assertSame(serviceClient, serviceClient1);
        Assertions.assertNotSame(serviceClient, serviceClient2);
    }

    @Test
    void testGetServiceClientSasCaching() {
        BlobServiceClient serviceClient = dataManagementFactoryImpl.getBlobServiceClient(CONNECTION_STRING);
        BlobServiceClient serviceClient1 = dataManagementFactoryImpl.getBlobServiceClient(CONNECTION_STRING);

        Assertions.assertSame(serviceClient, serviceClient1);
    }

    @Test
    void testGetBlobClient() {
        when(blobContainerClient.getBlobClient(String.valueOf(BLOB_ID))).thenReturn(blobClient);
        BlobClient blobClient = dataManagementFactoryImpl.getBlobClient(blobContainerClient, BLOB_ID);
        assertNotNull(blobClient);
    }
}