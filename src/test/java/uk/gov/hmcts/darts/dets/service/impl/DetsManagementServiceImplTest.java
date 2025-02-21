package uk.gov.hmcts.darts.dets.service.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.datamanagement.component.DataManagementAzureClientFactory;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.dets.config.DetsDataManagementConfiguration;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetsManagementServiceImplTest {

    public static final String BLOB_CONTAINER_NAME = "dummy_container";
    public static final String BLOB_ID = UUID.randomUUID().toString();
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

        String sasEndpoint = "test sas url endpoint";
        when(dataManagementConfiguration.getSasEndpoint()).thenReturn(sasEndpoint);
        when(dataManagementFactory.getBlobServiceClientWithSasEndpoint(sasEndpoint)).thenReturn(serviceClient);
        when(dataManagementConfiguration.getContainerName()).thenReturn(BLOB_CONTAINER_NAME);
    }

    @Test
    void testDownloadData() throws Exception {
        when(dataManagementFactory.getBlobContainerClient(BLOB_CONTAINER_NAME, serviceClient)).thenReturn(blobContainerClient);
        when(dataManagementFactory.getBlobClient(any(), any())).thenReturn(blobClient);
        when(dataManagementConfiguration.getTempBlobWorkspace()).thenReturn("tempWorkspace");

        try (DownloadResponseMetaData downloadResponseMetaData = dataManagementService.downloadData(BLOB_ID)) {
            assertEquals(DatastoreContainerType.DETS, downloadResponseMetaData.getContainerTypeUsedToDownload());
            verify(blobClient, times(1)).downloadStream(any());
        }
    }
}