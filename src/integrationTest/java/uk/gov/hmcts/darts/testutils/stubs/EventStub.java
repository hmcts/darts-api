package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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

    public EventEntity createEvent(HearingEntity hearing) {
        return createEvent(hearing, 10);
    }

    public EventEntity createEvent(HearingEntity hearing, int eventHandlerId) {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventText("testEventText");
        eventEntity.setEventName("testEventName");
        EventHandlerEntity eventHandlerEntity = eventHandlerRepository.findById(eventHandlerId).get();
        eventEntity.setEventType(eventHandlerEntity);
        eventEntity.setTimestamp(OffsetDateTime.of(2020, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        eventEntity.setCreatedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        eventEntity.addHearing(hearing);
        eventEntity.setLastModifiedBy(userAccountStub.getIntegrationTestUserAccountEntity());
        eventEntity.setLastModifiedDateTime(OffsetDateTime.of(2020, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        eventRepository.saveAndFlush(eventEntity);
        return eventEntity;
    }

}
