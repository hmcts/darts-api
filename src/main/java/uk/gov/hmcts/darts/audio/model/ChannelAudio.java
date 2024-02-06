package uk.gov.hmcts.darts.audio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
public class ChannelAudio {

    Integer channel;
    List<AudioFileInfo> audioFiles;

    public ChannelAudio(Integer channel) {
        this.channel = channel;
    }

    public ChannelAudio(List<AudioFileInfo> audioFiles) {
        this.audioFiles = audioFiles;
    }

    public void addAudioFile(AudioFileInfo audioFileInfo) {
        if (audioFiles == null) {
            audioFiles = new ArrayList<>();
        }
        audioFiles.add(audioFileInfo);
    }
}
