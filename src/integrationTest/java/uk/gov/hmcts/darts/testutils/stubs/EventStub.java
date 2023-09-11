package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.EventEntity;
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


    public EventEntity createEvent(HearingEntity hearing) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("testEventText");
        eventEntity.setEventName("testEventName");
        eventEntity.setEventType(eventHandlerRepository.findById(10).get());
        eventEntity.setTimestamp(OffsetDateTime.of(2020, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        eventEntity.setCreatedBy(userAccountStub.getDefaultUser());
        eventEntity.addHearing(hearing);
        eventRepository.saveAndFlush(eventEntity);
        return eventEntity;
    }
}
