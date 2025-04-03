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
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.testutil.ArmTestUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@ActiveProfiles({"dev", "h2db"})
@ExtendWith(MockitoExtension.class)
@Slf4j
class ArmServiceFunctionalTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final String FUNCTIONAL_TEST = "functional_test";

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
        log.info("------------------  saveBlobData test");

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String filename = String.format("%s_functional_test", UUID.randomUUID());
        String actualResult = armService.saveBlobData(armContainerName, filename, data);
        armSubmissionBlobsToBeDeleted.add(actualResult);
        assertNotNull(actualResult);
        log.info("Blob filename {}", actualResult);
        cleanupArmBlobData();
    }

    @Test
    void listSubmissionBlobs() {
        log.info("------------------  listSubmissionBlobs test");
        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String filename = String.format("functional_test_%s", UUID.randomUUID());
        String blobPathAndName = armSubmissionDropZone + filename;

        String actualResult = armService.saveBlobData(armContainerName, data, blobPathAndName);

        log.info("Saved blob {} in {}", actualResult, blobPathAndName);
        armBlobsWithPathToBeDeleted.add(actualResult);
        assertNotNull(actualResult);

        List<String> submissionBlobs = armService.listSubmissionBlobs(armContainerName, FUNCTIONAL_TEST);
        assertFalse(submissionBlobs.isEmpty());

        cleanupArmBlobData();
    }

    @Test
    void listBlobsUsingBatch() {
        log.info("------------------  listBlobsUsingBatch test");

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        uploadBatchedSubmissionBlobs(data);

        Integer batchSize = 5;
        List<String> blobs = armService.listSubmissionBlobsUsingBatch(armContainerName, FUNCTIONAL_TEST, batchSize);
        for (String blobPathAndName : blobs) {
            log.info("Blob about to be deleted {}", blobPathAndName);
            try {
                armTestUtil.deleteBlobData(armContainerName, blobPathAndName);
            } catch (AzureDeleteBlobException e) {
                fail("Exception " + e);
            }
        }

        assertFalse(blobs.isEmpty());
    }

    @Test
    void listBlobsUsingMarker() {
        log.info("------------------  listBlobsUsingMarker test");

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        uploadBatchedSubmissionBlobs(data);
        Integer batchSize = 10;
        String continuationToken = null;
        List<String> allBlobs = new ArrayList<>();
        do {
            ContinuationTokenBlobs continuationTokenBlobs = armService.listSubmissionBlobsWithMarker(
                armContainerName, FUNCTIONAL_TEST, batchSize, continuationToken);
            continuationToken = continuationTokenBlobs.getContinuationToken();
            log.info("continuationToken: \n{}", continuationToken);
            log.info("Total blobs {}", continuationTokenBlobs.getBlobNamesAndPaths().size());
            allBlobs.addAll(continuationTokenBlobs.getBlobNamesAndPaths());
            for (String blobPathAndName : continuationTokenBlobs.getBlobNamesAndPaths()) {
                try {
                    armTestUtil.deleteBlobData(armContainerName, blobPathAndName);
                } catch (AzureDeleteBlobException e) {
                    fail("Exception " + e);
                }
            }
        } while (nonNull(continuationToken));

        assertFalse(allBlobs.isEmpty());
    }

    @Test
    void listBlobsUsingMarkerTestContinuationToken() {
        log.info("------------------  listBlobsUsingMarkerTestContinuationToken test");

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        uploadBatchedSubmissionBlobs(data);
        Integer batchSize = 10;
        String continuationToken = null;
        List<String> allBlobs = new ArrayList<>();

        int maxContinuationBatchSize = 3;
        for (int pageSize = 0; pageSize < batchSize; pageSize += maxContinuationBatchSize) {
            ContinuationTokenBlobs continuationTokenBlobs = armService.listSubmissionBlobsWithMarker(
                armContainerName, FUNCTIONAL_TEST, maxContinuationBatchSize, continuationToken);
            continuationToken = continuationTokenBlobs.getContinuationToken();
            log.info("continuationToken: \n{}", continuationToken);
            log.info("Total blobs {}", continuationTokenBlobs.getBlobNamesAndPaths().size());
            allBlobs.addAll(continuationTokenBlobs.getBlobNamesAndPaths());
        }

        for (String blobPathAndName : allBlobs) {
            try {
                armTestUtil.deleteBlobData(armContainerName, blobPathAndName);
            } catch (AzureDeleteBlobException e) {
                fail("Exception " + e);
            }
        }
        assertFalse(allBlobs.isEmpty());

    }

    private void uploadBatchedSubmissionBlobs(BinaryData data) {
        for (int counter = 0; counter < 11; counter++) {
            String filename = String.format("functional_test_%s", UUID.randomUUID());
            String blobPathAndName = armSubmissionDropZone + filename;

            String actualResult = armService.saveBlobData(armContainerName, data, blobPathAndName);

            log.info("{} Saved blob {}", counter, actualResult);
            armBlobsWithPathToBeDeleted.add(actualResult);
        }
    }

    @AfterEach
    void cleanupArmBlobData() {

        for (String blobName : armSubmissionBlobsToBeDeleted) {
            String blobPathAndName = armSubmissionDropZone + blobName;
            try {
                armTestUtil.deleteBlobData(armContainerName, blobPathAndName);
            } catch (AzureDeleteBlobException e) {
                fail("Exception " + e);
            }
        }
        armSubmissionBlobsToBeDeleted.clear();

        for (String blobPathAndName : armBlobsWithPathToBeDeleted) {
            try {
                armTestUtil.deleteBlobData(armContainerName, blobPathAndName);
            } catch (AzureDeleteBlobException e) {
                fail("Exception " + e);
            }
        }
        armBlobsWithPathToBeDeleted.clear();
    }
}
