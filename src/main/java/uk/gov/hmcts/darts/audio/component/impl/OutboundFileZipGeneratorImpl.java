package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.OutboundFileZipGenerator;
import uk.gov.hmcts.darts.audio.component.OutboundFileZipGeneratorHelper;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.model.PlaylistInfo;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static uk.gov.hmcts.darts.common.util.DateConverterUtil.EUROPE_LONDON_ZONE;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboundFileZipGeneratorImpl implements OutboundFileZipGenerator {

    private static final String DAUDIO = "daudio";
    private static final String LOCALAUDIO = "localaudio";
    private static final String VIQ_METADATA_TYPE = "Zip";
    private final AudioConfigurationProperties audioConfigurationProperties;
    private final OutboundFileZipGeneratorHelper outboundFileZipGeneratorHelper;

    /**
     * Produce a structured zip file containing audio files.
     *
     * @param audioSessions      A grouping of audio sessions, as produced by OutboundFileProcessor.
     * @param mediaRequestEntity Details of media request
     * @return The local filepath of the produced zip file containing the provided audioSessions.
     */
    @Override
    public Path generateAndWriteZip(List<List<AudioFileInfo>> audioSessions, MediaRequestEntity mediaRequestEntity) {

        try {
            Path mediaRequestDir = Path.of(
                audioConfigurationProperties.getTempBlobWorkspace(),
                mediaRequestEntity.getId().toString()
            );
            if (Files.notExists(mediaRequestDir)) {
                mediaRequestDir = Files.createDirectory(mediaRequestDir);
            }

            Map<Path, Path> sourceToDestinationPaths = generateZipStructure(
                audioSessions,
                mediaRequestEntity,
                mediaRequestDir.toString()
            );

            var outputPath = Path.of(
                audioConfigurationProperties.getTempBlobWorkspace(),
                String.format("%s.zip", UUID.randomUUID())
            );
            writeZip(sourceToDestinationPaths, outputPath);

            return outputPath;
        } catch (IOException e) {
            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, e);
        }
    }

    private ViqMetaData createViqMetaData(MediaRequestEntity mediaRequestEntity) {
        return ViqMetaData.builder()
            .courthouse(mediaRequestEntity.getHearing().getCourtroom().getCourthouse().getCourthouseName())
            .raisedBy(mediaRequestEntity.getRequestor().getId().toString())
            .startTime(DateConverterUtil.toZonedDateTime(mediaRequestEntity.getStartTime()))
            .endTime(DateConverterUtil.toZonedDateTime(mediaRequestEntity.getEndTime()))
            .type(VIQ_METADATA_TYPE)
            .build();
    }

    private Map<Path, Path> generateZipStructure(List<List<AudioFileInfo>> audioSessions,
                                                 MediaRequestEntity mediaRequestEntity,
                                                 String mediaRequestDirString) {
        Map<Path, Path> sourceToDestinationPaths = new HashMap<>();

        HearingEntity hearingEntity = mediaRequestEntity.getHearing();
        final String caseNumber = hearingEntity.getCourtCase().getCaseNumber();

        sourceToDestinationPaths.put(Path.of(outboundFileZipGeneratorHelper.generateReadme(
            createViqMetaData(mediaRequestEntity),
            mediaRequestDirString
        )), Path.of("readMe.txt"));

        Set<PlaylistInfo> playlistInfos = new LinkedHashSet<>();

        for (int i = 0; i < audioSessions.size(); i++) {
            List<AudioFileInfo> audioSession = audioSessions.get(i);
            for (AudioFileInfo audioFileInfo : audioSession) {
                ZonedDateTime localStartTime = ZonedDateTime.ofInstant(
                    audioFileInfo.getStartTime(),
                    EUROPE_LONDON_ZONE
                );
                ZonedDateTime localEndTime = ZonedDateTime.ofInstant(
                    audioFileInfo.getEndTime(),
                    EUROPE_LONDON_ZONE
                );

                Path path = generateZipPath(caseNumber, i, audioFileInfo);
                Path viqOutputFile = Path.of(mediaRequestDirString, path.toString());
                Path viqFilePath = outboundFileZipGeneratorHelper.generateViqFile(audioFileInfo, viqOutputFile);
                sourceToDestinationPaths.put(viqFilePath, path);

                String parentPathString = path.getParent().toString();
                playlistInfos.add(PlaylistInfo.builder()
                                      .caseNumber(caseNumber)
                                      .startTime(localStartTime)
                                      .fileLocation(parentPathString)
                                      .build());

                Path annotationsOutputFile = Path.of(
                    mediaRequestDirString,
                    String.format("%d_%s", i, "annotations.xml")
                );
                if (Files.notExists(annotationsOutputFile)) {
                    sourceToDestinationPaths.put(Path.of(outboundFileZipGeneratorHelper.generateAnnotation(
                        hearingEntity,
                        localStartTime,
                        localEndTime,
                        annotationsOutputFile.toString()
                    )), Path.of(parentPathString, "annotations.xml"));
                }
            }
        }

        sourceToDestinationPaths.put(Path.of(outboundFileZipGeneratorHelper.generatePlaylist(
            playlistInfos,
            mediaRequestDirString
        )), Path.of("playlist.xml"));

        log.debug("Generated zip structure: {}", sourceToDestinationPaths);
        return sourceToDestinationPaths;
    }

    private Path generateZipPath(String caseNumber, int directoryIndex, AudioFileInfo audioFileInfo) {
        var nameElement1 = DAUDIO;
        var nameElement2 = LOCALAUDIO;
        var nameElement3 = caseNumber.substring(0, 5);
        var nameElement4 = caseNumber.substring(5);
        var nameElement5 = String.format("%04d", directoryIndex + 1);
        var filename = String.format("%s.a%02d", nameElement5, audioFileInfo.getChannel() - 1);

        return Path.of(nameElement1, nameElement2, nameElement3, nameElement4, nameElement5, filename);
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