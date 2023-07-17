package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.component.SystemCommandExecutor;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.audio.util.AudioConstants;
import uk.gov.hmcts.darts.audio.util.AudioConstants.AudioOperationTypes;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioOperationServiceImpl implements AudioOperationService {

    private static final String STRING_SLASH_STRING_FORMAT = "%s/%s";

    private final AudioConfigurationProperties audioConfigurationProperties;
    private final SystemCommandExecutor systemCommandExecutor;

    CommandLine generateConcatenateCommand(final Integer channel, final List<AudioFileInfo> audioFileInfos,
                                           final String outputFilename) {
        StringBuilder command = new StringBuilder(audioConfigurationProperties.getFfmpegExecutable());

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
            .append(' ').append(outputFilename);

        return CommandLine.parse(command.toString());
    }

    @Override
    public AudioFileInfo concatenate(final String workspaceDir, final List<AudioFileInfo> audioFileInfos)
        throws ExecutionException, InterruptedException {

        Integer channel = getFirstChannel(audioFileInfos);
        String baseFilePath = String.format(
            STRING_SLASH_STRING_FORMAT,
            audioConfigurationProperties.getConcatWorkspace(),
            workspaceDir
        );
        String outputFilename = generateOutputFilename(baseFilePath, AudioOperationTypes.CONCATENATE,
                                                       channel, AudioConstants.AudioFileFormats.MP2
        );
        CommandLine command = generateConcatenateCommand(channel, audioFileInfos, outputFilename);
        systemCommandExecutor.execute(command);

        return new AudioFileInfo(
            getEarliestStartTime(audioFileInfos),
            getLatestEndTime(audioFileInfos),
            outputFilename,
            channel
        );
    }

    @Override
    public AudioFileInfo merge(final List<AudioFileInfo> audioFilesInfo, String workspaceDir)
        throws ExecutionException, InterruptedException {

        String baseFilePath = String.format(
            STRING_SLASH_STRING_FORMAT,
            audioConfigurationProperties.getMergeWorkspace(),
            workspaceDir
        );

        Integer numberOfChannels = audioFilesInfo.size();
        String outputFilename = generateOutputFilename(baseFilePath, AudioConstants.AudioOperationTypes.MERGE,
                                                       0, AudioConstants.AudioFileFormats.MP2
        );

        CommandLine command = new CommandLine(audioConfigurationProperties.getFfmpegExecutable());
        for (AudioFileInfo audioFileInfo : audioFilesInfo) {
            command.addArgument("-i").addArgument(audioFileInfo.getFileName());
        }
        command.addArgument("-filter_complex")
            .addArgument(String.format("amix=inputs=%d:duration=longest", numberOfChannels))
            .addArgument(outputFilename);

        systemCommandExecutor.execute(command);

        return new AudioFileInfo(
            getEarliestStartTime(audioFilesInfo),
            getLatestEndTime(audioFilesInfo),
            outputFilename,
            0
        );
    }

    @Override
    public AudioFileInfo trim(String workspaceDir, AudioFileInfo audioFileInfo, String startTime, String endTime)
        throws ExecutionException, InterruptedException {

        String baseFilePath = String.format(
            STRING_SLASH_STRING_FORMAT,
            audioConfigurationProperties.getTrimWorkspace(),
            workspaceDir
        );

        String outputFilename = generateOutputFilename(baseFilePath, AudioOperationTypes.TRIM,
                                                       audioFileInfo.getChannel(), AudioConstants.AudioFileFormats.MP2
        );

        CommandLine command = new CommandLine(audioConfigurationProperties.getFfmpegExecutable());
        command.addArgument("-i").addArgument(audioFileInfo.getFileName());
        command.addArgument("-ss").addArgument(startTime);
        command.addArgument("-to").addArgument(endTime);
        command.addArgument("-c").addArgument("copy").addArgument(outputFilename);

        systemCommandExecutor.execute(command);

        return new AudioFileInfo(
            adjustTimeDuration(audioFileInfo.getStartTime(), startTime),
            adjustTimeDuration(audioFileInfo.getStartTime(), endTime),
            outputFilename,
            audioFileInfo.getChannel()
        );
    }

    @Override
    public AudioFileInfo reEncode(String workspaceDir, AudioFileInfo audioFileInfo)
        throws ExecutionException, InterruptedException {

        String baseFilePath = String.format(
            STRING_SLASH_STRING_FORMAT,
            audioConfigurationProperties.getReEncodeWorkspace(),
            workspaceDir
        );

        String outputFilename = generateOutputFilename(baseFilePath, AudioOperationTypes.ENCODE,
                                                       audioFileInfo.getChannel(), AudioConstants.AudioFileFormats.MP3
        );

        CommandLine command = new CommandLine(audioConfigurationProperties.getFfmpegExecutable());
        command.addArgument("-i").addArgument(audioFileInfo.getFileName());
        command.addArgument(outputFilename);

        systemCommandExecutor.execute(command);

        return new AudioFileInfo(
            audioFileInfo.getStartTime(),
            audioFileInfo.getEndTime(),
            outputFilename,
            audioFileInfo.getChannel()
        );
    }

    Instant adjustTimeDuration(Instant time, String timeDuration) {
        Instant adjustedInstant = Instant.from(time);
        LocalTime localTime = LocalTime.parse(timeDuration);
        adjustedInstant = adjustedInstant.plus(localTime.get(ChronoField.HOUR_OF_DAY), ChronoUnit.HOURS);
        adjustedInstant = adjustedInstant.plus(localTime.get(ChronoField.MINUTE_OF_HOUR), ChronoUnit.MINUTES);
        adjustedInstant = adjustedInstant.plus(localTime.get(ChronoField.SECOND_OF_MINUTE), ChronoUnit.SECONDS);
        return adjustedInstant;
    }

    private Instant getEarliestStartTime(final List<AudioFileInfo> audioFilesInfo) {
        var earliestStartDate = audioFilesInfo.stream()
            .map(AudioFileInfo::getStartTime)
            .min(Instant::compareTo);

        return earliestStartDate.orElseThrow();
    }

    private Instant getLatestEndTime(final List<AudioFileInfo> audioFilesInfo) {
        var latestEndTime = audioFilesInfo.stream()
            .map(AudioFileInfo::getEndTime)
            .max(Instant::compareTo);

        return latestEndTime.orElseThrow();
    }

    private Integer getFirstChannel(final List<AudioFileInfo> audioFilesInfo) {
        var firstChannelNumber = audioFilesInfo.stream()
            .map(AudioFileInfo::getChannel)
            .findFirst();

        return firstChannelNumber.orElseThrow();
    }

    private String generateOutputFilename(String baseDir, AudioConstants.AudioOperationTypes operationType,
                                          Integer channel, AudioConstants.AudioFileFormats outputFileFormat) {

        String currentTimeStamp = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.ENGLISH).format(new Date());
        return String.format(
            "%s/C%s-%s-%s.%s",
            baseDir,
            channel,
            operationType.name().toLowerCase(Locale.ENGLISH),
            currentTimeStamp,
            outputFileFormat.name().toLowerCase(Locale.ENGLISH)
        );
    }

}
