package uk.gov.hmcts.darts.audio.service;

import com.azure.core.util.BinaryData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.test.common.FileStore;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class FileOperationServiceTest extends IntegrationBase {

    @Autowired
    private FileOperationService fileOperationService;

    @TempDir
    private File tempDirectory;
    private InputStream mediaFile;
    private String fileName;

    @BeforeEach
    void setUp() {

        String data = "this is a binary data file";
        mediaFile = new ByteArrayInputStream(data.getBytes());
        fileName = "caseAudioFile.pdf";
    }

    @Test
    @DisplayName("Test-1: Check if file is created in temporary folder")
    void saveBlobDataToTempWorkspaceTestOne() throws IOException {
        Path filePath = fileOperationService.saveFileToTempWorkspace(mediaFile, fileName);
        assertTrue(Files.exists(filePath));
    }

    @Test
    @DisplayName("Test-2: Check if file is empty")
    void saveBlobDataToTempWorkspaceTestTwo() throws IOException {
        Path filePath = fileOperationService.saveFileToTempWorkspace(mediaFile, fileName);
        assertNotEquals(0L, Files.size(filePath));
    }

    @Test
    @DisplayName("Test-3: Check if the saved file is equal to the original BinaryData file")
    void saveBlobDataToTempWorkspaceTestThree() throws IOException {
        mediaFile.mark(0);
        Path filePath = fileOperationService.saveFileToTempWorkspace(mediaFile, fileName);
        mediaFile.reset();
        assertArrayEquals(mediaFile.readAllBytes(), Files.readAllBytes(filePath));
    }

    @Test
    @DisplayName("Test-4: Test for exception thrown")
    void saveBlobDataToTempWorkspaceTestFive() {
        String invalidFileName = "inlaid/<:?*|>/file";

        assertThrows(IOException.class, () -> fileOperationService.saveFileToTempWorkspace(mediaFile, invalidFileName));
    }

    @Test
    @DisplayName("Test-5: Test converting file to binary data")
    void convertFileToBinaryData() throws IOException {
        Path filePath = createDummyFile();
        log.debug("Created file {}", filePath);
        BinaryData binaryData = fileOperationService.convertFileToBinaryData(filePath.toFile().getAbsolutePath());
        assertNotNull(binaryData);
    }

    @Test
    @DisplayName("Test-6: Test save binary data to specified workspace")
    void saveBinaryDataToSpecifiedWorkspace() throws IOException {
        var tempFilename = UUID.randomUUID().toString();

        Path filePath = fileOperationService.saveBinaryDataToSpecifiedWorkspace(
            BinaryData.fromStream(mediaFile),
            tempFilename,
            tempDirectory.getAbsolutePath(),
            true
        );
        assertNotNull(filePath);
    }

    @Test
    @DisplayName("Test-7: createFile")
    void createFile() throws IOException {
        Path filePath = fileOperationService.createFile(fileName, tempDirectory.getAbsolutePath(), true);
        assertTrue(Files.exists(filePath));
        assertEquals(0L, Files.size(filePath));
    }

    private Path createDummyFile() throws IOException {
        var tempFilename = UUID.randomUUID().toString();
        var tempDirectoryName = UUID.randomUUID().toString();
        Path tempDirectory = Files.createTempDirectory(tempDirectoryName);

        return FileStore.getFileStore().create(tempDirectory, Path.of(tempFilename)).toPath();
    }
}