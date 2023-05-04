package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.config.AudioTransformConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.util.AudioConstants;
import uk.gov.hmcts.darts.audio.util.AudioUtil;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioOperationServiceImpl implements AudioOperationService {

    private final AudioTransformConfigurationProperties audioTransformConfigurationProperties;

    private final AudioUtil audioUtil;

    CommandLine generateConcatenateCommand(final Integer channel, final List<AudioFileInfo> audioFileInfos, final String baseFilePath) {
        StringBuilder command = new StringBuilder(audioTransformConfigurationProperties.getFfmpegExecutable());

        for (final AudioFileInfo audioFileInfo : audioFileInfos) {
            command.append(" -i ").append(String.format("%s/%s", baseFilePath, audioFileInfo.getFileName()));
        }

        command.append(" -filter_complex ");

        int concatNumberOfSegments = audioFileInfos.size();
        StringBuilder inputFileAudioStreams = new StringBuilder();
        for (int i = 0; i < concatNumberOfSegments; i++) {
            inputFileAudioStreams.append('[').append(i).append(":a]");
        }

        command.append(String.format("\"%sconcat=n=%d:v=0:a=1\"", inputFileAudioStreams, concatNumberOfSegments))
            .append(' ').append(String.format("%s/%s-concat-out.mp2", baseFilePath, channel));

        return new CommandLine(command.toString());
    }

    @Override
    public AudioFileInfo concatenate(final String workspaceDir, final List<AudioFileInfo> audioFileInfos) throws ExecutionException, InterruptedException {

        Integer channel = getFirstChannel(audioFileInfos);
        String baseFilePath = String.format(
            "%s/%s",
            audioTransformConfigurationProperties.getConcatWorkspace(),
            workspaceDir
        );

        CommandLine command = generateConcatenateCommand(channel, audioFileInfos, baseFilePath);
        audioUtil.execute(command);

        return new AudioFileInfo(
            getEarliestStartTime(audioFileInfos),
            getLatestEndTime(audioFileInfos),
            String.format("%s/%s-concat-out.mp2", baseFilePath, channel),
            channel
        );
    }

    @Override
    public AudioFileInfo merge(final List<AudioFileInfo> audioFilesInfo, String workspaceDir) throws ExecutionException, InterruptedException {

        String baseFilePath = String.format(
            "%s/%s",
            audioTransformConfigurationProperties.getMergeWorkspace(),
            workspaceDir
        );

        Integer numberOfChannels = audioFilesInfo.size();
        String outputFilename = generateOutputFilename(baseFilePath, AudioConstants.AudioOperationTypes.MERGE,
                                                       0, AudioConstants.AudioFileFormats.MP2);

        CommandLine command = new CommandLine(audioTransformConfigurationProperties.getFfmpegExecutable());
        for (AudioFileInfo audioFileInfo : audioFilesInfo) {
            command.addArgument(String.format("-i %s ", audioFileInfo.getFileName()));
        }
        command.addArgument(String.format("-filter_complex amix=inputs=%s:duration=longest %s",
                                          numberOfChannels, outputFilename));

        audioUtil.execute(command);

        return new AudioFileInfo(
            getEarliestStartTime(audioFilesInfo),
            getLatestEndTime(audioFilesInfo),
            outputFilename,
            0);
    }

    public Instant getEarliestStartTime(final List<AudioFileInfo> audioFilesInfo) {
        return audioFilesInfo.stream()
            .map(AudioFileInfo::getStartTime)
            .min(Instant::compareTo)
            .get();
    }

    public Instant getLatestEndTime(final List<AudioFileInfo> audioFilesInfo) {
        return audioFilesInfo.stream()
            .map(AudioFileInfo::getEndTime)
            .max(Instant::compareTo)
            .get();
    }

    private Integer getFirstChannel(final List<AudioFileInfo> audioFilesInfo) {
        return audioFilesInfo.stream()
            .map(AudioFileInfo::getChannel)
            .findFirst()
            .get();
    }

    private String generateOutputFilename(String baseDir, AudioConstants.AudioOperationTypes operationType,
                                          Integer channel, AudioConstants.AudioFileFormats outputFileFormat) {

        String currentTimeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.ENGLISH).format(new Date());
        return String.format("%s/%s/C%s-%s.%s", baseDir, operationType.name().toLowerCase(Locale.ENGLISH),
                                      channel, currentTimeStamp, outputFileFormat.name().toLowerCase(Locale.ENGLISH));
    }


}
