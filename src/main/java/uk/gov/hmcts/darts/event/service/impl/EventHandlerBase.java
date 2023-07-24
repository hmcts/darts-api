package uk.gov.hmcts.darts.event.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.EventTypeRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.event.exception.EventError;
import uk.gov.hmcts.darts.event.model.CourtroomCourthouseCourtcase;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventHandler;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

@SuppressWarnings({"PMD.TooManyMethods"})
@Slf4j
public abstract class EventHandlerBase implements EventHandler {

    protected final Map<String, Pair<Integer, String>> eventTypesToIdAndName = new ConcurrentHashMap<>();

    @Autowired
    private EventTypeRepository eventTypeRepository;
    @Autowired
    private CourtroomRepository courtroomRepository;
    @Autowired
    private CaseRepository caseRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private HearingRepository hearingRepository;

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

    private EventHandlerEntity eventTypeReference(DartsEvent dartsEvent) {
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
        final var actualCourtRoomEntity = getCourtRoom(dartsEvent);

        final var caseNumbers = dartsEvent.getCaseNumbers();
        if (caseNumbers.size() > 1) {
            log.warn(format(MULTIPLE_CASE_NUMBERS, dartsEvent.getEventId(), join(", ", caseNumbers)));
        }
        final var actualCourtHouse = actualCourtRoomEntity.getCourthouse();
        final var actualEventCaseNumber = caseNumbers.get(0);
        final var actualEventDate = dartsEvent.getDateTime();

        var actualCourtCaseEntity = getCourtCaseEntityOrCreate(actualCourtHouse, actualEventCaseNumber);

        boolean hearingIsNew = false;
        HearingEntity actualHearingEntity;
        var optionalHearingEntity = getHearingEntity(actualEventDate, actualCourtCaseEntity);
        if (optionalHearingEntity.isPresent()) {
            actualHearingEntity = optionalHearingEntity.get();
        } else {
            actualHearingEntity = newCaseHearing(actualCourtRoomEntity, actualEventDate, actualCourtCaseEntity);
            hearingIsNew = true;
        }
        actualHearingEntity.setHearingIsActual(true);

        saveEvent(dartsEvent, actualCourtRoomEntity);

        boolean isCourtroomDifferentFromHearing = isCourtroomDifferentFromHearingCourtroom(actualCourtRoomEntity, actualHearingEntity);

        if (isTheHearingNewOrTheCourtroomIsDifferent(hearingIsNew, isCourtroomDifferentFromHearing)) {
            actualHearingEntity.setCourtroom(actualCourtRoomEntity);
        }

        caseRepository.save(actualCourtCaseEntity);
        hearingRepository.save(actualHearingEntity);

        return CourtroomCourthouseCourtcase.builder()
            .courthouseEntity(actualCourtHouse)
            .courtroomEntity(actualCourtRoomEntity)
            .caseEntity(actualCourtCaseEntity)
            .isHearingNew(hearingIsNew)
            .isCourtroomDifferentFromHearing(isCourtroomDifferentFromHearing)
            .build();
    }

    protected CourtroomEntity getCourtRoom(DartsEvent dartsEvent) {
        return courtroomRepository
            .findByCourthouseNameAndCourtroomName(dartsEvent.getCourthouse(), dartsEvent.getCourtroom())
            .orElseThrow(() -> new DartsApiException(
                EventError.EVENT_DATA_NOT_FOUND,
                format(COURTHOUSE_COURTROOM_NOT_FOUND_MESSAGE, dartsEvent.getCourthouse(), dartsEvent.getCourtroom())));

    }

    protected CaseEntity getCourtCaseEntityOrCreate(CourthouseEntity courtHouse, String eventCaseNumber) {
        return caseRepository
            .findByCaseNumberAndCourthouse_CourthouseName(eventCaseNumber, courtHouse.getCourthouseName())
            .orElseGet(() -> createNewCaseAt(courtHouse, eventCaseNumber));
    }

    protected void saveEvent(DartsEvent dartsEvent, CourtroomEntity courtroomEntity) {
        var eventEntity = eventEntityFrom(dartsEvent);
        eventEntity.setCourtroom(courtroomEntity);
        eventRepository.save(eventEntity);
    }

    protected boolean isTheHearingNewOrTheCourtroomIsDifferent(boolean hearingIsNew, boolean isCourtroomDifferentFromHearing) {
        return hearingIsNew || isCourtroomDifferentFromHearing;
    }

    protected static boolean isCourtroomDifferentFromHearingCourtroom(CourtroomEntity courtroomEntity, HearingEntity hearingEntity) {
        return !hearingEntity.getCourtroom().equals(courtroomEntity);
    }

    protected Optional<HearingEntity> getHearingEntity(OffsetDateTime eventDate, CaseEntity courtCaseEntity) {
        return courtCaseEntity.getHearings().stream()
            .filter(hearingEntity -> hearingEntity.isFor(eventDate))
            .findFirst();
    }

    protected HearingEntity getHearingEntityOrCreate(CourtroomEntity courtRoomEntity, OffsetDateTime eventDate, CaseEntity courtCaseEntity) {
        return courtCaseEntity.getHearings().stream()
            .filter(hearingEntity -> hearingEntity.isFor(eventDate))
            .findFirst().orElseGet(() -> newCaseHearing(courtRoomEntity, eventDate, courtCaseEntity));
    }

    protected static HearingEntity newCaseHearing(CourtroomEntity actualCourtRoom, OffsetDateTime actualEventDate, CaseEntity courtCase) {
        var newHearing = new HearingEntity();
        newHearing.setHearingDate(actualEventDate.toLocalDate());
        newHearing.setCourtroom(actualCourtRoom);
        newHearing.setCourtCase(courtCase);
        newHearing.setHearingIsActual(true);
        courtCase.addHearing(newHearing);
        return newHearing;
    }

    protected static CaseEntity createNewCaseAt(CourthouseEntity actualCourtHouse, String caseNumber) {
        var newCourtCase = new CaseEntity();
        newCourtCase.setCourthouse(actualCourtHouse);
        newCourtCase.setCaseNumber(caseNumber);
        return newCourtCase;
    }

}
