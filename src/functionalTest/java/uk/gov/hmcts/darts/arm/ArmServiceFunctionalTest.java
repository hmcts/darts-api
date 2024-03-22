package uk.gov.hmcts.darts.arm;

import com.azure.core.util.BinaryData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.testutil.ArmTestUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Autowired
    private ArmService armService;

    @Autowired
    private ArmTestUtil armTestUtil;

    private final List<String> armSubmissionBlobsToBeDeleted = new ArrayList<>();

    private final List<String> armBlobsWithPathToBeDeleted = new ArrayList<>();


    @Test
    void saveBlobData() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String filename = String.format("%s_functional_test", UUID.randomUUID());
        String actualResult = armService.saveBlobData(armContainerName, filename, data);
        armSubmissionBlobsToBeDeleted.add(actualResult);
        assertNotNull(actualResult);
        log.info("Blob filename {}", actualResult);

    }

    @Test
    void listSubmissionBlobs() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String filename = String.format("functional_test_%s", UUID.randomUUID());
        String blobPathAndName = armSubmissionDropZone + filename;

        String actualResult = armService.saveBlobData(armContainerName, data, blobPathAndName);

        log.info("Saved blob {} in {}", actualResult, blobPathAndName);
        armBlobsWithPathToBeDeleted.add(actualResult);
        assertNotNull(actualResult);

        List<String> submissionBlobs = armService.listSubmissionBlobs(armContainerName, "functional_test");
        assertFalse(submissionBlobs.isEmpty());

    }

    @Test
    void listBlobsUsingBatch() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        uploadBatchedSubmissionBlobs(data);

        Integer batchSize = 5;
        List<String> blobs = armService.listSubmissionBlobsUsingBatch(armContainerName, "functional_test", batchSize);

        assertEquals(batchSize, blobs.size());
    }

    @Test
    void listBlobsUsingMarker() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        uploadBatchedSubmissionBlobs(data);
        Integer batchSize = 2;
        String continuationToken = null;
        List<String> allBlobs = new ArrayList<>();
        do {
            ContinuationTokenBlobs continuationTokenBlobs = armService.listSubmissionBlobsWithMarker(
                armContainerName, "functional_test", batchSize, continuationToken);
            continuationToken = continuationTokenBlobs.getContinuationToken();
            log.info("continuationToken: \n{}", continuationToken);
            log.info("Total blobs {}", continuationTokenBlobs.getBlobNamesWithAndPaths().size());
            allBlobs.addAll(continuationTokenBlobs.getBlobNamesWithAndPaths());
        } while (nonNull(continuationToken));

        assertEquals(11, allBlobs.size());
    }

    private void uploadBatchedSubmissionBlobs(BinaryData data) {
        for (int counter = 0; counter < 11; counter++) {
            String filename = String.format("functional_test_%s", UUID.randomUUID());
            String blobPathAndName = armSubmissionDropZone + filename;

            String actualResult = armService.saveBlobData(armContainerName, data, blobPathAndName);

            log.info("Saved blob {} in {}", actualResult, blobPathAndName);
            armBlobsWithPathToBeDeleted.add(actualResult);
        }
    }

    @AfterEach
    void cleanupArmBlobData() {

        for (String blobName : armSubmissionBlobsToBeDeleted) {
            String blobPathAndName = armSubmissionDropZone + blobName;
            armTestUtil.deleteBlobData(armContainerName, blobPathAndName);
        }
        armSubmissionBlobsToBeDeleted.clear();

        for (String blobPathAndName : armBlobsWithPathToBeDeleted) {
            armTestUtil.deleteBlobData(armContainerName, blobPathAndName);
        }
        armBlobsWithPathToBeDeleted.clear();
    }
}
