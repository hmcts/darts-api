package uk.gov.hmcts.darts.event.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.EventLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.EventLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import static uk.gov.hmcts.darts.event.enums.EventStatus.MODERNISED;

@Component
@RequiredArgsConstructor
public class EventPersistenceService {

    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final EventRepository eventRepository;
    private final EventLinkedCaseRepository eventLinkedCaseRepository;
    private final AuthorisationApi authorisationApi;


    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public EventEntity recordEvent(DartsEvent dartsEvent, EventHandlerEntity eventHandler, CourtroomEntity courtroomEntity) {
        var currentUser = authorisationApi.getCurrentUser();

        var eventEntity = saveEvent(dartsEvent, courtroomEntity, eventHandler, currentUser);

        dartsEvent.getCaseNumbers()
            .forEach(caseNumber -> createEventAndLinkToCaseNumber(dartsEvent, caseNumber, eventEntity));

        return eventEntity;
    }

    private void createEventAndLinkToCaseNumber(DartsEvent dartsEvent, String caseNumber, EventEntity eventEntity) {
        var courtCaseEntity = retrieveCoreObjectService.retrieveOrCreateCase(dartsEvent.getCourthouse(), caseNumber);

        var eventLinkedCase = new EventLinkedCaseEntity();
        eventLinkedCase.setEvent(eventEntity);
        eventLinkedCase.setCourtCase(courtCaseEntity);
        eventLinkedCaseRepository.save(eventLinkedCase);
    }

    private EventEntity saveEvent(DartsEvent dartsEvent, CourtroomEntity courtroom, EventHandlerEntity eventHandler, UserAccountEntity currentUser) {
        var eventEntity = eventEntityFrom(dartsEvent, eventHandler, currentUser);
        eventEntity.setCourtroom(courtroom);
        eventRepository.saveAndFlush(eventEntity);
        return eventEntity;
    }

    private EventEntity eventEntityFrom(DartsEvent dartsEvent, EventHandlerEntity eventHandler, UserAccountEntity currentUser) {
        var event = new EventEntity();
        event.setEventId(NumberUtils.createInteger(dartsEvent.getEventId()));
        event.setTimestamp(dartsEvent.getDateTime());
        event.setEventText(dartsEvent.getEventText());
        event.setEventType(eventHandler);
        event.setMessageId(dartsEvent.getMessageId());
        event.setIsLogEntry(dartsEvent.getIsMidTier());
        event.setCreatedBy(currentUser);
        event.setLastModifiedBy(currentUser);
        event.setIsCurrent(true);
        event.setEventStatus(MODERNISED.getStatusNumber());
        return event;
    }
}
