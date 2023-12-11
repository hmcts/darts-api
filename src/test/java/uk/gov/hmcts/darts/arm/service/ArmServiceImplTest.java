package uk.gov.hmcts.darts.arm.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.arm.service.impl.ArmServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmServiceImplTest {

    private static final String ARM_BLOB_CONTAINER_NAME = "arm_dummy_container";
    private static final String BLOB_FILENAME = "test_filename";
    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    private static final String TEST_DROP_ZONE = "dummy/dropzone/";
    @Mock
    private ArmDataManagementDao armDataManagementDao;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @InjectMocks
    private ArmServiceImpl armService;
    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);
    }

    @Test
    void testSaveBlobData() {
        when(armDataManagementConfiguration.getArmSubmissionDropZone()).thenReturn(TEST_DROP_ZONE);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);
        String blobId = armService.saveBlobData(ARM_BLOB_CONTAINER_NAME, BLOB_FILENAME, BINARY_DATA);
        assertNotNull(blobId);
        assertEquals(TEST_DROP_ZONE + BLOB_FILENAME, blobId);
    }
}
