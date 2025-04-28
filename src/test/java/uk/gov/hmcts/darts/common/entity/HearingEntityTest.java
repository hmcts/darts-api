package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HearingEntityTest {

    @Test
    void positiveContainsMedia() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMedias(Set.of(createMedia(1), createMedia(2), createMedia(3)));
        Assertions.assertTrue(hearing.containsMedia(createMedia(1)));
    }

    @Test
    void negativeContainsHearing() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMedias(Set.of(createMedia(1), createMedia(2), createMedia(3)));
        Assertions.assertFalse(hearing.containsMedia(createMedia(4)));

    }

    @Test
    void addMedia() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMedias(new HashSet<>(List.of(createMedia(1), createMedia(2), createMedia(3))));

        assertThat(hearing.getMedias()).hasSize(3);
        assertThat(hearing.getMedias()
                       .stream()
                       .map(mediaEntity -> mediaEntity.getId())
                       .toList())
            .containsExactlyInAnyOrder(1L, 2L, 3L);

        MediaEntity media = createMedia(4);
        hearing.addMedia(media);
        assertThat(hearing.getMedias()).hasSize(4);

        assertThat(hearing.getMedias()
                       .stream()
                       .map(mediaEntity -> mediaEntity.getId())
                       .toList())
            .containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    }

    private MediaEntity createMedia(long id) {
        MediaEntity media = new MediaEntity();
        media.setId(id);
        return media;
    }
}
