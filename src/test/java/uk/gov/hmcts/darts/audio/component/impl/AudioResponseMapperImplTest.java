package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.audio.component.AudioResponseMapper;
import uk.gov.hmcts.darts.audio.model.AudioMetadata;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AudioResponseMapperImplTest {

    private static final OffsetDateTime START_TIME = OffsetDateTime.now();
    private static final OffsetDateTime END_TIME = START_TIME.plusHours(1);

    private AudioResponseMapper audioResponseMapper;

    @BeforeEach
    void setUp() {
        audioResponseMapper = new AudioResponseMapperImpl();
    }

    @Test
    void mapToAudioMetadataShouldMapToExpectedStructure() {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(1);
        mediaEntity.setStart(START_TIME);
        mediaEntity.setEnd(END_TIME);

        List<AudioMetadata> mappedAudioMetadatas = audioResponseMapper.mapToAudioMetadata(Collections.singletonList(mediaEntity));

        assertEquals(mappedAudioMetadatas.size(), 1);
        AudioMetadata mappedAudioMetaData = mappedAudioMetadatas.getFirst();

        assertEquals(1, mappedAudioMetaData.getId());
        assertEquals(START_TIME, mappedAudioMetaData.getMediaStartTimestamp());
        assertEquals(END_TIME, mappedAudioMetaData.getMediaEndTimestamp());
    }

}
