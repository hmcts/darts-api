package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.OutboundFileProcessor;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.model.ChannelAudio;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.time.ZoneOffset.UTC;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
public class OutboundFileProcessorImpl implements OutboundFileProcessor {

    private final AudioOperationService audioOperationService;
    private final AudioConfigurationProperties audioConfigurationProperties;
    private static final int ONE = 1;
    private static final int TWO = 2;

    /**
     * Group the provided media/audio into logical groups in preparation for zipping with OutboundFileZipGenerator.
     *
     * @param mediaEntityToDownloadLocation Map relating each mediaEntity to the local filepath of its associated
     *                                      downloaded audio file.
     * @param mediaRequestStartTime         The time at which the first media/audio session should be trimmed to.
     * @param mediaRequestEndTime           The time at which the last media/audio session should be trimmed to.
     * @return A grouping of multichannel audio files for a recording session, trimmed to the request times.
     */
    @Override
    public List<List<AudioFileInfo>> processAudioForDownload(Map<MediaEntity, Path> mediaEntityToDownloadLocation,
                                                             OffsetDateTime mediaRequestStartTime,
                                                             OffsetDateTime mediaRequestEndTime)
        throws ExecutionException, InterruptedException, IOException {
        List<AudioFileInfo> audioFileInfos = mapToAudioFileInfos(mediaEntityToDownloadLocation);

        List<List<AudioFileInfo>> groupedAudioSessions = new ArrayList<>();
        for (AudioFileInfo audioFileInfo : audioFileInfos) {
            groupBySession(audioFileInfo, groupedAudioSessions);
        }

        List<List<AudioFileInfo>> finalisedAudioSessions = new ArrayList<>();

        final int numberOfSessions = groupedAudioSessions.size();
        if (numberOfSessions >= 1) {
            finalisedAudioSessions.add(trimAll(
                groupedAudioSessions.get(0), // trim start of session
                mediaRequestStartTime,
                mediaRequestEndTime
            ));
        }

        if (numberOfSessions > TWO) {
            finalisedAudioSessions.addAll(groupedAudioSessions.subList(1, numberOfSessions - 1)); // no need to trim mid session
        }

        if (numberOfSessions > ONE) {
            finalisedAudioSessions.add(trimAll(
                groupedAudioSessions.get(numberOfSessions - 1), // trim end of session
                mediaRequestStartTime,
                mediaRequestEndTime
            ));
        }

        return finalisedAudioSessions;
    }

