package uk.gov.hmcts.darts.common.service;

import com.azure.core.util.BinaryData;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public interface FileOperationService {


    private final AudioConfigurationProperties audioConfigurationProperties;

    public Path saveBlobDataToTempWorkspace(BinaryData mediaFile, String fileName) throws IOException {
        // get local logger or check with Hemanta on the project logging strategy
        // ca// file ops service

        Path targetTempDirectory = Path.of(audioConfigurationProperties.getTempBlobWorkspace());
        Path targetTempFile = targetTempDirectory.resolve(fileName);

        // log step

        try (InputStream audioInputStream = mediaFile.toStream()) {
            Files.createDirectories(targetTempDirectory);
            Path tempFilePath = Files.createFile(targetTempFile);
            Files.copy(audioInputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            //log error

            // implement exception handling. Speak to Chris Bellingham on the project error handling strategy
            throw new RuntimeException(e);
        }

        return targetTempFile;
    }


}
