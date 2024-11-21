package uk.gov.hmcts.darts.common.service.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.common.service.FileOperationService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileOperationServiceImpl implements FileOperationService {

    private final AudioConfigurationProperties audioConfigurationProperties;

    @Override
    public Path createFile(String fileName, String workspace, boolean appendUuidToWorkspace) throws IOException {
        Path workspacePath = Path.of(workspace);
        if (appendUuidToWorkspace) {
            workspacePath = workspacePath.resolve(UUID.randomUUID().toString());
        }
        Path targetTempFile = workspacePath.resolve(fileName);
        Files.createDirectories(workspacePath);
        return Files.createFile(targetTempFile);
    }

    @Override
    public Path saveFileToTempWorkspace(InputStream mediaFile, String fileName) throws IOException {

        Path tempFilePath;

        try (InputStream audioInputStream = mediaFile) {
            tempFilePath = createFile(fileName, audioConfigurationProperties.getTempBlobWorkspace(), true);
            Files.copy(audioInputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("IOException. Unable to copy Blob Data to temporary workspace");
            throw new IOException(e);
        }

        return tempFilePath;
    }

    @Override
    public Path saveBinaryDataToSpecifiedWorkspace(BinaryData binaryData, String fileName, String workspace, boolean appendUuidToWorkspace)
        throws IOException {

        Path targetTempFile;

        try (InputStream inputStream = binaryData.toStream()) {
            targetTempFile = createFile(fileName, workspace, appendUuidToWorkspace);
            Files.copy(inputStream, targetTempFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Unable to copy binary data to workspace {} - {}", workspace, e.getMessage());
            throw new IOException(e);
        }

        return targetTempFile;
    }

    @Override
    public BinaryData convertFileToBinaryData(String fileName) {
        return BinaryData.fromFile(Path.of(fileName));
    }

}
