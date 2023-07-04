package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
class AudioFileOperationServiceTest {

    @Autowired
    private AudioTransformationService audioTransformationService;

    private Path filePath;
    private BinaryData mediaFile;
    private String fileName;

    @BeforeEach
    void setUp() {

        String data = "this is a binary data file";
        mediaFile = BinaryData.fromString(data);
        fileName = "caseAudioFile.pdf";
    }

    @Test
    @DisplayName("Test-1: Check if file is created in temporary folder")
    void saveBlobDataToTempWorkspaceTestOne() throws IOException {

        filePath = audioTransformationService.saveBlobDataToTempWorkspace(mediaFile, fileName);
        assertTrue(Files.exists(filePath));
    }

    @Test
    @DisplayName("Test-2: Check if file is empty")
    void saveBlobDataToTempWorkspaceTestTwo() throws IOException {

        filePath = audioTransformationService.saveBlobDataToTempWorkspace(mediaFile, fileName);
        assertNotEquals(0L, Files.size(filePath));
    }

    @Test
    @DisplayName("Test-3: Check if the saved file is equal to the original BinaryData file")
    void saveBlobDataToTempWorkspaceTestThree() throws IOException {

        filePath = audioTransformationService.saveBlobDataToTempWorkspace(mediaFile, fileName);
        assertArrayEquals(mediaFile.toBytes(), Files.readAllBytes(filePath));
    }

    @AfterEach
    void deleteFile() throws IOException, InterruptedException {
        if (filePath != null) {
            Thread.sleep(1000);     //remove this line before pull request
            Files.delete(filePath);
        }
    }
}
