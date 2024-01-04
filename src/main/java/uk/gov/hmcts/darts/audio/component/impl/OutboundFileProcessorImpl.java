package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.OutboundFileProcessor;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
            List<AudioFileInfo> concatenatedAudios = concatenateByChannel(audioSession);
            List<AudioFileInfo> trimmedAudios = trimAllToPeriod(
                concatenatedAudios,
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
        List<List<AudioFileInfo>> concatenatedAudios = concatenateByChannelWithGaps(audioFileInfos);
        List<List<AudioFileInfo>> concatenationsList = convertChannelsListToConcatenationsList(concatenatedAudios);

        for (List<AudioFileInfo> audioFileInfoList :  concatenationsList) {
            AudioFileInfo mergedAudio = merge(audioFileInfoList);
            AudioFileInfo trimmedAudio = trimToPeriod(mergedAudio, mergedAudio.getStartTime().atOffset(ZoneOffset.UTC),
                                                      mergedAudio.getEndTime().atOffset(ZoneOffset.UTC));
            concatenatedAndMergedAudioFileInfos.add(reEncode((trimmedAudio)));

        }

        return concatenatedAndMergedAudioFileInfos;
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
        boolean hasEqualTimestamps = groupedAudioFileInfo.getStartTime().equals(ungroupedAudioFileInfo.getStartTime())
            && groupedAudioFileInfo.getEndTime().equals(ungroupedAudioFileInfo.getEndTime());

        boolean hasContinuity = ungroupedAudioFileInfo.getChannel().equals(groupedAudioFileInfo.getChannel())
            && (timeOverlaps(ungroupedAudioFileInfo, groupedAudioFileInfo.getEndTime().plusSeconds(allowableAudioGap.toSeconds()))
            || timeOverlaps(groupedAudioFileInfo, ungroupedAudioFileInfo.getEndTime().plusSeconds(allowableAudioGap.toSeconds())));

        return hasEqualTimestamps || hasContinuity;
    }

    private boolean timeOverlaps(AudioFileInfo audioFileInfo, Instant timeToCheck) {
        return !timeToCheck.isBefore(audioFileInfo.getStartTime())//is on or after start time
            && !timeToCheck.isAfter(audioFileInfo.getEndTime()); //is on or before end time
    }

    private List<AudioFileInfo> concatenateByChannel(List<AudioFileInfo> audioFileInfos)
        throws ExecutionException, InterruptedException, IOException {
        Map<Integer, List<AudioFileInfo>> audioFileInfosByChannel = audioFileInfos.stream()
            .collect(Collectors.groupingBy(AudioFileInfo::getChannel));

        List<AudioFileInfo> processedAudios = new ArrayList<>();

        for (List<AudioFileInfo> audioFileInfosForChannel : audioFileInfosByChannel.values()) {
            if (audioFileInfosForChannel.size() == 1) {
                // If there is only one file then there is nothing to concatenate
                processedAudios.add(audioFileInfosForChannel.get(0));
                continue;
            }

            // Sort to be sure concatenation occurs in chronological order
            audioFileInfosForChannel.sort(Comparator.comparing(AudioFileInfo::getStartTime));
            AudioFileInfo concatenatedAudio = audioOperationService.concatenate(
                StringUtils.EMPTY,
                audioFileInfosForChannel
            );
            processedAudios.add(concatenatedAudio);
        }

        return processedAudios;
    }

    private List<List<AudioFileInfo>> concatenateByChannelWithGaps(List<AudioFileInfo> audioFileInfos)
        throws ExecutionException, InterruptedException, IOException {
        Map<Integer, List<AudioFileInfo>> audioFileInfosByChannel = audioFileInfos.stream()
            .collect(Collectors.groupingBy(AudioFileInfo::getChannel));

        List<List<AudioFileInfo>> concatenateByChannelWithGaps = new ArrayList<>();

        for (List<AudioFileInfo> audioFileInfosForChannel : audioFileInfosByChannel.values()) {
            if (audioFileInfosForChannel.size() == 1) {
                // If there is only one file then there is nothing to concatenate
                List<AudioFileInfo> processedAudios = new ArrayList<>();
                processedAudios.add(audioFileInfosForChannel.get(0));
                concatenateByChannelWithGaps.add(processedAudios);
                continue;
            }

            List<AudioFileInfo> concatenatedAudios = audioOperationService.concatenateWithGaps(
                StringUtils.EMPTY,
                audioFileInfosForChannel,
                allowableAudioGap
            );
            concatenateByChannelWithGaps.add(concatenatedAudios);
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

    public static List<List<AudioFileInfo>> convertChannelsListToConcatenationsList(List<List<AudioFileInfo>> channelsList) {
        List<List<AudioFileInfo>> concatenationsList = new ArrayList<>();
        int numChannels = channelsList.size();
        int numConcatenations = channelsList.get(0).size();
        for (int i = 0; i < numConcatenations; i++) {
            List<AudioFileInfo> audioFileInfoList = new ArrayList<>();
            for (int j = 0; j < numChannels; j++) {
                audioFileInfoList.add(channelsList.get(j).get(i));
            }
            concatenationsList.add(audioFileInfoList);
        }
        return concatenationsList;
    }
}
