package uk.gov.hmcts.darts.dets;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.dets.service.impl.DetsApiServiceImpl;
import uk.gov.hmcts.darts.testutil.ArmTestUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

@SpringBootTest
@ActiveProfiles({"dev", "h2db"})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@ExtendWith(MockitoExtension.class)
class DetsDataManagementServiceTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";

    private final List<String> armSubmissionBlobsToBeDeleted = new ArrayList<>();

    private final List<String> armBlobsWithPathToBeDeleted = new ArrayList<>();

    @Autowired
    private DetsApiServiceImpl dataManagementService;

    @Autowired
    private ArmTestUtil armTestUtil;

    @Value("${darts.storage.arm.container-name}")
    private String armContainerName;
    @Value("${darts.storage.arm.folders.submission}")
    private String armSubmissionDropZone;


    @Test
    void fetchBinaryDataFromBlobStorage() throws IOException, FileNotDownloadedException {
        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        var uuid = dataManagementService.saveBlobData(data);

        try (DownloadResponseMetaData downloadResponseMetaData = dataManagementService.downloadData(uuid)) {
            assertEquals(TEST_BINARY_STRING, new String(downloadResponseMetaData.getResource().getInputStream().readAllBytes()));
        }
    }


    @Test
    void copyDetsBlobDataToArm() throws AzureDeleteBlobException {
        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        var uuid = dataManagementService.saveBlobData(data);

        String filename = String.format("functional_test_%s", UUID.randomUUID());
        String blobPathAndName = armSubmissionDropZone + filename;

        dataManagementService.copyDetsBlobDataToArm(uuid.toString(), blobPathAndName);

        boolean deleted = dataManagementService.deleteBlobDataFromContainer(uuid);

        armTestUtil.deleteBlobData(armContainerName, blobPathAndName);
        assertTrue("Failed to delete DETS blob " + uuid, deleted);
        
    }


}