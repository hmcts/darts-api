package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.impl.DataManagementServiceImpl;

import java.io.OutputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    public Response<Void> responseMock;
    @Mock
    private DataManagementAzureClientFactory dataManagementFactory;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    @InjectMocks
    private DataManagementServiceImpl dataManagementService;
    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    private BlobServiceClient serviceClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);
        serviceClient = mock(BlobServiceClient.class);
        when(dataManagementFactory.getBlobServiceClient(Mockito.notNull())).thenReturn(serviceClient);
        when(dataManagementConfiguration.getBlobStorageAccountConnectionString()).thenReturn("connection");
    }

    @Test
    void testGetBlobData() {
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(blobContainerClient, BLOB_ID)).thenReturn(blobClient);
        when(blobClient.downloadContent()).thenReturn(BINARY_DATA);
        BinaryData blobData = dataManagementService.getBlobData(BLOB_CONTAINER_NAME, BLOB_ID);
        assertNotNull(blobData);
        assertEquals(BINARY_DATA, blobData);
    }

    @Test
    void testSaveBlobData() {
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);
        UUID blobId = dataManagementService.saveBlobData(BLOB_CONTAINER_NAME, BINARY_DATA);
        assertNotNull(blobId);
    }

    @Test
    void testDeleteBlobData() throws AzureDeleteBlobException {
        when(dataManagementConfiguration.getDeleteTimeout()).thenReturn(20);
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);
        when(responseMock.getStatusCode()).thenReturn(202);
        when(blobClient.deleteWithResponse(any(), any(), any(), any())).thenReturn(responseMock);

        dataManagementService.deleteBlobData(BLOB_CONTAINER_NAME, BLOB_ID);

        verify(blobClient, times(1)).deleteWithResponse(any(), any(), any(), any());
    }

    @Test
    void testDeleteBlobDataWithFailure() {
        when(dataManagementConfiguration.getDeleteTimeout()).thenReturn(20);
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);
        when(responseMock.getStatusCode()).thenReturn(400);
        when(blobClient.deleteWithResponse(any(), any(), any(), any())).thenReturn(responseMock);

        assertThrows(AzureDeleteBlobException.class, () -> dataManagementService.deleteBlobData(BLOB_CONTAINER_NAME, BLOB_ID));
    }

    @Test
    void testDeleteBlobDataWithTimeout() {
        when(dataManagementConfiguration.getDeleteTimeout()).thenReturn(0);
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);
        when(blobClient.deleteWithResponse(any(), any(), any(), any())).thenThrow(new RuntimeException("timeout"));

        assertThrows(AzureDeleteBlobException.class, () -> dataManagementService.deleteBlobData(BLOB_CONTAINER_NAME, BLOB_ID));
    }

    @Test
    void testDownloadData() {
        OutputStream stream = Mockito.mock(OutputStream.class);

        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);

        DownloadResponseMetaData downloadResponseMetaData = new DownloadResponseMetaData(stream);

        dataManagementService.downloadData(DatastoreContainerType.UNSTRUCTURED, BLOB_CONTAINER_NAME, BLOB_ID, downloadResponseMetaData);

        Assertions.assertTrue(downloadResponseMetaData.isSuccessfulDownload());
        verify(blobClient, times(1)).downloadStream(any());
    }
}