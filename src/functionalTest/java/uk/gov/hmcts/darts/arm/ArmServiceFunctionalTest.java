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
import uk.gov.hmcts.darts.arm.model.ArmBlobInfo;
import uk.gov.hmcts.darts.arm.service.ArmService;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.testutil.ArmTestUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles({"dev", "h2db"})
@ExtendWith(MockitoExtension.class)
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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

    private final List<String> blobsToBeDeleted = new ArrayList<>();

    @Test
    @Order(1)
    void saveBlobData() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String filename = String.format("%s_functional_test", UUID.randomUUID().toString());
        String actualResult = armService.saveBlobData(armContainerName, filename, data);
        assertNotNull(actualResult);
        log.info("Blob filename {}", actualResult);
        blobsToBeDeleted.add(actualResult);
    }

    @AfterEach
    void cleanupArmBlobData() throws AzureDeleteBlobException {

        for (String blobName : blobsToBeDeleted) {
            String blobPathAndName = armSubmissionDropZone + blobName;
            armTestUtil.deleteBlobData(armContainerName, blobPathAndName);
        }
        blobsToBeDeleted.clear();
    }
}
