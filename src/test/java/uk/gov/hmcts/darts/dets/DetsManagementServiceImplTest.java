package uk.gov.hmcts.darts.dets;

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
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;
import uk.gov.hmcts.darts.dets.service.impl.DetsApiServiceImpl;

import java.io.OutputStream;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetsManagementServiceImplTest {

    public static final String BLOB_CONTAINER_NAME = "dummy_container";
    public static final UUID BLOB_ID = UUID.randomUUID();
    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    @Mock
    public Response<Void> responseMock;
    @Mock
    private DataManagementAzureClientFactory dataManagementFactory;
    @Mock
    private DetsDataManagementConfiguration dataManagementConfiguration;
    @InjectMocks
    private DetsApiServiceImpl dataManagementService;
    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    private BlobServiceClient serviceClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);
        serviceClient = mock(BlobServiceClient.class);

        String connectionString = "test connection string";
        when(dataManagementConfiguration.getConnectionString()).thenReturn(connectionString);
        when(dataManagementFactory.getBlobServiceClient(Mockito.eq(connectionString))).thenReturn(serviceClient);
        when(dataManagementConfiguration.getContainerName()).thenReturn(BLOB_CONTAINER_NAME);
    }

    @Test
    void testDownloadData() throws Exception {
        try (OutputStream stream = mock(OutputStream.class)) {
            when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
            when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);

            try (DownloadResponseMetaData downloadResponseMetaData = new DownloadResponseMetaData(stream)) {
                dataManagementService.downloadData(BLOB_ID, downloadResponseMetaData);

                Assertions.assertTrue(downloadResponseMetaData.isSuccessfulDownload());
                Assertions.assertEquals(DatastoreContainerType.DETS, downloadResponseMetaData.getContainerTypeUsedToDownload());
                verify(blobClient, times(1)).downloadStream(any());
            }
        }
    }
}