    @Override
    @SuppressWarnings({"PMD.CognitiveComplexity"})
    public List<AudioFileInfo> processAudioForPlaybacks(Map<MediaEntity, Path> mediaEntityToDownloadLocation,
                                                        OffsetDateTime mediaRequestStartTime,
                                                        OffsetDateTime mediaRequestEndTime)
        throws ExecutionException, InterruptedException, IOException {

        List<AudioFileInfo> audioFileInfos = mapToAudioFileInfos(mediaEntityToDownloadLocation);

        List<AudioFileInfo> concatenatedAndMergedAudioFileInfos = new ArrayList<>();
        if (isNotEmpty(audioFileInfos)) {
            // Used for logging only
            String audioFilenames = audioFileInfos.stream().map(AudioFileInfo::getMediaFile).collect(Collectors.joining(", "));

            List<ChannelAudio> concatenationsList = new ArrayList<>();

            
            if (isWellFormedAudio(audioFileInfos)) {
                log.debug("Audio files {} are well formed", audioFilenames);
                List<ChannelAudio> concatenatedAudios = concatenateByChannelWithGaps(audioFileInfos);
                if (isNotEmpty(concatenatedAudios)) {
                    concatenationsList = convertChannelsListToConcatenationsList(concatenatedAudios);
                }
            } else {
                log.debug("Audio files {} are not well formed", audioFilenames);
                concatenationsList = convertChannelsListToFilesList(audioFileInfos);
            }

            for (ChannelAudio audioFileInfoList : concatenationsList) {
                AudioFileInfo mergedAudio = merge(audioFileInfoList.getAudioFiles());
                if (nonNull(mergedAudio)) {
                    OffsetDateTime mergedAudioStartTime = mergedAudio.getStartTime().atOffset(UTC);
                    OffsetDateTime mergedAudioEndTime = mergedAudio.getEndTime().atOffset(UTC);
                    AudioFileInfo trimmedAudio = trim(
                        mergedAudio,
                        mergedAudioStartTime.isAfter(mediaRequestStartTime) ? mergedAudioStartTime : mediaRequestStartTime,
                        mergedAudioEndTime.isBefore(mediaRequestEndTime) ? mergedAudioEndTime : mediaRequestEndTime
                    );
                    concatenatedAndMergedAudioFileInfos.add(reEncode(trimmedAudio));
                } else {
                    throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, "No media present to process");
                }
            }
        }
        return concatenatedAndMergedAudioFileInfos;
    }

    private boolean isWellFormedAudio(List<AudioFileInfo> audioFileInfos) {

        // group by channel
        Map<Integer, List<AudioFileInfo>> audioFileInfosByChannel = audioFileInfos.stream()
            .collect(Collectors.groupingBy(AudioFileInfo::getChannel));

        if (audioFileInfosByChannel.values().size() > 1) {
            // collect number in each channel group
            List<Integer> numberOfChannelsList = new ArrayList<>();
            audioFileInfosByChannel.forEach((channelNumber, audioFileInfoList) -> numberOfChannelsList.add(audioFileInfoList.size()));

            // group by start time
            Map<Instant, List<AudioFileInfo>> audioFileInfosByStartTime = audioFileInfos.stream()
                .collect(Collectors.groupingBy(AudioFileInfo::getStartTime));

            String audioFilenames = audioFileInfos.stream().map(AudioFileInfo::getMediaFile).collect(Collectors.joining(", "));

            if (isNotEmpty(numberOfChannelsList)) {

                // collect number in each start time group
                List<Integer> numberOfStartTimesList = new ArrayList<>();
                boolean numberOfChannelsMatch;
                boolean startTimesMatch;
                audioFileInfosByStartTime.forEach((startTime, audioFileInfoList) -> numberOfStartTimesList.add(audioFileInfoList.size()));

                numberOfChannelsMatch = numberOfChannelsList.stream().allMatch(numberOfChannelsList.get(0)::equals);
                startTimesMatch = numberOfStartTimesList.stream().allMatch(numberOfStartTimesList.get(0)::equals)
                    && !numberOfStartTimesList.contains(1);
                log.debug("Do channels match: {}, start times match: {} for audios {}", numberOfChannelsMatch, startTimesMatch, audioFilenames);
                return numberOfChannelsMatch && startTimesMatch;
            } else {
                log.debug("Unable to group audio by channels", audioFilenames);
                return false;
            }
        }
        return true;
    }

    private List<AudioFileInfo> mapToAudioFileInfos(Map<MediaEntity, Path> mediaEntityPathMap) {
        List<AudioFileInfo> audioFileInfos = new ArrayList<>();
        if (nonNull(mediaEntityPathMap)) {
            for (Entry<MediaEntity, Path> mediaEntityPathEntry : mediaEntityPathMap.entrySet()) {
                audioFileInfos.add(mapToAudioFileInfo(mediaEntityPathEntry));
            }
        }
        return audioFileInfos;
    }

    private AudioFileInfo mapToAudioFileInfo(Entry<MediaEntity, Path> mediaEntityPathEntry) {
        MediaEntity mediaEntity = mediaEntityPathEntry.getKey();
        Path path = mediaEntityPathEntry.getValue();

        return AudioFileInfo.builder()
            .startTime(mediaEntity.getStart().toInstant())
            .endTime(mediaEntity.getEnd().toInstant())
            .channel(mediaEntity.getChannel())
            .mediaFile(mediaEntity.getMediaFile())
            .path(path)
            .build();
    }

    private void groupBySession(AudioFileInfo ungroupedAudioFileInfo, List<List<AudioFileInfo>> groupings) {
        for (List<AudioFileInfo> groupedAudioFileInfos : groupings) {
            for (AudioFileInfo groupedAudioFileInfo : groupedAudioFileInfos) {
                if (isSameSession(groupedAudioFileInfo, ungroupedAudioFileInfo)) {
                    groupedAudioFileInfos.add(ungroupedAudioFileInfo);
                    return;
                }
            }
        }
        groupings.add(new ArrayList<>(Collections.singletonList(ungroupedAudioFileInfo)));
    }

    /**
     * A discriminator used to establish whether the ungroupedAudioFileInfo belongs in the same logical "session" as the
     * groupedAudioFileInfo.
     */
    private boolean isSameSession(AudioFileInfo groupedAudioFileInfo, AudioFileInfo ungroupedAudioFileInfo) {
        return groupedAudioFileInfo.getStartTime().equals(ungroupedAudioFileInfo.getStartTime())
            && groupedAudioFileInfo.getEndTime().equals(ungroupedAudioFileInfo.getEndTime());
    }

    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
    private List<ChannelAudio> concatenateByChannelWithGaps(List<AudioFileInfo> audioFileInfos)
        throws ExecutionException, InterruptedException, IOException {

        List<ChannelAudio> concatenateByChannelWithGaps = new ArrayList<>();

        if (isNotEmpty(audioFileInfos)) {
            Map<Integer, List<AudioFileInfo>> audioFileInfosByChannel = audioFileInfos.stream()
                .collect(Collectors.groupingBy(AudioFileInfo::getChannel));

            for (List<AudioFileInfo> audioFileInfosForChannel : audioFileInfosByChannel.values()) {
                if (audioFileInfosForChannel.size() == 1) {
                    // If there is only one file then there is nothing to concatenate
                    List<AudioFileInfo> processedAudios = new ArrayList<>();
                    processedAudios.add(audioFileInfosForChannel.get(0));
                    concatenateByChannelWithGaps.add(new ChannelAudio(processedAudios));
                    continue;
                }

                List<AudioFileInfo> concatenatedAudios = audioOperationService.concatenateWithGaps(
                    StringUtils.EMPTY,
                    audioFileInfosForChannel,
                    audioConfigurationProperties.getAllowableAudioGapDuration()
                );
                concatenateByChannelWithGaps.add(new ChannelAudio(concatenatedAudios));
            }
        }
        return concatenateByChannelWithGaps;
    }

    private AudioFileInfo merge(List<AudioFileInfo> audioFileInfos)
        throws ExecutionException, InterruptedException, IOException {

        return audioOperationService.merge(audioFileInfos, StringUtils.EMPTY);
    }

    private AudioFileInfo trim(AudioFileInfo audioFileInfo,
                               OffsetDateTime trimStartTime,
                               OffsetDateTime trimEndTime)
        throws ExecutionException, InterruptedException, IOException {
        log.info("Trimming audioFileInfo [{}] from trimStartTime [{}] to trimEndTime [{}]",
                 audioFileInfo, trimStartTime, trimEndTime
        );

        var audioFileStartTime = audioFileInfo.getStartTime();
        return audioOperationService.trim(
            StringUtils.EMPTY,
            audioFileInfo,
            Duration.between(audioFileStartTime, trimStartTime),
            Duration.between(audioFileStartTime, trimEndTime)
        );
    }


    private List<AudioFileInfo> trimAll(List<AudioFileInfo> audioFileInfos,
                                        OffsetDateTime mediaRequestStartTime,
                                        OffsetDateTime mediaRequestEndTime)
        throws ExecutionException, InterruptedException, IOException {
        List<AudioFileInfo> processedAudios = new ArrayList<>();
        for (AudioFileInfo audioFileInfo : audioFileInfos) {
            final boolean audioStartsBeforeRequest = audioFileInfo.getStartTime().isBefore(mediaRequestStartTime.toInstant());
            final boolean audioEndsAfterRequest = audioFileInfo.getEndTime().isAfter(mediaRequestEndTime.toInstant());
            if (audioStartsBeforeRequest || audioEndsAfterRequest) {
                processedAudios.add(trim(
                    audioFileInfo,
                    audioStartsBeforeRequest ? mediaRequestStartTime : audioFileInfo.getStartTime().atOffset(UTC),
                    audioEndsAfterRequest ? mediaRequestEndTime : audioFileInfo.getEndTime().atOffset(UTC)
                ));
            } else {
                processedAudios.add(audioFileInfo);
            }
        }

        return processedAudios;
    }

    private AudioFileInfo reEncode(AudioFileInfo audioFileInfo)
        throws ExecutionException, InterruptedException, IOException {
        return audioOperationService.reEncode(StringUtils.EMPTY, audioFileInfo);
    }

    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
    private static List<ChannelAudio> convertChannelsListToConcatenationsList(List<ChannelAudio> channelsList) {
        List<ChannelAudio> concatenationsList = new ArrayList<>();
        if (isNotEmpty(channelsList)) {
            int numConcatenations = channelsList.get(0).getAudioFiles().size();
            for (int i = 0; i < numConcatenations; i++) {
                List<AudioFileInfo> audioFileInfoList = new ArrayList<>();
                for (ChannelAudio channelAudio : channelsList) {
                    audioFileInfoList.add(channelAudio.getAudioFiles().get(i));
                }
                concatenationsList.add(new ChannelAudio(audioFileInfoList));
            }
        }
        return concatenationsList;
    }

    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
    private static List<ChannelAudio> convertChannelsListToFilesList(List<AudioFileInfo> audioFileInfosByChannel) {

        List<ChannelAudio> audioFileInfoByFileList = new ArrayList<>();
        List<AudioFileInfo> audioFileInfoList = new ArrayList<>();

        if (isNotEmpty(audioFileInfosByChannel)) {

            final String audioFilenames = audioFileInfosByChannel.stream().map(AudioFileInfo::getMediaFile).collect(Collectors.joining(", "));
            AudioFileInfo previousAudio;

            audioFileInfosByChannel.sort(comparing(AudioFileInfo::getStartTime));

            AudioFileInfo thisAudio = audioFileInfosByChannel.get(0);
            audioFileInfoList.add(thisAudio);

            for (int counter = 1; counter < audioFileInfosByChannel.size(); counter++) {
                previousAudio = audioFileInfosByChannel.get(counter - 1);
                thisAudio = audioFileInfosByChannel.get(counter);
                if (thisAudio.getStartTime().compareTo(previousAudio.getStartTime()) != 0) {
                    audioFileInfoByFileList.add(new ChannelAudio(audioFileInfoList));
                    audioFileInfoList = new ArrayList<>();
                }
                audioFileInfoList.add(thisAudio);
            }
            audioFileInfoByFileList.add(new ChannelAudio(audioFileInfoList));
            String reorderedAudioFilenames = audioFileInfosByChannel.stream().map(AudioFileInfo::getMediaFile).collect(Collectors.joining(", "));
            log.debug("Reordered audio files {} to {} by start time", audioFilenames, reorderedAudioFilenames);
        }
        return audioFileInfoByFileList;
    }
}
