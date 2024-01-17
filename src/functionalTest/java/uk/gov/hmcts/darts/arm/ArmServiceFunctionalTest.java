package uk.gov.hmcts.darts.arm;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobItem;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.testutil.ArmTestUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles({"dev", "h2db"})
@ExtendWith(MockitoExtension.class)
@Slf4j
class ArmServiceFunctionalTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";

    @Value("${darts.storage.arm.container-name}")
    private String armContainerName;

    @Value("${darts.storage.arm.folders.submission}")
    private String armSubmissionDropZone;

    @Value("${darts.storage.arm.folders.collected}")
    private String armCollectedDropZone;

    @Autowired
    private ArmService armService;

    @Autowired
    private ArmTestUtil armTestUtil;

    private final List<String> armSubmissionBlobsToBeDeleted = new ArrayList<>();

    private final List<String> armCollectedBlobsToBeDeleted = new ArrayList<>();


    @Test
    void saveBlobData() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String filename = String.format("%s_functional_test", UUID.randomUUID().toString());
        String actualResult = armService.saveBlobData(armContainerName, filename, data);
        armSubmissionBlobsToBeDeleted.add(actualResult);
        assertNotNull(actualResult);
        log.info("Blob filename {}", actualResult);

    }

    @Test
    void listSubmissionBlobs() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String filename = String.format("functional_test_%s", UUID.randomUUID().toString());
        String blobPathAndName = armSubmissionDropZone + filename;
        String actualResult = armService.saveBlobData(armContainerName, data, blobPathAndName);
        log.info("Saved blob {} in {}", actualResult, blobPathAndName);
        armSubmissionBlobsToBeDeleted.add(actualResult);
        assertNotNull(actualResult);
        log.info("listSubmissionBlobs - Blob filename {}", actualResult);

        Map<String, BlobItem> submissionBlobs = armService.listSubmissionBlobs(armContainerName, armSubmissionDropZone);
        assertFalse(submissionBlobs.isEmpty());

    }

    @AfterEach
    void cleanupArmBlobData() throws AzureDeleteBlobException {

        for (String blobName : armSubmissionBlobsToBeDeleted) {
            String blobPathAndName = armSubmissionDropZone + blobName;
            armTestUtil.deleteBlobData(armContainerName, blobPathAndName);
        }
        armSubmissionBlobsToBeDeleted.clear();

        for (String blobPathAndName : armCollectedBlobsToBeDeleted) {
            armTestUtil.deleteBlobData(armContainerName, blobPathAndName);
        }
        armCollectedBlobsToBeDeleted.clear();
    }
}
