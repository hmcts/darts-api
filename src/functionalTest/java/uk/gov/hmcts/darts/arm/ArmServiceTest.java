package uk.gov.hmcts.darts.arm;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.arm.service.ArmService;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles({"dev", "h2db"})
@ExtendWith(MockitoExtension.class)
class ArmServiceTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";

    private static final String TEST_BLOB_ID = "b0f23c62-8dd3-4e4e-ae6a-321ff6eb61d8";

    @Value("${darts.storage.arm.container-name}")
    private String armContainerName;

    @Autowired
    ArmService armService;


    @Test
    void saveBlobData() {

        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);
        String filename = "1_1_1";
        var uniqueBlobName = armService.saveBlobData(armContainerName, filename, data);

        assertNotNull(uniqueBlobName);
    }
}
