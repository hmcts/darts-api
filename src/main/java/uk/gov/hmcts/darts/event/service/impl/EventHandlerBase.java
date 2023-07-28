package uk.gov.hmcts.darts.event.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.api.CommonApi;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.EventTypeRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.event.model.CourtroomCourthouseCourtcase;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

@SuppressWarnings({"PMD.TooManyMethods"})
@Slf4j
public abstract class EventHandlerBase implements EventHandler {

    protected final Map<String, Pair<Integer, String>> eventTypesToIdAndName = new ConcurrentHashMap<>();

    @Getter
    @Autowired
    private EventTypeRepository eventTypeRepository;
    @Autowired
    private CourtroomRepository courtroomRepository;
    @Getter
    @Autowired
    private CaseRepository caseRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private HearingRepository hearingRepository;

    @Autowired
    private CommonApi commonApi;

    private static final String COURTHOUSE_COURTROOM_NOT_FOUND_MESSAGE = "Courthouse: %s & Courtroom: %s combination not found in database";
    private static final String MULTIPLE_CASE_NUMBERS = "Event: %s contains multiple caseNumbers: %s";

    @PostConstruct
    public void populateMessageTypes() {
        eventTypeRepository.findByHandler(this.getClass().getSimpleName())
            .forEach(eventType -> {
                var key = buildKey(eventType.getType(), eventType.getSubType());
                eventTypesToIdAndName.put(key, Pair.of(eventType.getId(), eventType.getEventName()));
            });
    }

    @Override
    public boolean isHandlerFor(String type, String subType) {
        var key = buildKey(type, subType);
        return eventTypesToIdAndName.containsKey(key);
    }

    protected EventEntity eventEntityFrom(DartsEvent dartsEvent) {
        var event = new EventEntity();
        event.setLegacyEventId(NumberUtils.createInteger(dartsEvent.getEventId()));
        event.setTimestamp(dartsEvent.getDateTime());
        event.setEventName(eventNameFor(dartsEvent));
        event.setEventText(dartsEvent.getEventText());
        event.setEventType(eventTypeReference(dartsEvent));
        event.setMessageId(dartsEvent.getMessageId());
        return event;
    }

    protected EventHandlerEntity eventTypeReference(DartsEvent dartsEvent) {
        var key = buildKey(dartsEvent.getType(), dartsEvent.getSubType());
        return eventTypeRepository.getReferenceById(eventTypesToIdAndName.get(key).getLeft());
    }

    private String eventNameFor(DartsEvent dartsEvent) {
        return this.eventTypesToIdAndName.get(buildKey(dartsEvent)).getRight();
    }

    protected String buildKey(DartsEvent dartsEvent) {
        return this.buildKey(dartsEvent.getType(), dartsEvent.getSubType());
    }

    protected String buildKey(String type, String subType) {
        requireNonNull(type);
        return type + (isNull(subType) ? "" : subType);
    }

    protected CourtroomCourthouseCourtcase getOrCreateCourtroomCourtHouseAndCases(DartsEvent dartsEvent) {

        final var caseNumbers = dartsEvent.getCaseNumbers();
        if (caseNumbers.size() > 1) {
            log.warn(format(MULTIPLE_CASE_NUMBERS, dartsEvent.getEventId(), join(", ", caseNumbers)));
        }

        String caseNumber = caseNumbers.get(0);
        HearingEntity hearingEntity = commonApi.retrieveOrCreateHearing(
            dartsEvent.getCourthouse(),
            dartsEvent.getCourtroom(),
            caseNumber,
            dartsEvent.getDateTime().toLocalDate()
        );

        EventEntity eventEntity = saveEvent(dartsEvent, hearingEntity.getCourtroom());
        eventEntity.addHearing(hearingEntity);
        eventRepository.saveAndFlush(eventEntity);
        hearingEntity.setHearingIsActual(true);
        hearingRepository.saveAndFlush(hearingEntity);


        return CourtroomCourthouseCourtcase.builder()
            .courthouseEntity(hearingEntity.getCourtroom().getCourthouse())
            .courtroomEntity(hearingEntity.getCourtroom())
            .courtCaseEntity(hearingEntity.getCourtCase())
            .isHearingNew(hearingEntity.isNew())
            .isCourtroomDifferentFromHearing(false)//for now always creating a new one
            .build();
    }

    protected EventEntity saveEvent(DartsEvent dartsEvent, CourtroomEntity courtroomEntity) {
        var eventEntity = eventEntityFrom(dartsEvent);
        eventEntity.setCourtroom(courtroomEntity);
        eventRepository.saveAndFlush(eventEntity);
        return eventEntity;
    }

    protected boolean isTheHearingNewOrTheCourtroomIsDifferent(boolean hearingIsNew, boolean isCourtroomDifferentFromHearing) {
        return hearingIsNew || isCourtroomDifferentFromHearing;
    }


}
