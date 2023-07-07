package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.OutboundFileZipGenerator;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.exception.AudioError;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboundFileZipGeneratorImpl implements OutboundFileZipGenerator {

    private final AudioConfigurationProperties audioConfigurationProperties;

    /**
     * Produce a structured zip file containing audio files.
     *
     * @param audioSessions A grouping of audio sessions, as produced by OutboundFileProcessor.
     * @return The local filepath of the produced zip file containing the provided audioSessions.
     */
    @Override
    public Path generateAndWriteZip(List<List<AudioFileInfo>> audioSessions) {

        Map<Path, Path> sourceToDestinationPaths = generateZipStructure(audioSessions);
        try {
            var outputPath = Path.of(
                audioConfigurationProperties.getTempBlobWorkspace(),
                String.format("%s.zip", UUID.randomUUID())
            );
            writeZip(sourceToDestinationPaths, outputPath);

            return outputPath;
        } catch (IOException e) {
            throw new DartsApiException(AudioError.FAILED_TO_PROCESS_AUDIO_REQUEST, e);
        }
    }

    private Map<Path, Path> generateZipStructure(List<List<AudioFileInfo>> audioSessions) {
        Map<Path, Path> sourceToDestinationPaths = new HashMap<>();
        for (int i = 0; i < audioSessions.size(); i++) {
            List<AudioFileInfo> audioSession = audioSessions.get(i);
            for (AudioFileInfo audioFileInfo : audioSession) {
                Path path = generateZipPath(i, audioFileInfo);
                sourceToDestinationPaths.put(Path.of(audioFileInfo.getFileName()), path);
            }
        }

        log.debug("Generated zip structure: {}", sourceToDestinationPaths);
        return sourceToDestinationPaths;
    }

    private Path generateZipPath(int directoryIndex, AudioFileInfo audioFileInfo) {
        var directoryName = String.format("%04d", directoryIndex + 1);

        var filename = String.format("%s.a%02d", directoryName, audioFileInfo.getChannel() - 1);

        return Path.of(directoryName, filename);
    }

    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.AssignmentInOperand"})
    private void writeZip(Map<Path, Path> sourceToDestinationPaths, Path outputPath) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(outputPath))) {
            for (Entry<Path, Path> paths : sourceToDestinationPaths.entrySet()) {
                Path sourcePath = paths.getKey();
                Path destinationPath = paths.getValue();

                try (InputStream fileInputStream = Files.newInputStream(sourcePath)) {
                    var zipEntry = new ZipEntry(destinationPath.toString());
                    zipOutputStream.putNextEntry(zipEntry);

                    byte[] readBuffer = new byte[1024];
                    int length;
                    while ((length = fileInputStream.read(readBuffer)) >= 0) {
                        zipOutputStream.write(readBuffer, 0, length);
                    }
                }
            }
        }

        log.debug("Produced zip file: {}", outputPath);
    }

}
