package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.darts.event.enums.EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED;

@Component
@RequiredArgsConstructor
@Deprecated
public class EventStub {

    private final EventRepository eventRepository;
    private final EventHandlerRepository eventHandlerRepository;
    private final UserAccountStub userAccountStub;
    private final CourtroomStub courtroomStub;
    private final UserAccountRepository userAccountRepository;
    private final DartsDatabaseSaveStub dartsDatabaseSaveStub;

    public static final OffsetDateTime STARTED_AT = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private HearingStub hearingStub;
    @Autowired
    private HearingRepository hearingRepository;

    public EventEntity createEvent(HearingEntity hearing) {
        return createEvent(hearing, 10);
    }

    public EventEntity createEvent(HearingEntity hearing, int eventHandlerId) {
        return createEvent(
            hearing,
            eventHandlerId,
            OffsetDateTime.of(2020, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC),
            "testEventName"
        );
    }

    @Transactional
    public EventEntity createEvent(HearingEntity hearing, int eventHandlerId, OffsetDateTime eventTimestamp, String eventName) {
        return createEvent(hearing, eventHandlerId, eventTimestamp, eventName, -1);
    }

    @Transactional
    public EventEntity createEvent(HearingEntity hearing, int eventHandlerId, OffsetDateTime eventTimestamp, String eventName, Integer eventId) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("testEventText");
        EventHandlerEntity eventHandlerEntity = eventHandlerRepository.findById(eventHandlerId).get();
        eventEntity.setEventType(eventHandlerEntity);
        eventEntity.setTimestamp(eventTimestamp);
        eventEntity.setCreatedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        eventEntity.addHearing(hearing);
        eventEntity.setLastModifiedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        eventEntity.setLastModifiedDateTime(eventTimestamp);
        eventEntity.setCreatedDateTime(eventTimestamp);
        eventEntity.setLogEntry(false);
        eventEntity.setCourtroom(hearing.getCourtroom());
        eventEntity.setIsCurrent(true);
        eventEntity.setEventStatus(AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber());
        eventEntity.setEventId(eventId);
        return dartsDatabaseSaveStub.save(eventEntity);
    }

    public EventEntity createEvent(CourtroomEntity courtroom, int eventHandlerId, OffsetDateTime eventTimestamp, String eventName) {
        return createEvent(courtroom, eventHandlerId, eventTimestamp, eventName, -1);
    }


    @Transactional
    public EventEntity createEvent(CourtroomEntity courtroom, int eventHandlerId, OffsetDateTime eventTimestamp, String eventName, Integer eventId) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("testEventText");
        EventHandlerEntity eventHandlerEntity = eventHandlerRepository.findById(eventHandlerId).get();
        eventEntity.setEventType(eventHandlerEntity);
        eventEntity.setTimestamp(eventTimestamp);
        eventEntity.setCreatedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        eventEntity.setLastModifiedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        eventEntity.setLastModifiedDateTime(eventTimestamp);
        eventEntity.setLogEntry(false);
        eventEntity.setCourtroom(courtroom);
        eventEntity.setIsCurrent(true);
        eventEntity.setEventStatus(AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber());
        eventEntity.setEventId(eventId);
        return dartsDatabaseSaveStub.save(eventEntity);
    }

    public EventEntity createDefaultEvent() {
        CourtroomEntity courtroom = courtroomStub.createCourtroomUnlessExists("testCourthouse", "testCourtroom", userAccountRepository.getReferenceById(0));
        return createEvent(courtroom, 10, OffsetDateTime.of(2020, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC),
                           "testEventName"
        );
    }

    /**
     * generates two event records with the same event id. So the number of events generated will
     * always be double that actually specified.
     * NOTE: This method also generates an event with a null event id and an event with an event id that only corresponds to one
     * event record
     *
     * @return The map that corresponds to the event ids and the events mapped to each event id
     */
    @Transactional
    public Map<Integer, List<EventEntity>> generateEventIdEventsIncludingZeroEventId(int numberOfEvents) {
        return generateEventIdEventsIncludingZeroEventId(numberOfEvents, 2, true);
    }

    @Transactional
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public Map<Integer, List<EventEntity>> generateEventIdEventsIncludingZeroEventId(int numberOfEvents,
                                                                                     int numberOfEventsPerEventId,
                                                                                     boolean includeNull) {
        Map<Integer, List<EventEntity>> eventIdMap = new HashMap<>();

        HearingEntity hearingForEvent = hearingStub.createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(STARTED_AT));

        // add a solitary event to prove this does not get processed
        if (includeNull) {
            EventEntity standAloneEventIdEvent = createEvent(hearingForEvent, 10, STARTED_AT.minusMinutes(20), "LOG", numberOfEvents + 1);
            eventIdMap.put(standAloneEventIdEvent.getEventId(), List.of(standAloneEventIdEvent));
        }
        HearingEntity hearingDifferentCourtroom = hearingStub.createHearing("Bristol", "2", "case2", DateConverterUtil.toLocalDateTime(STARTED_AT));

        for (int index = 0; index < numberOfEvents; index++) {
            List<EventEntity> eventEntities = new ArrayList<>(numberOfEventsPerEventId);
            for (int i = 0; i < numberOfEventsPerEventId; i++) {
                eventEntities.add(createEvent(hearingDifferentCourtroom, 10, STARTED_AT.minusMinutes(i), "LOG", index));
            }
            eventIdMap.put(index, eventEntities);
        }

        return eventIdMap;
    }

    public boolean isOnlyOneOfTheEventIdSetToCurrent(List<EventEntity> eventEntityList) {
        boolean currentFnd = false;
        for (EventEntity event : eventEntityList) {
            Optional<EventEntity> readEventEntity = eventRepository.findById(event.getId());

            if (readEventEntity.isPresent() && readEventEntity.get().getIsCurrent() && !currentFnd) {
                currentFnd = true;
            } else if (readEventEntity.isPresent() && readEventEntity.get().getIsCurrent()) {
                return false;
            }
        }

        return currentFnd;
    }

    public boolean hasOnlyOneOfTheEventIdsGotHearings(List<EventEntity> eventEntityList) {
        int numWithHearings = 0;
        for (EventEntity event : eventEntityList) {
            Optional<EventEntity> readEventEntity = eventRepository.findById(event.getId());

            if (readEventEntity.isPresent() && !hearingRepository.findHearingIdsByEventId(readEventEntity.get().getId()).isEmpty()) {
                numWithHearings++;
            }
        }

        return numWithHearings == 1;
    }
}