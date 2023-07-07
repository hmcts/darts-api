package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.component.OutboundFileProcessor;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.service.AudioOperationService;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.nio.file.Path;
import java.time.OffsetDateTime;
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
public class OutboundFileProcessorImpl implements OutboundFileProcessor {

    private final AudioOperationService audioOperationService;

    /**
     * Group the provided media/audio into logical groups in preparation for zipping with OutboundFileZipGenerator.
     *
     * @param mediaEntityToDownloadLocation Map relating each mediaEntity to the local filepath of its associated
     *                                      downloaded audio file.
     * @param overallStartTime              The time at which the audio start should be trimmed to.
     * @param overallEndTime                The time at which the audio end should be trimmed to.
     * @return A grouping of trimmed and concatenated multichannel audio files, whereby each group is a collection of
     *     audio files that belong to a continuous recording session.
     */
    @Override
    public List<List<AudioFileInfo>> processAudio(Map<MediaEntity, Path> mediaEntityToDownloadLocation,
                                                  OffsetDateTime overallStartTime,
                                                  OffsetDateTime overallEndTime)
        throws ExecutionException, InterruptedException {
        List<AudioFileInfo> audioFileInfos = mapToAudioFileInfos(mediaEntityToDownloadLocation);

        List<List<AudioFileInfo>> groupedAudioSessions = new ArrayList<>();
        for (AudioFileInfo audioFileInfo : audioFileInfos) {
            groupBySession(audioFileInfo, groupedAudioSessions);
        }

        List<List<AudioFileInfo>> finalisedAudioSessions = new ArrayList<>();
        for (List<AudioFileInfo> audioSession : groupedAudioSessions) {
            List<AudioFileInfo> concatenatedAudios = concatenateByChannel(audioSession);
            List<AudioFileInfo> trimmedAudios = trimToPeriod(
                concatenatedAudios,
                overallStartTime,
                overallEndTime
            );
            finalisedAudioSessions.add(trimmedAudios);
        }

        return finalisedAudioSessions;
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
            mediaEntity.getChannel()
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
            && (ungroupedAudioFileInfo.getStartTime().equals(groupedAudioFileInfo.getEndTime())
            || ungroupedAudioFileInfo.getEndTime().equals(groupedAudioFileInfo.getStartTime()));

        return hasEqualTimestamps || hasContinuity;
    }

    private List<AudioFileInfo> concatenateByChannel(List<AudioFileInfo> audioFileInfos)
        throws ExecutionException, InterruptedException {
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

    private List<AudioFileInfo> trimToPeriod(List<AudioFileInfo> audioFileInfos, OffsetDateTime start,
                                             OffsetDateTime end) throws ExecutionException, InterruptedException {
        List<AudioFileInfo> processedAudios = new ArrayList<>();
        for (AudioFileInfo audioFileInfo : audioFileInfos) {
            AudioFileInfo trimmedAudio = audioOperationService.trim(
                StringUtils.EMPTY,
                audioFileInfo,
                start.toLocalTime().toString(),
                end.toLocalTime().toString()
            );
            processedAudios.add(trimmedAudio);
        }

        return processedAudios;
    }

}
