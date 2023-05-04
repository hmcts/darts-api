package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.config.AudioTransformConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.util.AudioUtil;

import java.util.List;
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

        AudioFileInfo firstAudioFileInfo = audioFileInfos.get(0);
        Integer channel = firstAudioFileInfo.getChannel();
        String baseFilePath = String.format(
            "%s/%s",
            audioTransformConfigurationProperties.getConcatWorkspace(),
            workspaceDir
        );

        CommandLine command = generateConcatenateCommand(channel, audioFileInfos, baseFilePath);
        audioUtil.execute(command);

        return new AudioFileInfo(
            firstAudioFileInfo.getStartTime(),
            audioFileInfos.get(audioFileInfos.size() - 1).getEndTime(),
            String.format("%s/%s-concat-out.mp2", baseFilePath, channel),
            channel
        );
    }
}
