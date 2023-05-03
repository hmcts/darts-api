package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.config.AudioTransformConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioOperationServiceImpl implements AudioOperationService {

    private final AudioTransformConfigurationProperties audioTransformConfigurationProperties;

    String generateConcatenateCommand(final Integer channel, final List<AudioFileInfo> audioFileInfos) {
        StringBuilder command = new StringBuilder(audioTransformConfigurationProperties.getFfmpegExecutable());

        for (final AudioFileInfo audioFileInfo : audioFileInfos) {
            command.append(" -i ").append(audioFileInfo.getFileName());
        }

        command.append(" -filter_complex ");

        int concatNumberOfSegments = audioFileInfos.size();
        StringBuilder inputFileAudioStreams = new StringBuilder();
        for (int i = 0; i < concatNumberOfSegments; i++) {
            inputFileAudioStreams.append('[').append(i).append(":a]");
        }

        command.append(String.format("\"%sconcat=n=%d:v=0:a=1\"", inputFileAudioStreams, concatNumberOfSegments))
            .append(' ').append(String.format("%s-concat-out.mp2", channel));

        return command.toString();
    }

    @Override
    public AudioFileInfo concatenate(final String workspaceDir, final List<AudioFileInfo> audioFileInfos) {

        String baseFilePath = String.format(
            "%s/%s",
            audioTransformConfigurationProperties.getConcatWorkspace(),
            workspaceDir
        );

        AudioFileInfo firstAudioFileInfo = audioFileInfos.get(0);
        Integer channel = firstAudioFileInfo.getChannel();

        String command = generateConcatenateCommand(channel, audioFileInfos);
        execute(baseFilePath, command, channel);

        return new AudioFileInfo(
            firstAudioFileInfo.getStartTime(),
            audioFileInfos.get(audioFileInfos.size() - 1).getEndTime(),
            String.format("%s-concat-out.mp2", channel),
            channel
        );
    }

    private void execute(String baseFilePath, String command, Integer channel) {
        log.info(
            "command [{}], baseFilePath [{}], channel [{}]",
            command,
            baseFilePath,
            channel
        );
    }
}
