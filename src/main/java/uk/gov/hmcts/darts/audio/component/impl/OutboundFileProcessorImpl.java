package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.OutboundFileProcessor;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.model.ChannelAudio;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class OutboundFileProcessorImpl implements OutboundFileProcessor {

    private final AudioOperationService audioOperationService;

    @Value("${darts.audio.allowable_audio_gap_duration}")
    private Duration allowableAudioGap;

    /**
     * Group the provided media/audio into logical groups in preparation for zipping with OutboundFileZipGenerator.
     *
     * @param mediaEntityToDownloadLocation Map relating each mediaEntity to the local filepath of its associated
     *                                      downloaded audio file.
     * @param overallStartTime              The time at which the audio start should be trimmed to.
     * @param overallEndTime                The time at which the audio end should be trimmed to.
     * @return A grouping of trimmed and concatenated multichannel audio files, whereby each group is a collection of audio files
     *     that belong to a continuous recording session.
     */
    @Override
    public List<List<AudioFileInfo>> processAudioForDownload(Map<MediaEntity, Path> mediaEntityToDownloadLocation,
                                                             OffsetDateTime overallStartTime,
                                                             OffsetDateTime overallEndTime)
        throws ExecutionException, InterruptedException, IOException {
        List<AudioFileInfo> audioFileInfos = mapToAudioFileInfos(mediaEntityToDownloadLocation);

        List<List<AudioFileInfo>> groupedAudioSessions = new ArrayList<>();
        for (AudioFileInfo audioFileInfo : audioFileInfos) {
            groupBySession(audioFileInfo, groupedAudioSessions);
        }

        List<List<AudioFileInfo>> finalisedAudioSessions = new ArrayList<>();
        for (List<AudioFileInfo> audioSession : groupedAudioSessions) {
            List<AudioFileInfo> trimmedAudios = trimAllToPeriod(
                audioSession,
                overallStartTime,
                overallEndTime
            );
            finalisedAudioSessions.add(trimmedAudios);
        }

        return finalisedAudioSessions;
    }

    @Override
    public List<AudioFileInfo> processAudioForPlaybacks(Map<MediaEntity, Path> mediaEntityToDownloadLocation,
                                                        OffsetDateTime startTime,
                                                        OffsetDateTime endTime)
        throws ExecutionException, InterruptedException, IOException {
        List<AudioFileInfo> audioFileInfos = mapToAudioFileInfos(mediaEntityToDownloadLocation);

        List<AudioFileInfo> concatenatedAndMergedAudioFileInfos = new ArrayList<>();

        List<ChannelAudio> concatenationsList;

        if (isWellFormedAudio(audioFileInfos)) {
            List<ChannelAudio> concatenatedAudios = concatenateByChannelWithGaps(audioFileInfos);
            concatenationsList = convertChannelsListToConcatenationsList(concatenatedAudios);
        } else {
            concatenationsList = convertChannelsListToFilesList(audioFileInfos);
        }

        for (ChannelAudio audioFileInfoList :  concatenationsList) {
            AudioFileInfo mergedAudio = merge(audioFileInfoList.getAudioFiles());
            AudioFileInfo trimmedAudio = trimToPeriod(
                mergedAudio,
                mergedAudio.getStartTime().atOffset(ZoneOffset.UTC),
                mergedAudio.getEndTime().atOffset(ZoneOffset.UTC)
            );
            concatenatedAndMergedAudioFileInfos.add(reEncode((trimmedAudio)));

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
            audioFileInfosByChannel.forEach((channelNumber,audioFileInfoList) -> numberOfChannelsList.add(audioFileInfoList.size()));

            // group by start time
            Map<Instant, List<AudioFileInfo>> audioFileInfosByStartTime = audioFileInfos.stream()
                .collect(Collectors.groupingBy(AudioFileInfo::getStartTime));

            // collect number in each start time group
            List<Integer> numberOfStartTimesList = new ArrayList<>();
            audioFileInfosByStartTime.forEach((startTime,audioFileInfoList) -> numberOfStartTimesList.add(audioFileInfoList.size()));

            boolean numberOfChannelsMatch = numberOfChannelsList.stream().allMatch(numberOfChannelsList.get(0)::equals);
            boolean startTimesMatch = numberOfStartTimesList.stream().allMatch(numberOfStartTimesList.get(0)::equals) && !numberOfStartTimesList.contains(1);

            return numberOfChannelsMatch && startTimesMatch;
        }
        return true;
    }

    private List<AudioFileInfo> mapToAudioFileInfos(Map<MediaEntity, Path> mediaEntityPathMap) {
        List<AudioFileInfo> audioFileInfos = new ArrayList<>();
        for (Entry<MediaEntity, Path> mediaEntityPathEntry : mediaEntityPathMap.entrySet()) {
            audioFileInfos.add(mapToAudioFileInfo(mediaEntityPathEntry));
        }
        return audioFileInfos;
    }

    private AudioFileInfo mapToAudioFileInfo(Entry<MediaEntity, Path> mediaEntityPathEntry) {
        MediaEntity mediaEntity = mediaEntityPathEntry.getKey();
        Path path = mediaEntityPathEntry.getValue();

        return new AudioFileInfo(
            mediaEntity.getStart().toInstant(),
            mediaEntity.getEnd().toInstant(),
            path.toString(),
            mediaEntity.getChannel(),
            path
        );
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

    private List<ChannelAudio> concatenateByChannelWithGaps(List<AudioFileInfo> audioFileInfos)
        throws ExecutionException, InterruptedException, IOException {
        Map<Integer, List<AudioFileInfo>> audioFileInfosByChannel = audioFileInfos.stream()
            .collect(Collectors.groupingBy(AudioFileInfo::getChannel));

        List<ChannelAudio> concatenateByChannelWithGaps = new ArrayList<>();

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
                allowableAudioGap
            );
            concatenateByChannelWithGaps.add(new ChannelAudio(concatenatedAudios));
        }

        return concatenateByChannelWithGaps;
    }

    private AudioFileInfo merge(List<AudioFileInfo> audioFileInfos)
        throws ExecutionException, InterruptedException, IOException {

        return audioOperationService.merge(audioFileInfos, StringUtils.EMPTY);
    }

    private AudioFileInfo trimToPeriod(AudioFileInfo audioFileInfo, OffsetDateTime trimPeriodStart,
                                       OffsetDateTime trimPeriodEnd)
        throws ExecutionException, InterruptedException, IOException {
        var audioFileStartTime = audioFileInfo.getStartTime();

        log.info("Trimming dates for ATS. Audio file start time is: " + audioFileStartTime.toString() + ", trim period start is: "
                     + trimPeriodStart.toString() + ", trim period end is: " + trimPeriodEnd.toString());

        var trimStartDuration = Duration.between(audioFileStartTime, trimPeriodStart);
        var trimEndDuration = Duration.between(audioFileStartTime, trimPeriodEnd);

        return audioOperationService.trim(
            StringUtils.EMPTY,
            audioFileInfo,
            trimStartDuration,
            trimEndDuration
        );
    }


    private List<AudioFileInfo> trimAllToPeriod(List<AudioFileInfo> audioFileInfos, OffsetDateTime start,
                                                OffsetDateTime end)
        throws ExecutionException, InterruptedException, IOException {
        List<AudioFileInfo> processedAudios = new ArrayList<>();
        for (AudioFileInfo audioFileInfo : audioFileInfos) {
            AudioFileInfo trimmedAudio = trimToPeriod(audioFileInfo, start, end);
            processedAudios.add(trimmedAudio);
        }

        return processedAudios;
    }

    private AudioFileInfo reEncode(AudioFileInfo audioFileInfo)
        throws ExecutionException, InterruptedException, IOException {
        return audioOperationService.reEncode(StringUtils.EMPTY, audioFileInfo);
    }

    public static List<ChannelAudio> convertChannelsListToConcatenationsList(List<ChannelAudio> channelsList) {
        List<ChannelAudio> concatenationsList = new ArrayList<>();
        int numChannels = channelsList.size();
        int numConcatenations = channelsList.get(0).getAudioFiles().size();
        for (int i = 0; i < numConcatenations; i++) {
            List<AudioFileInfo> audioFileInfoList = new ArrayList<>();
            for (int j = 0; j < numChannels; j++) {
                audioFileInfoList.add(channelsList.get(j).getAudioFiles().get(i));
            }
            concatenationsList.add(new ChannelAudio(audioFileInfoList));
        }
        return concatenationsList;
    }

    public static List<ChannelAudio> convertChannelsListToFilesList(List<AudioFileInfo> audioFileInfosByChannel) {

        audioFileInfosByChannel.sort(comparing(AudioFileInfo::getStartTime));

        List<ChannelAudio> audioFileInfoByFileList = new ArrayList<>();
        List<AudioFileInfo> audioFileInfoList = new ArrayList<>();
        AudioFileInfo previousAudio;
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

        return audioFileInfoByFileList;
    }
}
