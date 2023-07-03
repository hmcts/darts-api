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

@Service
@RequiredArgsConstructor
@Slf4j
public class FileOperationServiceImpl implements FileOperationService {

    private final AudioConfigurationProperties audioConfigurationProperties;

    @Override
    public Path saveFileToTempWorkspace(BinaryData mediaFile, String fileName) throws IOException {
        // get local logger or check with Hemanta on the project logging strategy

        Path targetTempDirectory = Path.of(audioConfigurationProperties.getTempBlobWorkspace());
        Path targetTempFile = targetTempDirectory.resolve(fileName);

        try (InputStream audioInputStream = mediaFile.toStream()) {
            Files.createDirectories(targetTempDirectory);
            Path tempFilePath = Files.createFile(targetTempFile);
            Files.copy(audioInputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            log.error("");
            throw new IOException(e);
        }

        return targetTempFile;
    }
}
