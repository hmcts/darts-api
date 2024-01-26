package uk.gov.hmcts.darts.arm.api.impl;

import com.azure.core.util.BinaryData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArmService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
class ArmDataManagementApiImplTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";

    private static final String ARM_BLOB_CONTAINER_NAME = "arm_dummy_container";

    private static final String ARM_DROP_ZONE = "arm_drop_zone/";

    private ArmDataManagementApiImpl armDataManagementApi;

    @Mock
    private ArmService armService;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @BeforeEach
    void setUp() {
        when(armDataManagementConfiguration.getContainerName()).thenReturn(ARM_BLOB_CONTAINER_NAME);
        armDataManagementApi = new ArmDataManagementApiImpl(armService, armDataManagementConfiguration);
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

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(ARM_DROP_ZONE);
        foldersConfig.setCollected(ARM_DROP_ZONE);
        foldersConfig.setResponse(ARM_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String blobname = "1_1_1";

        String blobNameAndPath = ARM_DROP_ZONE + blobname;
        when(armService.getBlobData(ARM_BLOB_CONTAINER_NAME, blobNameAndPath)).thenReturn(data);

        BinaryData binaryData = armDataManagementApi.getResponseBlobData(blobname);
        assertEquals(data, binaryData);
    }
}
