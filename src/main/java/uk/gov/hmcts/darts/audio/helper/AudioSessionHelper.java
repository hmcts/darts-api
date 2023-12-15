package uk.gov.hmcts.darts.audio.helper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AudioSessionHelper {

    private static final int CONVERT_TO_SEC = 1000;

    public List<List<AudioFileInfo>> getSeparatedAudioFileInfo(List<AudioFileInfo> audioFileInfoList, int acceptableAudioGapSecs) {

        List<List<AudioFileInfo>> seperatedAudioFileInfoList = new ArrayList<>();

        Iterator<AudioFileInfo> firstAudioFileInfoIterator = audioFileInfoList.iterator();
        Iterator<AudioFileInfo> secondAudioFileInfoIterator = audioFileInfoList.iterator();

        AudioFileInfo firstAudioFileInfo;
        AudioFileInfo secondAudioFileInfo = getNextAudioFileInfo(secondAudioFileInfoIterator);

        while (firstAudioFileInfoIterator.hasNext()) {
            firstAudioFileInfo = firstAudioFileInfoIterator.next();
            secondAudioFileInfo = getNextAudioFileInfo(secondAudioFileInfoIterator);


            List<AudioFileInfo> concatenatedAudioFileInfoList = new ArrayList<>(Collections.singletonList(firstAudioFileInfo));

            boolean gapBetweenAudios = hasGapBetweenAudios(firstAudioFileInfo, secondAudioFileInfo, acceptableAudioGapSecs);
            while (!gapBetweenAudios) {
                concatenatedAudioFileInfoList.add(secondAudioFileInfo);
                firstAudioFileInfo = getNextAudioFileInfo(firstAudioFileInfoIterator);
                secondAudioFileInfo = getNextAudioFileInfo(secondAudioFileInfoIterator);
                gapBetweenAudios = hasGapBetweenAudios(firstAudioFileInfo, secondAudioFileInfo, acceptableAudioGapSecs);
            }
            seperatedAudioFileInfoList.add(concatenatedAudioFileInfoList);
        }

        return seperatedAudioFileInfoList;
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

    private AudioFileInfo getNextAudioFileInfo(Iterator<AudioFileInfo> audioFileInfoIterator) {
        if (audioFileInfoIterator.hasNext()) {
            return audioFileInfoIterator.next();
        } else {
            return null;
        }
    }

    private boolean hasGapBetweenAudios(AudioFileInfo audioFileInfoFirst, AudioFileInfo audioFileInfoNext,int acceptableAudioGapSecs) {
        boolean ret = false;
        if (audioFileInfoFirst == null || audioFileInfoNext == null) {
            ret = true;
        } else {
            long msEnd = audioFileInfoFirst.getEndTime().toEpochMilli();
            long msStart = audioFileInfoNext.getStartTime().toEpochMilli();
            long calcGap = (msStart - msEnd) / CONVERT_TO_SEC;
            if (calcGap > acceptableAudioGapSecs) {
                ret = true;
            }
        }
        return ret;
    }

}
