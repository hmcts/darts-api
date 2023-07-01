package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.core.util.BinaryData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.PROCESSING;

@Service
@RequiredArgsConstructor
public class AudioTransformationServiceImpl implements AudioTransformationService {

    private final MediaRequestService mediaRequestService;

    // I am having issues injecting properties from the yaml
//    @Value("${darts.audio.temp-blob-workspace}")
//    private final String tempDir;

    private final String tempDir = "/Users/uchennamani/audiotransform/tempworkspace";

    @Transactional
    @Override
    public MediaRequestEntity processAudioRequest(Integer requestId) {

        return mediaRequestService.updateAudioRequestStatus(requestId, PROCESSING);
    }

    @Override
    public Path saveBlobDataToTempWorkspace(BinaryData mediaFile, String fileName) {
// get logger
        Path targetTempDirectory = Path.of(tempDir);
        Path targetTempFile = targetTempDirectory.resolve(System.currentTimeMillis() + fileName);
        // log step

        try (InputStream audioInputStream = mediaFile.toStream()) {
            Files.createDirectories(targetTempDirectory);
            Path tempFilePath = Files.createFile(targetTempFile);
            Files.copy(audioInputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            //log error
            // implement exception handling. Speak to Chris
            throw new RuntimeException(e);
        }

        return targetTempFile;
    }

}
