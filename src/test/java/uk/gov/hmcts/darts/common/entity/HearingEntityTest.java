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
        hearing.setMedias(Set.of(createMedia(1L), createMedia(2L), createMedia(3L)));
        Assertions.assertTrue(hearing.containsMedia(createMedia(1L)));
    }

    @Test
    void containsMedia_shouldReturnFalse_whenHearingDoesNotHasMedia() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMedias(Set.of(createMedia(1L), createMedia(2L), createMedia(3L)));
        Assertions.assertFalse(hearing.containsMedia(createMedia(4L)));
    }

    @Test
    void containsMedia_shouldReturnFalse_whenMediaHasNullId() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMedias(Set.of(createMedia(1L), createMedia(2L), createMedia(3L)));
        Assertions.assertFalse(hearing.containsMedia(createMedia(null)));
    }

    @Test
    void containsEvent_shouldReturnTrue_whenHearingHasEvent() {
        HearingEntity hearing = new HearingEntity();
        hearing.setEvents(Set.of(createEvent(1L), createEvent(2L), createEvent(3L)));
        Assertions.assertTrue(hearing.containsEvent(createEvent(1L)));
    }

    @Test
    void containsEvent_shouldReturnFalse_whenHearingDoesNotHasEvent() {
        HearingEntity hearing = new HearingEntity();
        hearing.setEvents(Set.of(createEvent(1L), createEvent(2L), createEvent(3L)));
        Assertions.assertFalse(hearing.containsEvent(createEvent(4L)));
    }

    @Test
    void containsEvent_shouldReturnFalse_whenEventEntityHasNullId() {
        HearingEntity hearing = new HearingEntity();
        hearing.setEvents(Set.of(createEvent(1L), createEvent(2L), createEvent(3L)));
        Assertions.assertFalse(hearing.containsEvent(createEvent(null)));
    }


    @Test
    void addMedia_shouldAddToMedias_whenMediaDoesNotContainSameMediaId() {
        HearingEntity hearing = new HearingEntity();
        hearing.setMedias(new HashSet<>(List.of(createMedia(1L), createMedia(2L), createMedia(3L))));
        assertThat(hearing.getMedias()).hasSize(3);
        assertThat(hearing.getMedias()
                       .stream()
                       .map(mediaEntity -> mediaEntity.getId())
                       .toList())
            .containsExactlyInAnyOrder(1L, 2L, 3L);

        MediaEntity media = createMedia(4L);
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
        hearing.setMedias(new HashSet<>(List.of(createMedia(1L), createMedia(2L), createMedia(3L))));
        assertThat(hearing.getMedias()).hasSize(3);
        assertThat(hearing.getMedias()
                       .stream()
                       .map(mediaEntity -> mediaEntity.getId())
                       .toList())
            .containsExactlyInAnyOrder(1L, 2L, 3L);

        MediaEntity media = createMedia(1L);
        hearing.addMedia(media);
        assertThat(hearing.getMedias()).hasSize(3);
        assertThat(hearing.getMedias()
                       .stream()
                       .map(mediaEntity -> mediaEntity.getId())
                       .toList())
            .containsExactlyInAnyOrder(1L, 2L, 3L);
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
