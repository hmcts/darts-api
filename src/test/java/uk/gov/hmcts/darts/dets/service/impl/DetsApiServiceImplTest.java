package uk.gov.hmcts.darts.dets.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.util.AzureCopyUtil;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetsApiServiceImplTest {

    private static final String DETS_UUID = "dets-uuid";
    private static final String BLOB_PATH_AND_NAME = "folder/blob.mp3";
    private static final String DETS_CONTAINER = "dets-container";
    private static final String ARM_CONTAINER = "arm-container";
    private static final String DETS_SAS_URL = "https://dets.blob.core.windows.net/dets-container";
    private static final String ARM_SAS_URL = "https://arm.blob.core.windows.net/arm-container";

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    private static final String BLOB_CONTAINER_NAME = "dummy_container";
    private static final String BLOB_ID = UUID.randomUUID().toString();
    public static final String TEST_SAS_URL_ENDPOINT = "test sas url endpoint";

    @Mock
    private DataManagementAzureClientFactory dataManagementFactory;
    @Mock
    private DetsDataManagementConfiguration detsDataManagementConfiguration;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private AzureCopyUtil azureCopyUtil;

    @InjectMocks
    private DetsApiServiceImpl detsApiService;

    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    private BlobServiceClient serviceClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);
        serviceClient = mock(BlobServiceClient.class);

        when(detsDataManagementConfiguration.getSasEndpoint()).thenReturn(TEST_SAS_URL_ENDPOINT);
        when(detsDataManagementConfiguration.getContainerName()).thenReturn(BLOB_CONTAINER_NAME);
    }

    @Test
    void downloadData_ShouldDownloadBlob() throws Exception {
        when(dataManagementFactory.getBlobServiceClientWithSasEndpoint(TEST_SAS_URL_ENDPOINT)).thenReturn(serviceClient);
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);
        when(detsDataManagementConfiguration.getTempBlobWorkspace()).thenReturn("tempWorkspace");

        try (DownloadResponseMetaData downloadResponseMetaData = detsApiService.downloadData(BLOB_ID)) {
            assertEquals(DatastoreContainerType.DETS, downloadResponseMetaData.getContainerTypeUsedToDownload());
            verify(blobClient, times(1)).downloadStream(any());
        }
    }

    @Test
    void saveBlobData_ShouldSaveBlobData_UsingFilename() {
        when(dataManagementFactory.getBlobServiceClientWithSasEndpoint(TEST_SAS_URL_ENDPOINT)).thenReturn(serviceClient);
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);

        String filename = "tempfile.mp2";
        detsApiService.saveBlobData(BINARY_DATA, filename);

        verify(blobClient, times(1)).upload(BINARY_DATA);

    }

    @Test
    void saveBlobData_ShouldSaveBlobData_UsingUuid() {
        when(dataManagementFactory.getBlobServiceClientWithSasEndpoint(TEST_SAS_URL_ENDPOINT)).thenReturn(serviceClient);
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);

        detsApiService.saveBlobData(BINARY_DATA);

        verify(blobClient, times(1)).upload(BINARY_DATA);

    }

    @Test
    void copyDetsBlobDataToArm_shouldCallAzureCopyUtilWithCorrectArguments() {
        when(detsDataManagementConfiguration.getSasEndpoint()).thenReturn(DETS_SAS_URL);
        when(armDataManagementConfiguration.getSasEndpoint()).thenReturn(ARM_SAS_URL);
        when(detsDataManagementConfiguration.getContainerName()).thenReturn(DETS_CONTAINER);
        when(armDataManagementConfiguration.getContainerName()).thenReturn(ARM_CONTAINER);

        detsApiService.copyDetsBlobDataToArm(DETS_UUID, BLOB_PATH_AND_NAME);

        String expectedSourceUrl = DETS_SAS_URL.replace(DETS_CONTAINER,
                                                        DETS_CONTAINER + "/" + java.net.URLEncoder.encode(DETS_UUID, java.nio.charset.StandardCharsets.UTF_8));
        String expectedDestUrl = ARM_SAS_URL.replace(ARM_CONTAINER, ARM_CONTAINER + "/" + BLOB_PATH_AND_NAME);

        verify(azureCopyUtil, times(1)).copy(eq(expectedSourceUrl), eq(expectedDestUrl));
    }

    @Test
    void copyDetsBlobDataToArm_shouldThrowDartsExceptionWhenCopyFails() {
        when(detsDataManagementConfiguration.getSasEndpoint()).thenReturn(DETS_SAS_URL);
        when(armDataManagementConfiguration.getSasEndpoint()).thenReturn(ARM_SAS_URL);
        when(detsDataManagementConfiguration.getContainerName()).thenReturn(DETS_CONTAINER);
        when(armDataManagementConfiguration.getContainerName()).thenReturn(ARM_CONTAINER);

        doThrow(new RuntimeException("copy failed")).when(azureCopyUtil).copy(anyString(), anyString());

        assertThrows(DartsException.class, () ->
            detsApiService.copyDetsBlobDataToArm(DETS_UUID, BLOB_PATH_AND_NAME)
        );
    }
}