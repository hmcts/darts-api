package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.EventHandlerRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
public class EventStub {

    private final EventRepository eventRepository;
    private final EventHandlerRepository eventHandlerRepository;
    private final UserAccountStub userAccountStub;
    private final CourtroomStub courtroomStub;

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
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("testEventText");
        eventEntity.setEventName(eventName);
        EventHandlerEntity eventHandlerEntity = eventHandlerRepository.findById(eventHandlerId).get();
        eventEntity.setEventType(eventHandlerEntity);
        eventEntity.setTimestamp(eventTimestamp);
        eventEntity.setCreatedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        eventEntity.addHearing(hearing);
        eventEntity.setLastModifiedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        eventEntity.setLastModifiedDateTime(eventTimestamp);
        eventEntity.setIsLogEntry(false);
        eventEntity.setCourtroom(hearing.getCourtroom());
        eventRepository.saveAndFlush(eventEntity);
        return eventEntity;
    }

    @Transactional
    public EventEntity createEvent(CourtroomEntity courtroom, int eventHandlerId, OffsetDateTime eventTimestamp, String eventName) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("testEventText");
        eventEntity.setEventName(eventName);
        EventHandlerEntity eventHandlerEntity = eventHandlerRepository.findById(eventHandlerId).get();
        eventEntity.setEventType(eventHandlerEntity);
        eventEntity.setTimestamp(eventTimestamp);
        eventEntity.setCreatedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        eventEntity.setLastModifiedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        eventEntity.setLastModifiedDateTime(eventTimestamp);
        eventEntity.setIsLogEntry(false);
        eventEntity.setCourtroom(courtroom);
        eventRepository.saveAndFlush(eventEntity);
        return eventEntity;
    }

    public EventEntity createDefaultEvent() {
        CourtroomEntity courtroom = courtroomStub.createCourtroomUnlessExists("testCourthouse", "testCourtroom");
        return createEvent(courtroom, 10, OffsetDateTime.of(2020, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC),
              "testEventName"
        );
    }

}
