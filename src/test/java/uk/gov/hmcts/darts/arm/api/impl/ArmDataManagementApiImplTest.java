package uk.gov.hmcts.darts.arm.api.impl;

import com.azure.core.util.BinaryData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArmApiService;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.dets.api.impl.DetsDataManagementApiImpl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.CloseResource")
@Slf4j
class ArmDataManagementApiImplTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";

    private static final String ARM_BLOB_CONTAINER_NAME = "arm_dummy_container";
    private static final String UNSTRUCTURED_CONTAINER_NAME = "darts-unstructured";
    private static final String ARM_DROP_ZONE = "arm_drop_zone/";
    private static final String EXTERNAL_RECORD_ID = "4bfe4fc7-4e2f-4086-8a0e-146cc4556260";
    private static final String EXTERNAL_FILE_ID = "075987ea-b34d-49c7-b8db-439bfbe2496c";

    private ArmDataManagementApiImpl armDataManagementApi;

    @Mock
    private ArmService armService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    @Mock
    private ArmApiService armApiService;
    @Mock
    private DataManagementService dataManagementService;
    @Mock
    private DetsDataManagementApiImpl detsDataManagementApi;

    @BeforeEach
    void setUp() {
        lenient().when(armDataManagementConfiguration.getContainerName()).thenReturn(ARM_BLOB_CONTAINER_NAME);
        armDataManagementApi = new ArmDataManagementApiImpl(
            armService, armDataManagementConfiguration, armApiService, dataManagementConfiguration, dataManagementService, detsDataManagementApi);
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
    void downloadArmData() throws FileNotDownloadedException {

        DownloadResponseMetaData metaData = Mockito.mock(DownloadResponseMetaData.class);
        when(armApiService.downloadArmData(EXTERNAL_RECORD_ID, EXTERNAL_FILE_ID)).thenReturn(metaData);

        ExternalObjectDirectoryEntity entity = new ExternalObjectDirectoryEntity();
        entity.setExternalFileId(EXTERNAL_FILE_ID);
        entity.setExternalRecordId(EXTERNAL_RECORD_ID);

        DownloadResponseMetaData response = armDataManagementApi.downloadBlobFromContainer(DatastoreContainerType.ARM, entity);

        assertNotNull(response);
    }

    @Test
    void copyBlobDataToArm() {

        UUID unstructuredUuid = UUID.randomUUID();
        String filename = "someFile";
        when(dataManagementConfiguration.getUnstructuredContainerName()).thenReturn(UNSTRUCTURED_CONTAINER_NAME);
        when(armDataManagementConfiguration.getFolders().getSubmission()).thenReturn("DARTS/submission/");

        armDataManagementApi.copyBlobDataToArm(unstructuredUuid.toString(), filename);

        verify(dataManagementService).copyBlobData(
            UNSTRUCTURED_CONTAINER_NAME, ARM_BLOB_CONTAINER_NAME, unstructuredUuid.toString(), "DARTS/submission/" + filename);
    }

    @Test
    void copyDetsBlobDataToArm() {

        UUID detsUuid = UUID.randomUUID();
        String filename = "someFile";
        when(armDataManagementConfiguration.getFolders().getSubmission()).thenReturn("DARTS/submission/");

        armDataManagementApi.copyDetsBlobDataToArm(detsUuid.toString(), filename);

        verify(detsDataManagementApi).copyDetsBlobDataToArm(detsUuid.toString(), "DARTS/submission/" + filename);
    }
}