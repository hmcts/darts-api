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
    public Path saveFileToTempWorkspace(BinaryData mediaFile, String fileName) throws IOException {

        Path targetTempDirectory = Path.of(audioConfigurationProperties.getTempBlobWorkspace())
            .resolve(UUID.randomUUID().toString());
        Path targetTempFile = targetTempDirectory.resolve(fileName);

        try (InputStream audioInputStream = mediaFile.toStream()) {
            Files.createDirectories(targetTempDirectory);
            Path tempFilePath = Files.createFile(targetTempFile);
            Files.copy(audioInputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            log.error("IOException. Unable to copy Blob Data to temporary workspace");
            throw new IOException(e);
        }

        return targetTempFile;
    }

    public BinaryData saveFileToBinaryData(String fileName) {
        return BinaryData.fromFile(Path.of(fileName));
    }

}
