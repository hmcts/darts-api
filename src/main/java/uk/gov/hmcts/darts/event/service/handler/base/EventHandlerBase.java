package uk.gov.hmcts.darts.event.service.handler.base;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.ApplicationEventPublisher;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.CreatedHearingAndEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventHandler;

import static java.lang.String.format;
import static java.lang.String.join;

@SuppressWarnings({"PMD.TooManyMethods"})
@Slf4j
@AllArgsConstructor
public abstract class EventHandlerBase implements EventHandler {

    private static final String MULTIPLE_CASE_NUMBERS = "Event: %s contains multiple caseNumbers: %s";

    private RetrieveCoreObjectService retrieveCoreObjectService;
    protected EventRepository eventRepository;
    protected HearingRepository hearingRepository;
    protected CaseRepository caseRepository;
    protected ApplicationEventPublisher eventPublisher;
    protected AuthorisationApi authorisationApi;

    @Override
    public boolean isHandlerFor(String handlerName) {
        return StringUtils.equals(handlerName, this.getClass().getSimpleName());
    }

    protected EventEntity eventEntityFrom(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {
        UserAccountEntity currentUser = authorisationApi.getCurrentUser();
        var event = new EventEntity();
        event.setLegacyEventId(NumberUtils.createInteger(dartsEvent.getEventId()));
        event.setTimestamp(dartsEvent.getDateTime());
        event.setEventName(eventHandler.getEventName());
        event.setEventText(dartsEvent.getEventText());
        event.setEventType(eventHandler);
        event.setMessageId(dartsEvent.getMessageId());
        event.setIsLogEntry(dartsEvent.getIsMidTier());
        event.setCreatedBy(currentUser);
        event.setLastModifiedBy(currentUser);
        return event;
    }

    protected CreatedHearingAndEvent createHearingAndSaveEvent(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {

        final var caseNumbers = dartsEvent.getCaseNumbers();
        if (caseNumbers.size() > 1) {
            log.warn(format(MULTIPLE_CASE_NUMBERS, dartsEvent.getEventId(), join(", ", caseNumbers)));
        }

        String caseNumber = caseNumbers.get(0);
        HearingEntity hearingEntity = retrieveCoreObjectService.retrieveOrCreateHearing(
            dartsEvent.getCourthouse(),
            dartsEvent.getCourtroom(),
            caseNumber,
            dartsEvent.getDateTime().toLocalDate()
        );

        EventEntity eventEntity = saveEvent(dartsEvent, hearingEntity, eventHandler);
        setHearingToActive(hearingEntity);

        return CreatedHearingAndEvent.builder()
            .hearingEntity(hearingEntity)
            .isHearingNew(hearingEntity.isNew())
            .isCourtroomDifferentFromHearing(false)//for now always creating a new one
            .eventEntity(eventEntity)
            .build();
    }

    private void setHearingToActive(HearingEntity hearingEntity) {
        hearingEntity.setHearingIsActual(true);
        hearingRepository.saveAndFlush(hearingEntity);
    }

    protected EventEntity saveEvent(DartsEvent dartsEvent, HearingEntity hearingEntity, EventHandlerEntity eventHandler) {
        var eventEntity = eventEntityFrom(dartsEvent, eventHandler);
        eventEntity.setCourtroom(hearingEntity.getCourtroom());
        eventRepository.saveAndFlush(eventEntity);
        eventEntity.addHearing(hearingEntity);
        eventRepository.saveAndFlush(eventEntity);
        return eventEntity;
    }

    protected boolean isTheHearingNewOrTheCourtroomIsDifferent(boolean hearingIsNew, boolean isCourtroomDifferentFromHearing) {
        return hearingIsNew || isCourtroomDifferentFromHearing;
    }
}
