package uk.gov.hmcts.darts.common.entity;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MediaEntityTest {

    @Test
    void getObjectAdminAction_shouldReturnEmptyOptional_whenNoAdminActionsAreSet() {
        MediaEntity media = PersistableFactory.getMediaTestData().someMinimalBuilder()
            .build()
            .getEntity();

        Optional<ObjectAdminActionEntity> adminActionOptional = media.getObjectAdminAction();
        assertTrue(adminActionOptional.isEmpty());
    }

    @Test
    void getObjectAdminAction_shouldReturnAdminAction_whenSingularAdminActionIsSet() {
        ObjectAdminActionEntity adminAction = PersistableFactory.getObjectAdminActionTestData().someMinimal();
        MediaEntity media = PersistableFactory.getMediaTestData().someMinimalBuilder()
            .objectAdminActions(Collections.singletonList(adminAction))
            .build()
            .getEntity();

        Optional<ObjectAdminActionEntity> adminActionOptional = media.getObjectAdminAction();
        assertTrue(adminActionOptional.isPresent());

        ObjectAdminActionEntity retrievedAdminAction = adminActionOptional.get();
        assertEquals(adminAction, retrievedAdminAction);
    }

    @Test
    void getObjectAdminAction_shouldReturnFirstAdminAction_whenMultipleAdminActionsAreSet() {
        ObjectAdminActionEntity firstAdminAction = PersistableFactory.getObjectAdminActionTestData().someMinimal();
        ObjectAdminActionEntity otherAdminAction = PersistableFactory.getObjectAdminActionTestData().someMinimal();

        MediaEntity media = PersistableFactory.getMediaTestData().someMinimalBuilder()
            .objectAdminActions(List.of(firstAdminAction, otherAdminAction))
            .build()
            .getEntity();

        Optional<ObjectAdminActionEntity> adminActionOptional = media.getObjectAdminAction();
        assertTrue(adminActionOptional.isPresent());

        ObjectAdminActionEntity retrievedAdminAction = adminActionOptional.get();
        assertEquals(firstAdminAction, retrievedAdminAction);
    }

    @Test
    void setObjectAdminAction_shouldSetSingularAdminAction_whenSomeAlreadyExist() {
        ObjectAdminActionEntity existingAdminAction = PersistableFactory.getObjectAdminActionTestData().someMinimal();
        ObjectAdminActionEntity newAdminAction = PersistableFactory.getObjectAdminActionTestData().someMinimal();

        List<ObjectAdminActionEntity> adminActions = new ArrayList<>();
        adminActions.add(existingAdminAction);

        MediaEntity media = PersistableFactory.getMediaTestData().someMinimalBuilder()
            .objectAdminActions(adminActions)
            .build()
            .getEntity();

        media.setObjectAdminAction(newAdminAction);

        assertEquals(1, media.getObjectAdminActions().size());
        assertEquals(newAdminAction, media.getObjectAdminActions().getFirst());
    }

    @Test
    void isCurrent_whenNull_shouldReturnFalse() {
        MediaEntity media = new MediaEntity();
        media.setIsCurrent(null);
        assertThat(media.isCurrent()).isFalse();
    }

    @Test
    void isCurrent_whenFalse_shouldReturnFalse() {
        MediaEntity media = new MediaEntity();
        media.setIsCurrent(false);
        assertThat(media.isCurrent()).isFalse();

    }

    @Test
    void isCurrent_whenTrue_shouldReturnTrue() {
        MediaEntity media = new MediaEntity();
        media.setIsCurrent(true);
        assertThat(media.isCurrent()).isTrue();

    }

    @Test
    void addHearing_shouldAddHearing_whenMediaDoesNotContainHearing() {
        MediaEntity media = new MediaEntity();
        HearingEntity hearing = mock(HearingEntity.class);
        assertThat(media.getHearings()).isEmpty();

        media.addHearing(hearing);
        assertThat(media.getHearings())
            .hasSize(1)
            .contains(hearing);
    }

    @Test
    void addHearing_shouldNotAddHearing_whenMediaAlreadyContainsHearing() {
        MediaEntity media = new MediaEntity();
        HearingEntity hearing = mock(HearingEntity.class);
        media.setHearings(Set.of(hearing));
        assertThat(media.getHearings())
            .hasSize(1)
            .contains(hearing);
        media.addHearing(hearing);
        assertThat(media.getHearings())
            .hasSize(1)
            .contains(hearing);
        verify(hearing, never()).addMedia(any());
    }

    @Test
    void removeHearing_shouldRemoveBidirectionalLink() {
        MediaEntity media = new MediaEntity();
        media.setId(1L);

        HearingEntity hearing = new HearingEntity();
        hearing.setId(101);

        // establish link on both sides
        hearing.setMedias(new HashSet<>());
        hearing.getMedias().add(media);

        media.setHearings(new HashSet<>());
        media.getHearings().add(hearing);

        // sanity check
        assertThat(hearing.getMedias()).contains(media);
        assertThat(media.getHearings()).contains(hearing);

        media.removeHearing(hearing);

        assertThat(hearing.getMedias()).doesNotContain(media);
        assertThat(media.getHearings()).doesNotContain(hearing);
    }

    @Test
    void removeHearing_shouldBeIdempotent_whenLinkDoesNotExist() {
        MediaEntity media = new MediaEntity();
        media.setId(1L);

        HearingEntity hearing = new HearingEntity();
        hearing.setId(101);

        hearing.setMedias(new HashSet<>());
        media.setHearings(new HashSet<>());

        media.removeHearing(hearing);

        assertThat(hearing.getMedias()).isEmpty();
        assertThat(media.getHearings()).isEmpty();
    }

    @Test
    void removeHearing_shouldRemoveFromHearingSide_evenIfMediaSideWasNotLinked() {
        MediaEntity media = new MediaEntity();
        media.setId(1L);

        HearingEntity hearing = new HearingEntity();
        hearing.setId(101);

        hearing.setMedias(new HashSet<>());
        hearing.getMedias().add(media);

        media.setHearings(new HashSet<>()); // no back-link

        media.removeHearing(hearing);

        assertThat(hearing.getMedias()).doesNotContain(media);
        assertThat(media.getHearings()).isEmpty();
    }

    @Test
    void removeHearing_shouldRemoveFromMediaSide_evenIfHearingSideWasNotLinked() {
        MediaEntity media = new MediaEntity();
        media.setId(1L);

        HearingEntity hearing = new HearingEntity();
        hearing.setId(101);

        hearing.setMedias(new HashSet<>()); // no link

        media.setHearings(new HashSet<>());
        media.getHearings().add(hearing);

        media.removeHearing(hearing);

        assertThat(hearing.getMedias()).isEmpty();
        assertThat(media.getHearings()).doesNotContain(hearing);
    }

}