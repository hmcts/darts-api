package uk.gov.hmcts.darts.event.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.event.model.CreatedHearing;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventHandler;

import static java.lang.String.format;
import static java.lang.String.join;

@SuppressWarnings({"PMD.TooManyMethods"})
@Slf4j
public abstract class EventHandlerBase implements EventHandler {

    private static final String MULTIPLE_CASE_NUMBERS = "Event: %s contains multiple caseNumbers: %s";
    @Autowired
    protected EventTypeToHandlerMap handlerMap;
    @Autowired
    protected EventRepository eventRepository;
    @Autowired
    protected HearingRepository hearingRepository;
    @Autowired
    protected CaseRepository caseRepository;
    @Autowired
    private RetrieveCoreObjectService retrieveCoreObjectService;

    @Override
    public boolean isHandlerFor(DartsEvent event) {
        return handlerMap.hasMapping(event, this.getClass().getSimpleName());
    }

    protected EventEntity eventEntityFrom(DartsEvent dartsEvent) {
        var event = new EventEntity();
        event.setLegacyEventId(NumberUtils.createInteger(dartsEvent.getEventId()));
        event.setTimestamp(dartsEvent.getDateTime());
        event.setEventName(handlerMap.eventNameFor(dartsEvent));
        event.setEventText(dartsEvent.getEventText());
        event.setEventType(handlerMap.eventTypeReference(dartsEvent));
        event.setMessageId(dartsEvent.getMessageId());
        return event;
    }

    protected CreatedHearing createHearing(DartsEvent dartsEvent) {

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

        saveEvent(dartsEvent, hearingEntity.getCourtroom(), hearingEntity);
        setHearingToActive(hearingEntity);


        return CreatedHearing.builder()
            .hearingEntity(hearingEntity)
            .isHearingNew(hearingEntity.isNew())
            .isCourtroomDifferentFromHearing(false)//for now always creating a new one
            .build();
    }

    private void setHearingToActive(HearingEntity hearingEntity) {
        hearingEntity.setHearingIsActual(true);
        hearingRepository.saveAndFlush(hearingEntity);
    }

    protected EventEntity saveEvent(DartsEvent dartsEvent, CourtroomEntity courtroomEntity, HearingEntity hearingEntity) {
        var eventEntity = eventEntityFrom(dartsEvent);
        eventEntity.setCourtroom(courtroomEntity);
        eventRepository.saveAndFlush(eventEntity);
        eventEntity.addHearing(hearingEntity);
        eventRepository.saveAndFlush(eventEntity);
        return eventEntity;
    }

    protected boolean isTheHearingNewOrTheCourtroomIsDifferent(boolean hearingIsNew, boolean isCourtroomDifferentFromHearing) {
        return hearingIsNew || isCourtroomDifferentFromHearing;
    }
}
