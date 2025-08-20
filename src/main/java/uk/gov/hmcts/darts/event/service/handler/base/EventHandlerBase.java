package uk.gov.hmcts.darts.event.service.handler.base;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.event.model.CreatedHearingAndEvent;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventHandler;
import uk.gov.hmcts.darts.event.service.EventPersistenceService;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.LocalDateTime;

import static java.lang.String.format;
import static java.lang.String.join;
import static uk.gov.hmcts.darts.event.enums.DarNotifyType.CASE_UPDATE;

@Slf4j
@AllArgsConstructor
public abstract class EventHandlerBase implements EventHandler {

    private static final String MULTIPLE_CASE_NUMBERS = "Event: %s contains multiple caseNumbers: %s";

    private RetrieveCoreObjectService retrieveCoreObjectService;
    protected EventRepository eventRepository;
    protected HearingRepository hearingRepository;
    protected CaseRepository caseRepository;
    protected ApplicationEventPublisher eventPublisher;
    private LogApi logApi;
    private EventPersistenceService eventPersistenceService;

    @Override
    public boolean isHandlerFor(String handlerName) {
        return StringUtils.equals(handlerName, this.getClass().getSimpleName());
    }

    protected CreatedHearingAndEvent createHearingAndSaveEvent(DartsEvent dartsEvent, EventHandlerEntity eventHandler) {

        try {

            var eventEntity = eventPersistenceService.recordEvent(dartsEvent, eventHandler);

            final var caseNumbers = dartsEvent.getCaseNumbers();
            if (caseNumbers.size() > 1) {
                log.warn(format(MULTIPLE_CASE_NUMBERS, dartsEvent.getEventId(), join(", ", caseNumbers)));
                // This needs fixing to deal with multiple case numbers https://tools.hmcts.net/jira/browse/DMP-2835
            }
            String caseNumber = caseNumbers.getFirst();

            LocalDateTime hearingDateTime = DateConverterUtil.toLocalDateTime(dartsEvent.getDateTime());
            HearingEntity hearingEntity = retrieveCoreObjectService.retrieveOrCreateHearing(
                dartsEvent.getCourthouse(),
                dartsEvent.getCourtroom(),
                caseNumber,
                hearingDateTime
            );

            eventEntity.addHearing(hearingEntity);
            eventRepository.saveAndFlush(eventEntity);
            setHearingToActive(hearingEntity);

            var createdHearingAndEvent = CreatedHearingAndEvent.builder()
                .hearingEntity(hearingEntity)
                .isHearingNew(hearingEntity.isNew())
                .isCourtroomDifferentFromHearing(false) // for now always creating a new one
                .eventEntity(eventEntity)
                .build();

            publishEventIfHearingNewOrCourtroomDifferent(dartsEvent, createdHearingAndEvent);

            return createdHearingAndEvent;
        } catch (DartsApiException dartsException) {
            if (dartsException.getError() == CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST) {
                logApi.missingCourthouse(dartsEvent);
            }
            throw dartsException;
        }
    }

    private void setHearingToActive(HearingEntity hearingEntity) {
        hearingEntity.setHearingIsActual(true);
        hearingRepository.saveAndFlush(hearingEntity);
    }

    private void publishEventIfHearingNewOrCourtroomDifferent(DartsEvent dartsEvent, CreatedHearingAndEvent createdHearingAndEvent) {
        if (isTheHearingNewOrTheCourtroomIsDifferent(
            createdHearingAndEvent.isHearingNew(),
            createdHearingAndEvent.isCourtroomDifferentFromHearing()
        )) {
            var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, CASE_UPDATE, createdHearingAndEvent.getCourtroomId());
            eventPublisher.publishEvent(notifyEvent);
        }
    }

    protected boolean isTheHearingNewOrTheCourtroomIsDifferent(boolean hearingIsNew, boolean isCourtroomDifferentFromHearing) {
        return hearingIsNew || isCourtroomDifferentFromHearing;
    }
}
