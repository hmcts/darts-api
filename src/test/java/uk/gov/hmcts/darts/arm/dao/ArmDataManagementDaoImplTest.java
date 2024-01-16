package uk.gov.hmcts.darts.arm.dao;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.impl.ArmDataManagementDaoImpl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmDataManagementDaoImplTest {

    @InjectMocks
    private ArmDataManagementDaoImpl armDataManagementDao;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    private static final String BLOB_FILENAME = "12_45_1";
    public static final String BLOB_CONTAINER_NAME = "arm_dummy_container";
    private static final String SAS_ENDPOINT = "https://dartssasdummy.blob.core.windows.net/darts-arm?sp=r&st=2011-11-11T11:11:11Z&" +
        "se=2011-11-11T11:11:21Z&spr=https&sv=2022-11-02&sr=c&sig=AAAAAAA";

    private static final String ARM_DROP_ZONE = "dummy/dropzone/submission/";
    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = Mockito.mock(BlobContainerClient.class);
        blobClient = Mockito.mock(BlobClient.class);
    }

    @Test
    void testGetBlobContainerClient() {
        when(armDataManagementConfiguration.getSasEndpoint()).thenReturn(SAS_ENDPOINT);
        BlobContainerClient blobContainerClient = armDataManagementDao.getBlobContainerClient(BLOB_CONTAINER_NAME);
        assertNotNull(blobContainerClient);
    }

    @Test
    void testGetBlobClient() {
        String blobId = ARM_DROP_ZONE + BLOB_FILENAME;

        when(blobContainerClient.getBlobClient(blobId)).thenReturn(blobClient);
        BlobClient blobClient = armDataManagementDao.getBlobClient(blobContainerClient, blobId);
        assertNotNull(blobClient);
    }
}
