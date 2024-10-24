package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HearingEntityTest {

    @Test
    void positiveContainsMedia() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMediaList(List.of(createMedia(1), createMedia(2), createMedia(3)));
        Assertions.assertTrue(hearing.containsMedia(createMedia(1)));
    }

    @Test
    void negativeContainsHearing() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMediaList(List.of(createMedia(1), createMedia(2), createMedia(3)));
        Assertions.assertFalse(hearing.containsMedia(createMedia(4)));

    }

    @Test
    void addMedia() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMediaList(new ArrayList<>(List.of(createMedia(1), createMedia(2), createMedia(3))));

        assertThat(hearing.getMediaList()).hasSize(3);
        List<MediaEntity> mediaEntities = hearing.getMediaList();
        assertThat(mediaEntities.get(0).getId()).isEqualTo(1);
        assertThat(mediaEntities.get(1).getId()).isEqualTo(2);
        assertThat(mediaEntities.get(2).getId()).isEqualTo(3);

        hearing.addMedia(createMedia(4));
        assertThat(hearing.getMediaList()).hasSize(4);

        mediaEntities = hearing.getMediaList();
        assertThat(mediaEntities.get(0).getId()).isEqualTo(1);
        assertThat(mediaEntities.get(1).getId()).isEqualTo(2);
        assertThat(mediaEntities.get(2).getId()).isEqualTo(3);
        assertThat(mediaEntities.get(3).getId()).isEqualTo(4);
    }

    private MediaEntity createMedia(int id) {
        MediaEntity media = new MediaEntity();
        media.setId(id);
        return media;
    }
}
