package uk.gov.hmcts.darts.arm.api.impl;

import com.azure.core.util.BinaryData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.CloseResource")
@Slf4j
class ArmDataManagementApiImplTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";

    private static final String ARM_BLOB_CONTAINER_NAME = "arm_dummy_container";

    private static final String ARM_DROP_ZONE = "arm_drop_zone/";
    private static final String EXTERNAL_RECORD_ID = "4bfe4fc7-4e2f-4086-8a0e-146cc4556260";
    private static final String EXTERNAL_FILE_ID = "075987ea-b34d-49c7-b8db-439bfbe2496c";

    private ArmDataManagementApiImpl armDataManagementApi;

    @Mock
    private ArmService armService;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private ArmApiService armApiService;

    @BeforeEach
    void setUp() {
        lenient().when(armDataManagementConfiguration.getContainerName()).thenReturn(ARM_BLOB_CONTAINER_NAME);
        armDataManagementApi = new ArmDataManagementApiImpl(armService, armDataManagementConfiguration, armApiService);
    }

    @Test
    void saveBlobDataToArm() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String filename = "1_1_1";

        when(armService.saveBlobData(ARM_BLOB_CONTAINER_NAME, filename, data)).thenReturn(filename);

        String actualResult = armDataManagementApi.saveBlobDataToArm(filename, data);
        assertEquals(filename, actualResult);
    }

    @Test
    void listCollectedBlobs() {

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);

        when(armService.listCollectedBlobs(ARM_BLOB_CONTAINER_NAME, prefix)).thenReturn(responseBlobs);

        List<String> blobs = armDataManagementApi.listCollectedBlobs(prefix);

        assertEquals(1, blobs.size());
    }

    @Test
    void listResponseBlobs() {
        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);

        when(armService.listResponseBlobs(ARM_BLOB_CONTAINER_NAME, prefix)).thenReturn(responseBlobs);

        List<String> blobs = armDataManagementApi.listResponseBlobs(prefix);
        assertEquals(1, blobs.size());
    }

    @Test
    void getResponseBlobData() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String blobname = "1_1_1";

        String blobNameAndPath = ARM_DROP_ZONE + blobname;
        when(armService.getBlobData(ARM_BLOB_CONTAINER_NAME, blobNameAndPath)).thenReturn(data);

        BinaryData binaryData = armDataManagementApi.getBlobData(blobNameAndPath);
        assertEquals(data, binaryData);
    }

    @Test
    void downloadArmData() throws IOException {

        var inputStream = new ByteArrayInputStream("some file binary content".getBytes());
        DownloadResponseMetaData metaData = new DownloadResponseMetaData(null);
        when(armApiService.downloadArmData(EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID, metaData)).thenReturn(inputStream);

        ExternalObjectDirectoryEntity entity = new ExternalObjectDirectoryEntity();
        entity.setExternalFileId(EXTERNAL_FILE_ID);
        entity.setExternalRecordId(EXTERNAL_RECORD_ID);

        boolean result = armDataManagementApi.downloadBlobFromContainer(DatastoreContainerType.ARM, entity, metaData);

        assertTrue(result);
    }
}