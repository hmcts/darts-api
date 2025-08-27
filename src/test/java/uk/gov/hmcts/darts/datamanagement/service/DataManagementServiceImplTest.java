package uk.gov.hmcts.darts.datamanagement.service;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponseImpl;
import uk.gov.hmcts.darts.datamanagement.service.impl.DataManagementServiceImpl;
import uk.gov.hmcts.darts.util.AzureCopyUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataManagementServiceImplTest {

    private static final String BLOB_CONTAINER_NAME = "dummy_container";
    private static final String BLOB_ID = UUID.randomUUID().toString();
    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());

    @Mock
    private Response<Boolean> responseMock;
    @Mock
    private DataManagementAzureClientFactory dataManagementFactory;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    @Mock
    private AzureCopyUtil azureCopyUtil;
    @Mock
    private FileContentChecksum fileContentChecksum;
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
        String blobId = dataManagementService.saveBlobData(BLOB_CONTAINER_NAME, BINARY_DATA);
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
            .thenReturn(BLOB_ID);

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
        assertThat(fileNameCaptor.getValue()).contains(tempDirectory.getAbsolutePath());
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
        String sourceBlobId = UUID.randomUUID().toString();
        String destinationUuid = UUID.randomUUID().toString();

        dataManagementService.copyBlobData("darts-inbound-container",
                                           "darts-unstructured",
                                           sourceBlobId,
                                           destinationUuid);

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
        String sourceBlobId = UUID.randomUUID().toString();
        String destinationLocation = "DARTS/submission/" + UUID.randomUUID().toString();

        dataManagementService.copyBlobData("darts-unstructured",
                                           "dropzone",
                                           sourceBlobId,
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

        String uuid1 = UUID.randomUUID().toString();
        String uuid2 = UUID.randomUUID().toString();
        assertThrows(DartsException.class, () ->
            dataManagementService.copyBlobData(
                "darts-inbound-container",
                "darts-unstructured",
                uuid1,
                uuid2));
    }


    @Test
    void positiveGetChecksumTypical() {
        final String checksum = "abc123";
        final byte[] checkSumBytes = checksum.getBytes();
        final String blobId = "431318c8-97db-415c-b321-120c48f0ffe2";
        final String containerName = "container123";
        final String connectionString = "connectionString";

        BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobProperties blobProperties = mock(BlobProperties.class);
        when(blobProperties.getContentMd5())
            .thenReturn(checkSumBytes);
        BlobClient blobClient = mock(BlobClient.class);
        when(blobClient.getProperties())
            .thenReturn(blobProperties);

        when(dataManagementFactory.getBlobServiceClient(any()))
            .thenReturn(blobServiceClient);
        when(dataManagementFactory.getBlobContainerClient(any(), any()))
            .thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any()))
            .thenReturn(blobClient);
        when(dataManagementConfiguration.getBlobStorageAccountConnectionString())
            .thenReturn(connectionString);
        when(fileContentChecksum.encodeToString(checkSumBytes))
            .thenReturn(checksum);

        when(blobClient.exists()).thenReturn(true);


        assertThat(dataManagementService.getChecksum(containerName, blobId))
            .isEqualTo(checksum);

        verify(dataManagementConfiguration, times(1)).getBlobStorageAccountConnectionString();
        verify(dataManagementFactory, times(1)).getBlobServiceClient(connectionString);
        verify(dataManagementFactory, times(1)).getBlobContainerClient(containerName, blobServiceClient);
        verify(dataManagementFactory, times(1)).getBlobClient(blobContainerClient, blobId);
        verify(blobClient, times(2)).exists();
        verify(blobClient, times(1)).getProperties();
        verify(blobProperties, times(1)).getContentMd5();
        verify(fileContentChecksum, times(1)).encodeToString(checkSumBytes);
    }

    @Test
    void negativeGetChecksumFileNotFoundExistsFalse() {
        assertGetChecksumNotFound(false);
    }

    @Test
    void negativeGetChecksumFileNotFoundExistsNull() {
        assertGetChecksumNotFound(null);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void negativeGetChecksumChecksumNotFound(byte[] checksumBytes) {
        final String blobId = "431318c8-97db-415c-b321-120c48f0ffe2";
        final String containerName = "container123";
        final String connectionString = "connectionString";

        BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobProperties blobProperties = mock(BlobProperties.class);
        when(blobProperties.getContentMd5()).thenReturn(checksumBytes);
        BlobClient blobClient = mock(BlobClient.class);
        when(blobClient.getProperties()).thenReturn(blobProperties);

        when(dataManagementFactory.getBlobServiceClient(any()))
            .thenReturn(blobServiceClient);
        when(dataManagementFactory.getBlobContainerClient(any(), any()))
            .thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any()))
            .thenReturn(blobClient);
        when(dataManagementConfiguration.getBlobStorageAccountConnectionString())
            .thenReturn(connectionString);
        when(blobClient.exists()).thenReturn(true);


        Assertions.assertThatThrownBy(() -> dataManagementService.getChecksum(containerName, blobId))
            .isInstanceOf(DartsApiException.class)
            .hasMessage("Resource not found. Blob '431318c8-97db-415c-b321-120c48f0ffe2' does exist in container 'container123'" +
                            " but does not contain a checksum.")
            .hasFieldOrPropertyWithValue("error", CommonApiError.NOT_FOUND);

        verify(dataManagementConfiguration, times(1)).getBlobStorageAccountConnectionString();
        verify(dataManagementFactory, times(1)).getBlobServiceClient(connectionString);
        verify(dataManagementFactory, times(1)).getBlobContainerClient(containerName, blobServiceClient);
        verify(dataManagementFactory, times(1)).getBlobClient(blobContainerClient, blobId);
        verify(blobClient, times(2)).exists();
        verify(blobClient, times(1)).getProperties();
        verify(blobProperties, times(1)).getContentMd5();
        verify(blobClient, times(2)).exists();
        verifyNoInteractions(fileContentChecksum);
    }

    @Test
    void positiveGetChecksumAzureChecksumNotFoundMetaDataChecksumFound() {
        final String blobId = "431318c8-97db-415c-b321-120c48f0ffe2";
        final String containerName = "container123";
        final String connectionString = "connectionString";

        BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobProperties blobProperties = mock(BlobProperties.class);
        when(blobProperties.getContentMd5()).thenReturn(null);
        Map<String, String> metaData = new HashMap<>();
        metaData.put("checksum", "abc123");
        when(blobProperties.getMetadata()).thenReturn(metaData);

        BlobClient blobClient = mock(BlobClient.class);
        when(blobClient.getProperties()).thenReturn(blobProperties);

        when(dataManagementFactory.getBlobServiceClient(any()))
            .thenReturn(blobServiceClient);
        when(dataManagementFactory.getBlobContainerClient(any(), any()))
            .thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any()))
            .thenReturn(blobClient);
        when(dataManagementConfiguration.getBlobStorageAccountConnectionString())
            .thenReturn(connectionString);
        when(blobClient.exists()).thenReturn(true);

        assertThat(dataManagementService.getChecksum(containerName, blobId))
            .isEqualTo("abc123");

        verify(dataManagementConfiguration, times(1)).getBlobStorageAccountConnectionString();
        verify(dataManagementFactory, times(1)).getBlobServiceClient(connectionString);
        verify(dataManagementFactory, times(1)).getBlobContainerClient(containerName, blobServiceClient);
        verify(dataManagementFactory, times(1)).getBlobClient(blobContainerClient, blobId);
        verify(blobClient, times(2)).exists();
        verify(blobClient, times(1)).getProperties();
        verify(blobProperties, times(1)).getMetadata();
        verify(blobProperties, times(1)).getContentMd5();
        verify(fileContentChecksum, never()).encodeToString(any());
    }


    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void negativeGetChecksumAzureChecksumNotFoundMetaDataChecksumInvalid(String checksum) {
        final String blobId = "431318c8-97db-415c-b321-120c48f0ffe2";
        final String containerName = "container123";
        final String connectionString = "connectionString";

        BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobProperties blobProperties = mock(BlobProperties.class);
        when(blobProperties.getContentMd5()).thenReturn(null);
        Map<String, String> metaData = new HashMap<>();
        metaData.put("checksum", checksum);
        when(blobProperties.getMetadata()).thenReturn(metaData);

        BlobClient blobClient = mock(BlobClient.class);
        when(blobClient.getProperties()).thenReturn(blobProperties);

        when(dataManagementFactory.getBlobServiceClient(any()))
            .thenReturn(blobServiceClient);
        when(dataManagementFactory.getBlobContainerClient(any(), any()))
            .thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any()))
            .thenReturn(blobClient);
        when(dataManagementConfiguration.getBlobStorageAccountConnectionString())
            .thenReturn(connectionString);
        when(blobClient.exists()).thenReturn(true);

        Assertions.assertThatThrownBy(() -> dataManagementService.getChecksum(containerName, blobId))
            .isInstanceOf(DartsApiException.class)
            .hasMessage("Resource not found. Blob '431318c8-97db-415c-b321-120c48f0ffe2' does exist in container 'container123'" +
                            " but does not contain a checksum.")
            .hasFieldOrPropertyWithValue("error", CommonApiError.NOT_FOUND);

        verify(dataManagementConfiguration, times(1)).getBlobStorageAccountConnectionString();
        verify(dataManagementFactory, times(1)).getBlobServiceClient(connectionString);
        verify(dataManagementFactory, times(1)).getBlobContainerClient(containerName, blobServiceClient);
        verify(dataManagementFactory, times(1)).getBlobClient(blobContainerClient, blobId);
        verify(blobClient, times(2)).exists();
        verify(blobClient, times(1)).getProperties();
        verify(blobProperties, times(1)).getMetadata();
        verify(blobProperties, times(1)).getContentMd5();
        verify(fileContentChecksum, never()).encodeToString(any());
    }

    private void assertGetChecksumNotFound(Boolean exists) {
        final String blobId = "431318c8-97db-415c-b321-120c48f0ffe2";
        final String containerName = "container123";
        final String connectionString = "connectionString";

        BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);
        when(dataManagementFactory.getBlobServiceClient(any()))
            .thenReturn(blobServiceClient);
        when(dataManagementFactory.getBlobContainerClient(any(), any()))
            .thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any()))
            .thenReturn(blobClient);
        when(dataManagementConfiguration.getBlobStorageAccountConnectionString())
            .thenReturn(connectionString);

        when(blobClient.exists()).thenReturn(exists);


        Assertions.assertThatThrownBy(() -> dataManagementService.getChecksum(containerName, blobId))
            .isInstanceOf(DartsApiException.class)
            .hasMessage("Resource not found. Blob '431318c8-97db-415c-b321-120c48f0ffe2' does not exist in container 'container123'.")
            .hasFieldOrPropertyWithValue("error", CommonApiError.NOT_FOUND);

        verify(dataManagementConfiguration, times(1)).getBlobStorageAccountConnectionString();
        verify(dataManagementFactory, times(1)).getBlobServiceClient(connectionString);
        verify(dataManagementFactory, times(1)).getBlobContainerClient(containerName, blobServiceClient);
        verify(dataManagementFactory, times(1)).getBlobClient(blobContainerClient, blobId);
        verify(blobClient, times(exists == null ? 1 : 2)).exists();
        verifyNoInteractions(fileContentChecksum);
    }
}