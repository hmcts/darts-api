package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.event.exception.EventError;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;

import java.time.OffsetDateTime;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static uk.gov.hmcts.darts.event.enums.DarNotifyType.CASE_UPDATE;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class DefaultEventHandler extends EventHandlerBase {

    private static final String NOT_FOUND_MESSAGE = "Courthouse: %s & Courtroom: %s combination not found in database";
    private static final String MULTIPLE_CASE_NUMBERS = "Event: %s contains multiple caseNumbers: %s";
    private final CaseRepository caseRepository;
    private final EventRepository eventRepository;
    private final CourtroomRepository courtroomRepository;
    private final HearingRepository hearingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void handle(final DartsEvent dartsEvent) {
        final var actualCourtRoomEntity = courtroomRepository
              .findByCourthouseNameAndCourtroomName(dartsEvent.getCourthouse(), dartsEvent.getCourtroom())
              .orElseThrow(() -> new DartsApiException(
                    EventError.EVENT_DATA_NOT_FOUND,
                    format(NOT_FOUND_MESSAGE, dartsEvent.getCourthouse(), dartsEvent.getCourtroom())));

        final var caseNumbers = dartsEvent.getCaseNumbers();
        if (caseNumbers.size() > 1) {
            log.warn(format(MULTIPLE_CASE_NUMBERS, dartsEvent.getEventId(), join(", ", caseNumbers)));
        }
        final var actualCourtHouse = actualCourtRoomEntity.getCourthouse();
        final var actualEventCaseNumber = caseNumbers.get(0);
        final var actualEventDate = dartsEvent.getDateTime();

        var actualCourtCaseEntity = caseRepository
              .findByCaseNumberAndCourthouse_CourthouseName(actualEventCaseNumber, actualCourtHouse.getCourthouseName())
              .orElseGet(() -> createNewCaseAt(actualCourtHouse, actualEventCaseNumber));

        var actualHearingEntity = actualCourtCaseEntity.getHearings().stream()
              .filter(hearingEntity -> hearingEntity.isFor(actualEventDate))
              .findFirst().orElseGet(() -> newCaseHearing(actualCourtRoomEntity, actualEventDate, actualCourtCaseEntity));

        actualHearingEntity.setHearingIsActual(true);

        var eventEntity = eventEntityFrom(dartsEvent);
        eventEntity.setCourtroom(actualCourtRoomEntity);
        eventRepository.save(eventEntity);

        if (eitherTheHearingIsNewOrTheCourtroomIsDifferent(actualCourtRoomEntity, actualHearingEntity)) {
            actualHearingEntity.setCourtroom(actualCourtRoomEntity);
            var notifyEvent = new DarNotifyApplicationEvent(this, dartsEvent, CASE_UPDATE);
            eventPublisher.publishEvent(notifyEvent);
        }

        caseRepository.save(actualCourtCaseEntity);
        hearingRepository.save(actualHearingEntity);
    }

    private static boolean eitherTheHearingIsNewOrTheCourtroomIsDifferent(CourtroomEntity actualCourtRoom, HearingEntity actualHearing) {
        return isNull(actualHearing.getId()) || !actualHearing.getCourtroom().equals(actualCourtRoom);
    }

    private static HearingEntity newCaseHearing(CourtroomEntity actualCourtRoom, OffsetDateTime actualEventDate, CaseEntity courtCase) {
        var newHearing = new HearingEntity();
        newHearing.setHearingDate(actualEventDate.toLocalDate());
        newHearing.setCourtroom(actualCourtRoom);
        newHearing.setCourtCase(courtCase);
        newHearing.setHearingIsActual(true);
        courtCase.addHearing(newHearing);
        return newHearing;
    }

    private static CaseEntity createNewCaseAt(CourthouseEntity actualCourtHouse, String caseNumber) {
        var newCourtCase = new CaseEntity();
        newCourtCase.setCourthouse(actualCourtHouse);
        newCourtCase.setCaseNumber(caseNumber);
        return newCourtCase;
    }

}
