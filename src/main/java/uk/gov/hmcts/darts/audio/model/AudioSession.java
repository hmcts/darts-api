package uk.gov.hmcts.darts.audio.model;

import java.time.Instant;
import java.util.List;

public class AudioSession {
    private Instant startTime;
    private Instant endTime;

    private List<ChannelAudio> channelAudioList;
}
