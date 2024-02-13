package uk.gov.hmcts.darts.dets;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.FileBasedDownloadResponseMetaData;
import uk.gov.hmcts.darts.dets.service.impl.DetsApiServiceImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles({"dev", "h2db"})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@ExtendWith(MockitoExtension.class)
class DetsDataManagementServiceTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";

    private static final String TEST_BLOB_ID = "b0f23c62-8dd3-4e4e-ae6a-321ff6eb61d8";

    @Autowired
    private DetsApiServiceImpl dataManagementService;

    @Test
    void fetchBinaryDataFromBlobStorage() throws IOException {
        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        var uuid = dataManagementService.saveBlobData(data);

        File fileToDownloadTo = File.createTempFile("tmp", "download");
        try (DownloadResponseMetaData responseMetaData = new FileBasedDownloadResponseMetaData(fileToDownloadTo)) {
            dataManagementService.downloadData(
                    uuid,
                    responseMetaData
            );
            assertEquals(TEST_BINARY_STRING, new String(responseMetaData.getInputStream().readAllBytes()));
        }
    }
}