package uk.gov.hmcts.darts.event.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.IntStream.range;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.createEventForHearing;

@Component
public class AdminEventsSearchGivensBuilder {

    @Autowired
    private DartsDatabaseStub dartsDatabase;

    public List<EventEntity> persistedEvents(int quantity) {
        return range(0, quantity).mapToObj(i -> createEvent()).toList();
    }

    public List<EventEntity> persistedEventsWithHearings(int quantity, int hearingQuantity) {
        return range(0, quantity).mapToObj(i -> createEvent(hearingQuantity)).toList();
    }

    public List<EventEntity> persistedEventsForHearing(int quantity, HearingEntity hearing, boolean isEventCurrent) {
        return range(0, quantity).mapToObj(i -> createEventWithHearing(hearing, isEventCurrent)).toList();
    }

    public List<EventEntity> persistedEventsForCourtroom(int quantity, CourtroomEntity courtroom) {
        return range(0, quantity).mapToObj(i -> createEventWithCourtroom(courtroom)).toList();
    }

    public List<EventEntity> persistedEventsWithHearingDates(int quantity, LocalDate... hearingDates) {
        return range(0, quantity).mapToObj(i -> createEventForHearingOn(hearingDates[i])).toList();
    }

    private EventEntity createEventWithCourtroom(CourtroomEntity courtroom) {
        saveWithTransients(courtroom);
        var hearingEntity = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntity.setCourtroom(courtroom);
        hearingEntity.getCourtCase().setCourthouse(courtroom.getCourthouse());

        var eventForHearing = createEventForHearing(hearingEntity, true);
        dartsDatabase.saveEventsForHearing(hearingEntity, eventForHearing);

        return eventForHearing;
    }

    private EventEntity createEventWithHearing(HearingEntity hearingEntity, boolean isEventCurrent) {
        saveWithTransients(hearingEntity.getCourtroom());
        var eventForHearing = createEventForHearing(hearingEntity, isEventCurrent);
        dartsDatabase.saveEventsForHearing(hearingEntity, eventForHearing);
        return eventForHearing;
    }

    private EventEntity createEvent() {
        var hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        saveWithTransients(hearing.getCourtroom());
        var eventForHearing = createEventForHearing(hearing, true);

        dartsDatabase.saveEventsForHearing(hearing, eventForHearing);
        return eventForHearing;
    }

    private EventEntity createEvent(int hearingsForEvent) {
        EventEntity eventEntity = createEvent();

        for (int i = 0; i < hearingsForEvent - 1; i++) {
            var hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
            saveWithTransients(hearing.getCourtroom());
            dartsDatabase.saveEventsForHearing(hearing, eventEntity);
        }

        return eventEntity;
    }

    private EventEntity createEventForHearingOn(LocalDate date) {
        var hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearing.setHearingDate(date);
        saveWithTransients(hearing.getCourtroom());
        var eventForHearing = createEventForHearing(hearing, true);
        dartsDatabase.saveEventsForHearing(hearing, eventForHearing);
        return eventForHearing;
    }


    private void saveWithTransients(CourtroomEntity courtroom) {
        dartsDatabase.save(courtroom.getCourthouse());
        dartsDatabase.save(courtroom);
    }

}