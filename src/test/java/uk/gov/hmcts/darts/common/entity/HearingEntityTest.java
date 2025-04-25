package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class HearingEntityTest {

    @Test
    void containsMedia_shouldReturnTrue_whenHearingHasMedia() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMedias(Set.of(createMedia(1), createMedia(2), createMedia(3)));
        Assertions.assertTrue(hearing.containsMedia(createMedia(1)));
    }

    @Test
    void containsMedia_shouldReturnFalse_whenHearingDoesNotHasMedia() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMedias(Set.of(createMedia(1), createMedia(2), createMedia(3)));
        Assertions.assertFalse(hearing.containsMedia(createMedia(4)));
    }

    @Test
    void containsMedia_shouldReturnFalse_whenMediaHasNullId() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMedias(Set.of(createMedia(1), createMedia(2), createMedia(3)));
        Assertions.assertFalse(hearing.containsMedia(createMedia(null)));
    }

    @Test
    void containsEvent_shouldReturnTrue_whenHearingHasEvent() {
        HearingEntity hearing = new HearingEntity();
        hearing.setEvents(Set.of(createEvent(1), createEvent(2), createEvent(3)));
        Assertions.assertTrue(hearing.containsEvent(createEvent(1)));
    }

    @Test
    void containsEvent_shouldReturnFalse_whenHearingDoesNotHasEvent() {
        HearingEntity hearing = new HearingEntity();
        hearing.setEvents(Set.of(createEvent(1), createEvent(2), createEvent(3)));
        Assertions.assertFalse(hearing.containsEvent(createEvent(4)));
    }

    @Test
    void containsEvent_shouldReturnFalse_whenEventEntityHasNullId() {
        HearingEntity hearing = new HearingEntity();
        hearing.setEvents(Set.of(createEvent(1), createEvent(2), createEvent(3)));
        Assertions.assertFalse(hearing.containsEvent(createEvent(null)));
    }


    @Test
    void addMedia_shouldAddToMedias_whenMediaDoesNotContainSameMediaId() {
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

    @Test
    void addMedia_shouldNotAddToMedias_whenMediaDoesContainSameMediaId() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMedias(new HashSet<>(List.of(createMedia(1), createMedia(2), createMedia(3))));
        assertThat(hearing.getMedias()).hasSize(3);
        assertThat(hearing.getMedias()
                       .stream()
                       .map(mediaEntity -> mediaEntity.getId())
                       .toList())
            .containsExactlyInAnyOrder(1, 2, 3);

        MediaEntity media = createMedia(1);
        hearing.addMedia(media);
        assertThat(hearing.getMedias()).hasSize(3);
        assertThat(hearing.getMedias()
                       .stream()
                       .map(mediaEntity -> mediaEntity.getId())
                       .toList())
            .containsExactlyInAnyOrder(1, 2, 3);
    }

    private MediaEntity createMedia(Long id) {
        MediaEntity media = new MediaEntity();
        media.setId(id);
        return media;
    }

    private EventEntity createEvent(Long id) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setId(id);
        return eventEntity;
    }
}
