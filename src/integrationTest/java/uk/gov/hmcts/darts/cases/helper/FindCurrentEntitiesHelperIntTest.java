package uk.gov.hmcts.darts.cases.helper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FindCurrentEntitiesHelperIntTest extends IntegrationBase {

    @Autowired
    private DartsPersistence dartsPersistence;
    @Autowired
    private FindCurrentEntitiesHelper findCurrentEntitiesHelper;

    @Test
    void getCurrentEvents_shouldReturnCurrentEvents_whenCurrentAndNonCurrentEventsExist() {

        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsPersistence.save(hearing);
        EventEntity eventEntity1 = PersistableFactory.getEventTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearingEntities(Set.of(hearing))
            .build()
            .getEntity();

        EventEntity eventEntity2 = PersistableFactory.getEventTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearingEntities(Set.of(hearing))
            .build()
            .getEntity();

        EventEntity eventEntity3 = PersistableFactory.getEventTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearingEntities(Set.of(hearing))
            .isCurrent(false)
            .build()
            .getEntity();

        dartsPersistence.saveAll(eventEntity1, eventEntity2, eventEntity3);

        dartsPersistence.save(hearing);

        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearings(List.of(hearing))
            .build()
            .getEntity();

        List<EventEntity> currentEvents = findCurrentEntitiesHelper.getCurrentEvents(courtCaseEntity);
        assertThat(currentEvents)
            .extracting(EventEntity::getId)
            .containsExactlyInAnyOrder(eventEntity1.getId(), eventEntity2.getId())
            .doesNotContain(eventEntity3.getId());
    }

    @Test
    void getCurrentMedia_shouldReturnCurrentMedia_whenCurrentAndNonCurrentMediaExists() {

        CourtroomEntity existingCourtroom = PersistableFactory.getCourtroomTestData().someMinimalBuilderHolder().getBuilder()
            .courthouse(PersistableFactory.getCourthouseTestData().someMinimal())
            .build()
            .getEntity();
        dartsPersistence.save(existingCourtroom);

        MediaEntity mediaEntity1 = PersistableFactory.getMediaTestData().someMinimalBuilderHolder()
            .getBuilder()
            .courtroom(existingCourtroom)
            .mediaFile("test")
            .isCurrent(true)
            .build()
            .getEntity();

        dartsPersistence.save(mediaEntity1);
        mediaEntity1.setChronicleId(mediaEntity1.getId().toString());
        dartsPersistence.save(mediaEntity1);

        MediaEntity mediaEntity2 = PersistableFactory.getMediaTestData().someMinimalBuilderHolder()
            .getBuilder()
            .courtroom(existingCourtroom)
            .mediaFile("test")
            .isCurrent(true)
            .build()
            .getEntity();

        dartsPersistence.save(mediaEntity2);
        mediaEntity2.setChronicleId(mediaEntity2.getId().toString());
        dartsPersistence.save(mediaEntity2);

        MediaEntity mediaEntity3 = PersistableFactory.getMediaTestData().someMinimalBuilderHolder()
            .getBuilder()
            .courtroom(existingCourtroom)
            .mediaFile("test")
            .isCurrent(false)
            .build()
            .getEntity();

        dartsPersistence.save(mediaEntity3);
        mediaEntity3.setChronicleId(mediaEntity3.getId().toString());
        dartsPersistence.save(mediaEntity3);

        HearingEntity hearing = PersistableFactory.getHearingTestData().hearingWith(
            "1078",
            "a_courthouse",
            "1",
            LocalDateTime.now().minusYears(7).toString()
        );
        hearing.addMedia(mediaEntity1);
        hearing.addMedia(mediaEntity2);
        hearing.addMedia(mediaEntity3);
        dartsPersistence.save(hearing);

        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearings(List.of(hearing))
            .build()
            .getEntity();

        List<MediaEntity> currentMedia = findCurrentEntitiesHelper.getCurrentMedia(courtCaseEntity);
        assertThat(currentMedia)
            .extracting(MediaEntity::getId)
            .containsExactlyInAnyOrder(mediaEntity1.getId(), mediaEntity2.getId())
            .doesNotContain(mediaEntity3.getId());
    }

    @Test
    void getCurrentNonLogEvents_shouldReturnCurrentNonLogEvents_whenCurrentLogAndNonCurrentEventsExist() {

        HearingEntity hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsPersistence.save(hearing);

        EventEntity currentNonLogEvent1 = createEventForHearing(hearing, true, false);
        EventEntity currentNonLogEvent2 = createEventForHearing(hearing, true, false);
        EventEntity currentLogEvent = createEventForHearing(hearing, true, true);
        EventEntity nonCurrentNonLogEvent = createEventForHearing(hearing, false, false);
        dartsPersistence.saveAll(currentNonLogEvent1, currentNonLogEvent2, currentLogEvent, nonCurrentNonLogEvent);

        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearings(List.of(hearing))
            .build()
            .getEntity();

        List<EventEntity> currentNonLogEvents = findCurrentEntitiesHelper.getCurrentNonLogEvents(courtCaseEntity);
        assertThat(currentNonLogEvents)
            .extracting(EventEntity::getId)
            .containsExactlyInAnyOrder(currentNonLogEvent1.getId(), currentNonLogEvent2.getId())
            .doesNotContain(currentLogEvent.getId(), nonCurrentNonLogEvent.getId());
    }

    @Test
    void getCurrentNonLogEvents_shouldReturnCurrentNonLogEventsAcrossAllCaseHearings() {

        HearingEntity hearing1 = PersistableFactory.getHearingTestData().someMinimalHearing();
        HearingEntity hearing2 = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsPersistence.save(hearing1);
        dartsPersistence.save(hearing2);

        EventEntity currentNonLogEventForHearing1 = createEventForHearing(hearing1, true, false);
        EventEntity currentNonLogEventForHearing2 = createEventForHearing(hearing2, true, false);
        dartsPersistence.saveAll(currentNonLogEventForHearing1, currentNonLogEventForHearing2);

        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearings(List.of(hearing1, hearing2))
            .build()
            .getEntity();

        List<EventEntity> currentNonLogEvents = findCurrentEntitiesHelper.getCurrentNonLogEvents(courtCaseEntity);
        assertThat(currentNonLogEvents)
            .extracting(EventEntity::getId)
            .containsExactlyInAnyOrder(currentNonLogEventForHearing1.getId(), currentNonLogEventForHearing2.getId());
    }

    private EventEntity createEventForHearing(HearingEntity hearing, boolean isCurrent, boolean isLogEntry) {
        EventEntity eventEntity = PersistableFactory.getEventTestData().someMinimalBuilderHolder()
            .getBuilder()
            .hearingEntities(Set.of(hearing))
            .isCurrent(isCurrent)
            .isLogEntry(isLogEntry)
            .build()
            .getEntity();
        eventEntity.setCourtroom(hearing.getCourtroom());
        return eventEntity;
    }
}
