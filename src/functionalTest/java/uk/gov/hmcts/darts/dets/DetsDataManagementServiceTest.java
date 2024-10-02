package uk.gov.hmcts.darts.dets;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.dets.service.impl.DetsApiServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles({"dev", "h2db"})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@ExtendWith(MockitoExtension.class)
class DetsDataManagementServiceTest {

    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";

    @Autowired
    private DetsApiServiceImpl dataManagementService;

    @Test
    void fetchBinaryDataFromBlobStorage() throws IOException, FileNotDownloadedException {
        byte[] testStringInBytes = TEST_BINARY_STRING.getBytes(StandardCharsets.UTF_8);
        BinaryData data = BinaryData.fromBytes(testStringInBytes);

        var uuid = dataManagementService.saveBlobData(data);

        try (DownloadResponseMetaData downloadResponseMetaData = dataManagementService.downloadData(
            uuid
        )) {
            assertEquals(TEST_BINARY_STRING, new String(downloadResponseMetaData.getResource().getInputStream().readAllBytes()));
        }
    }

    
}