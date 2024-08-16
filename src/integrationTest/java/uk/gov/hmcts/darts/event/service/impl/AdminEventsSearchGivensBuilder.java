package uk.gov.hmcts.darts.event.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.IntStream.range;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.createEventForHearing;
import static uk.gov.hmcts.darts.test.common.data.HearingTestData.createSomeMinimalHearing;

@Component
public class AdminEventsSearchGivensBuilder {

    @Autowired
    private DartsDatabaseStub dartsDatabase;

    public List<EventEntity> persistedEvents(int quantity) {
        return range(0, quantity).mapToObj(i -> createEvent()).toList();
    }

    public List<EventEntity> persistedEventsForHearing(int quantity, HearingEntity hearing) {
        return range(0, quantity).mapToObj(i -> createEventWithHearing(hearing)).toList();
    }

    public List<EventEntity> persistedEventsForCourtroom(int quantity, CourtroomEntity courtroom) {
        return range(0, quantity).mapToObj(i -> createEventWithCourtroom(courtroom)).toList();
    }

    public List<EventEntity> persistedEventsWithHearingDates(int quantity, LocalDate... hearingDates) {
        return range(0, quantity).mapToObj(i -> createEventForHearingOn(hearingDates[i])).toList();
    }

    private EventEntity createEventWithCourtroom(CourtroomEntity courtroom) {
        dartsDatabase.save(courtroom);
        var hearingEntity = createSomeMinimalHearing();
        hearingEntity.setCourtroom(courtroom);
        hearingEntity.getCourtCase().setCourthouse(courtroom.getCourthouse());

        var eventForHearing = createEventForHearing(hearingEntity);
        dartsDatabase.saveEventsForHearing(hearingEntity, eventForHearing);

        return eventForHearing;
    }

    private EventEntity createEventWithHearing(HearingEntity hearingEntity) {
        dartsDatabase.save(hearingEntity.getCourtroom());
        var eventForHearing = createEventForHearing(hearingEntity);
        dartsDatabase.saveEventsForHearing(hearingEntity, eventForHearing);
        return eventForHearing;
    }

    private EventEntity createEvent() {
        var hearing = createSomeMinimalHearing();
        dartsDatabase.save(hearing.getCourtroom());
        var eventForHearing = createEventForHearing(hearing);
        dartsDatabase.saveEventsForHearing(hearing, eventForHearing);
        return eventForHearing;
    }

    private EventEntity createEventForHearingOn(LocalDate date) {
        var hearing = createSomeMinimalHearing();
        hearing.setHearingDate(date);
        dartsDatabase.save(hearing.getCourtroom());
        var eventForHearing = createEventForHearing(hearing);
        dartsDatabase.saveEventsForHearing(hearing, eventForHearing);
        return eventForHearing;
    }

}
