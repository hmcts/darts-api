package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponseImpl;
import uk.gov.hmcts.darts.datamanagement.service.impl.DataManagementServiceImpl;
import uk.gov.hmcts.darts.util.AzureCopyUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
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
    public Response<Boolean> responseMock;
    @Mock
    private DataManagementAzureClientFactory dataManagementFactory;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    @Mock
    private AzureCopyUtil azureCopyUtil;
    @InjectMocks
    private DataManagementServiceImpl dataManagementService;
    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    @TempDir
    private File tempDirectory;


    private BlobServiceClient serviceClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);
        serviceClient = mock(BlobServiceClient.class);
        lenient().when(dataManagementFactory.getBlobServiceClient(Mockito.notNull())).thenReturn(serviceClient);
        lenient().when(dataManagementConfiguration.getBlobStorageAccountConnectionString()).thenReturn("connection");
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
    void testSaveBlobDataViaInputStream() {
        // Given
        when(dataManagementConfiguration.getBlobClientBlockSizeBytes())
            .thenReturn(1L);
        when(dataManagementConfiguration.getBlobClientMaxSingleUploadSizeBytes())
            .thenReturn(1L);
        when(dataManagementConfiguration.getBlobClientMaxConcurrency())
            .thenReturn(1);
        when(dataManagementConfiguration.getBlobClientTimeout())
            .thenReturn(Duration.ofMinutes(1));
        when(dataManagementFactory.getBlobClient(any(), any()))
            .thenReturn(blobClient);
        when(blobClient.getBlobName())
            .thenReturn(BLOB_ID.toString());

        // When
        BlobClientUploadResponseImpl blobClientUploadResponse = dataManagementService.saveBlobData(BLOB_CONTAINER_NAME,
                                                                                                   new ByteArrayInputStream(TEST_BINARY_STRING.getBytes()));

        // Then
        assertEquals(BLOB_ID, blobClientUploadResponse.getBlobName());
    }

    @Test
    void downloadBlobToFile() {
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(blobContainerClient, BLOB_ID)).thenReturn(blobClient);

        dataManagementService.downloadBlobToFile(BLOB_CONTAINER_NAME, BLOB_ID, tempDirectory.getAbsolutePath());

        var fileNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(blobClient).downloadToFile(fileNameCaptor.capture());
        assertThat(fileNameCaptor.getValue().contains(tempDirectory.getAbsolutePath())).isTrue();
    }

    @Test
    void testDeleteBlobData() throws AzureDeleteBlobException {
        when(dataManagementConfiguration.getDeleteTimeout()).thenReturn(20);
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);
        when(responseMock.getStatusCode()).thenReturn(202);
        when(blobClient.deleteIfExistsWithResponse(any(), any(), any(), any())).thenReturn(responseMock);

        dataManagementService.deleteBlobData(BLOB_CONTAINER_NAME, BLOB_ID);

        verify(blobClient, times(1)).deleteIfExistsWithResponse(any(), any(), any(), any());
    }

    @Test
    void testDeleteBlobDataWithFailure() {
        when(dataManagementConfiguration.getDeleteTimeout()).thenReturn(20);
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);
        when(responseMock.getStatusCode()).thenReturn(400);
        when(blobClient.deleteIfExistsWithResponse(any(), any(), any(), any())).thenReturn(responseMock);

        assertThrows(AzureDeleteBlobException.class, () -> dataManagementService.deleteBlobData(BLOB_CONTAINER_NAME, BLOB_ID));
    }

    @Test
    void testDeleteBlobDataWithNotFoundError() {
        when(dataManagementConfiguration.getDeleteTimeout()).thenReturn(20);
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);
        when(responseMock.getStatusCode()).thenReturn(404);
        when(blobClient.deleteIfExistsWithResponse(any(), any(), any(), any())).thenReturn(responseMock);

        assertDoesNotThrow(() -> dataManagementService.deleteBlobData(BLOB_CONTAINER_NAME, BLOB_ID));
    }

    @Test
    void testDeleteBlobDataWithTimeout() {
        when(dataManagementConfiguration.getDeleteTimeout()).thenReturn(0);
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);
        when(blobClient.deleteIfExistsWithResponse(any(), any(), any(), any())).thenThrow(new RuntimeException("timeout"));

        assertThrows(AzureDeleteBlobException.class, () -> dataManagementService.deleteBlobData(BLOB_CONTAINER_NAME, BLOB_ID));
    }

    @Test
    void testDownloadData() throws Exception {
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(true);
        when(dataManagementConfiguration.getTempBlobWorkspace()).thenReturn(System.getProperty("java.io.tmpdir") + "/tempWorkspace");

        dataManagementService.downloadData(DatastoreContainerType.UNSTRUCTURED, BLOB_CONTAINER_NAME, BLOB_ID);
        verify(blobClient, times(1)).downloadStream(any());
    }

    @Test
    void testCopyData() {
        when(dataManagementConfiguration.getContainerSasUrl("darts-inbound-container"))
            .thenReturn("https://dartssastg.blob....net/darts-inbound-container?sp=r&st=2024-05-23T13...%3D");
        when(dataManagementConfiguration.getContainerSasUrl("darts-unstructured"))
            .thenReturn("https://dartssastg.blob....net/darts-unstructured?sp=r&st=2024-05-23T13...%3D");
        UUID sourceBlobId = UUID.randomUUID();
        UUID destinationUuid = UUID.randomUUID();

        dataManagementService.copyBlobData("darts-inbound-container",
                                           "darts-unstructured",
                                           sourceBlobId.toString(),
                                           destinationUuid.toString());

        verify(azureCopyUtil).copy(
            "https://dartssastg.blob....net/darts-inbound-container/" + sourceBlobId + "?sp=r&st=2024-05-23T13...%3D",
            "https://dartssastg.blob....net/darts-unstructured/" + destinationUuid + "?sp=r&st=2024-05-23T13...%3D"
        );
    }

    @Test
    void testCopyDataToArm() {
        when(dataManagementConfiguration.getArmContainerName()).thenReturn("dropzone");
        when(dataManagementConfiguration.getContainerSasUrl("darts-unstructured"))
            .thenReturn("https://dartssastg.blob....net/darts-unstructured?sp=r&st=2024-05-23T13...%3D");
        when(dataManagementConfiguration.getContainerSasUrl("dropzone"))
            .thenReturn("https://dartsarmstg.blob....net/dropzone/DARTS?sp=rwdl&st=2024-03-28T08...sdd=1");
        UUID sourceBlobId = UUID.randomUUID();
        String destinationLocation = "DARTS/submission/" + UUID.randomUUID();

        dataManagementService.copyBlobData("darts-unstructured",
                                           "dropzone",
                                           sourceBlobId.toString(),
                                           destinationLocation);

        verify(azureCopyUtil).copy(
            "https://dartssastg.blob....net/darts-unstructured/" + sourceBlobId + "?sp=r&st=2024-05-23T13...%3D",
            "https://dartsarmstg.blob....net/dropzone/" + destinationLocation + "?sp=rwdl&st=2024-03-28T08...sdd=1"
        );
    }

    @Test
    void testCopyDataRethrowsExceptionsAsDartsApiException() {
        when(dataManagementConfiguration.getContainerSasUrl("darts-inbound-container"))
            .thenReturn("https://dartssastg.blob....net/darts-inbound-container?sp=r&st=2024-05-23T13...%3D");
        when(dataManagementConfiguration.getContainerSasUrl("darts-unstructured"))
            .thenReturn("https://dartssastg.blob....net/darts-unstructured?sp=r&st=2024-05-23T13...%3D");
        doThrow(RuntimeException.class).when(azureCopyUtil).copy(any(), any());

        assertThrows(DartsException.class, () ->
            dataManagementService.copyBlobData(
                "darts-inbound-container",
                "darts-unstructured",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()));
    }
}