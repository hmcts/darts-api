package uk.gov.hmcts.darts.common.service.impl;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileOperationServiceImplTest {

    @Mock
    private AudioConfigurationProperties audioConfigurationProperties;

    @InjectMocks
    private FileOperationServiceImpl fileOperationService;

    @TempDir
    Path tempDir;

    private InputStream mediaFile;
    private String fileName;

    @BeforeEach
    void setUp() {
        mediaFile = new ByteArrayInputStream("this is a binary data file".getBytes());
        fileName = "testFile.txt";
    }

    @Test
    void createFile_shouldCreateFile() throws IOException {
        Path filePath = fileOperationService.createFile(fileName, tempDir.toString(), true);
        assertTrue(Files.exists(filePath));
        assertEquals(0L, Files.size(filePath));
    }

    @Test
    void saveFileToTempWorkspace_shouldSaveFile() throws IOException {
        when(audioConfigurationProperties.getTempBlobWorkspace()).thenReturn(tempDir.toString());
        Path filePath = fileOperationService.saveFileToTempWorkspace(mediaFile, fileName);
        assertTrue(Files.exists(filePath));
        assertNotEquals(0L, Files.size(filePath));
    }

    @Test
    void saveFileToTempWorkspace_withStorageConfig_shouldSaveFile() throws IOException {
        StorageConfiguration storageConfiguration = mock(StorageConfiguration.class);
        when(storageConfiguration.getTempBlobWorkspace()).thenReturn(tempDir.toString());
        Path filePath = fileOperationService.saveFileToTempWorkspace(mediaFile, fileName, storageConfiguration, true);
        assertTrue(Files.exists(filePath));
        assertNotEquals(0L, Files.size(filePath));
    }

    @Test
    void saveBinaryDataToSpecifiedWorkspace_shouldSaveFile() throws IOException {
        BinaryData binaryData = BinaryData.fromStream(mediaFile);
        Path filePath = fileOperationService.saveBinaryDataToSpecifiedWorkspace(binaryData, fileName, tempDir.toString(), true);
        assertTrue(Files.exists(filePath));
        assertNotEquals(0L, Files.size(filePath));
    }

    @Test
    void saveBinaryDataToSpecifiedWorkspace_shouldThrowIOException() {
        BinaryData binaryData = BinaryData.fromStream(mediaFile);
        assertThrows(IOException.class, () -> fileOperationService.saveBinaryDataToSpecifiedWorkspace(binaryData, fileName, "/invalid/path", true));
    }

    @Test
    void convertFileToBinaryData_shouldConvertFile() throws IOException {
        Path filePath = Files.createFile(tempDir.resolve(fileName));
        Files.write(filePath, "this is a binary data file".getBytes());
        BinaryData binaryData = fileOperationService.convertFileToBinaryData(filePath.toString());
        assertNotNull(binaryData);
    }
}