package uk.gov.hmcts.darts.datamanagement;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.FunctionalTest;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponse;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles({"dev", "h2db", "functionalTest"})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@ExtendWith(MockitoExtension.class)
class DataManagementServiceFunctionalTest extends FunctionalTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";

    private static final String TEST_BLOB_ID = "b0f23c62-8dd3-4e4e-ae6a-321ff6eb61d8";

    @Value("${darts.storage.blob.container-name.unstructured}")
    String unstructuredStorageContainerName;

    @Autowired
    DataManagementService dataManagementService;
    @Autowired
    DataManagementConfiguration dataManagementConfiguration;
    @Autowired
    ArmDataManagementConfiguration armDataManagementConfiguration;

    @Test
    void saveBinaryDataToBlobStorage() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        var uniqueBlobName = dataManagementService.saveBlobData(unstructuredStorageContainerName, data);

        assertTrue(uniqueBlobName instanceof String);
    }

    @Test
    void fetchBinaryDataFromBlobStorage() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        var uniqueBlobName = dataManagementService.saveBlobData(unstructuredStorageContainerName, data);

        var blobData = dataManagementService.getBlobData(
            unstructuredStorageContainerName,
            uniqueBlobName
        );

        assertEquals(TEST_BINARY_STRING, blobData.toString());
    }

    @Test
    void whenGetShouldThrowExceptionWhenProvidedWithInvalidContainerName() {
        assertThrows(BlobStorageException.class, () ->
            dataManagementService.getBlobData(
                "INVALID_CONTAINER_NAME",
                TEST_BLOB_ID
            ));
    }

    @Test
    void saveBinaryDataWithMetadataToBlobStorage() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        Map<String, String> metaDataMap = new HashMap<>();
        metaDataMap.put("TestKey", "TestValue");
        BlobClient blobClient = dataManagementService.saveBlobData(unstructuredStorageContainerName, data, metaDataMap);

        assertTrue(blobClient.getProperties().getMetadata().containsKey("TestKey"));
    }

    @Test
    void fetchDownloadBinaryDataFromBlobStorage() throws IOException, FileNotDownloadedException {
        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        var uniqueBlobName = dataManagementService.saveBlobData(unstructuredStorageContainerName, data);

        try (DownloadResponseMetaData downloadResponseMetaData = dataManagementService.downloadData(DatastoreContainerType.UNSTRUCTURED,
                                                                                                    unstructuredStorageContainerName,
                                                                                                    uniqueBlobName)) {
            assertEquals(TEST_BINARY_STRING, new String(downloadResponseMetaData.getResource().getInputStream().readAllBytes()));
        }
    }

    @Test
    void saveBlobDataShouldSucceedAndReturnUuid() {
        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(testStringInBytes);

        BlobClientUploadResponse blobClientUploadResponse = dataManagementService.saveBlobData(unstructuredStorageContainerName, byteArrayInputStream);

        assertNotNull(blobClientUploadResponse.getBlobName());
        assertNotNull(blobClientUploadResponse.getBlobSize());
    }

    @Disabled // Disabled as this test is currently giving a false negative
    @Test
    void copyBetweenStorageContainers() throws AzureDeleteBlobException {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        var sourceUuid = dataManagementService.saveBlobData(dataManagementConfiguration.getInboundContainerName(), data);

        String destinationLocation = UUID.randomUUID().toString();
        dataManagementService.copyBlobData(
            dataManagementConfiguration.getInboundContainerName(),
            dataManagementConfiguration.getUnstructuredContainerName(),
            sourceUuid, destinationLocation);

        var blobData = dataManagementService.getBlobData(
            dataManagementConfiguration.getUnstructuredContainerName(),
            destinationLocation
        );

        dataManagementService.deleteBlobData(dataManagementConfiguration.getInboundContainerName(), sourceUuid);
        dataManagementService.deleteBlobData(dataManagementConfiguration.getUnstructuredContainerName(), destinationLocation);

        assertEquals(TEST_BINARY_STRING, blobData.toString());
    }

}