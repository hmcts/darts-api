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
class ArmServiceFunctionalTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";

    @Value("${darts.storage.arm.container-name}")
    private String armContainerName;

    @Autowired
    private ArmService armService;

    @Autowired
    private ArmTestUtil armTestUtil;

    private List<String> blobsToBeDeleted = new ArrayList<>();

    @Test
    void saveBlobData() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String filename = String.format("%s_functional_test", UUID.randomUUID().toString());
        ArmBlobInfo armBlobInfo = armService.saveBlobData(armContainerName, filename, data);
        assertNotNull(armBlobInfo);
        log.info("Blob name {}, Blob path {}", armBlobInfo.getBlobName(), armBlobInfo.getBlobPathAndName());
        blobsToBeDeleted.add(armBlobInfo.getBlobName());
    }

    @AfterEach
    void cleanupArmBlobData() throws AzureDeleteBlobException {
        for (String blobName : blobsToBeDeleted) {
            armTestUtil.deleteBlobData(armContainerName, blobName);
        }
        blobsToBeDeleted.clear();
    }
}
