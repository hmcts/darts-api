package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.someMinimalCase;

class InterpreterUsedHandlerTest extends IntegrationBase {

    public static final String SOME_COURTHOUSE = "some-courthouse";
    public static final String SOME_ROOM = "some-room";
    public static final String SOME_OTHER_ROOM = "some-other-room";
    public static final String SOME_CASE_NUMBER = "some-case-number";
    public static final String INTERPRETER_USED_EVENT_TYPE = "2917";
    public static final String INTERPRETER_USED_EVENT_SUBTYPE = "3979";
    private final OffsetDateTime today = now();

    @Autowired
    private EventDispatcher eventDispatcher;

    @Autowired
    private CaseRepository caseRepository;

    @Test
    void throwsOnUnknownCourthouse() {
        dartsDatabase.save(someMinimalCase());
        DartsEvent event = someMinimalDartsEvent().courthouse(SOME_ROOM);
        event.setCaseNumbers(List.of("123"));
        event.setDateTime(today);
        assertThatThrownBy(() -> eventDispatcher.receive(event))
            .isInstanceOf(DartsApiException.class);
    }

    @Test
    void handlesScenarioWhereCourtCaseAndHearingDontExist() {
        dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        dartsGateway.darNotificationReturnsSuccess();

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(today));

        var courtCase =
            caseRepository.findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(
                SOME_CASE_NUMBER,
                SOME_COURTHOUSE
            );

        assertThat(courtCase.get().getInterpreterUsed()).isTrue();

        dartsGateway.verifyReceivedNotificationType(3);
    }

    @Test
    void handlesScenarioWhereHearingDoesntExist() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM
        );
        dartsGateway.darNotificationReturnsSuccess();

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(today));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_ROOM, today.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertThat(persistedCase.getInterpreterUsed()).isTrue();

        dartsGateway.verifyReceivedNotificationType(3);
    }

    @Test
    void handlesScenarioWhereCaseAndHearingExistsButRoomNumberHasChanged() {
        var caseEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM
        );

        dartsDatabase.givenTheCourtHouseHasRoom(caseEntity.getCourthouse(), SOME_OTHER_ROOM);
        dartsGateway.darNotificationReturnsSuccess();

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_OTHER_ROOM)
                                    .dateTime(today));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        var caseHearing = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_OTHER_ROOM, today.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_OTHER_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(caseHearing.size()).isEqualTo(1);
        assertThat(caseHearing.get(0).getHearingIsActual()).isEqualTo(true);

        assertTrue(
            dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, today.toLocalDate()).isEmpty());

        assertThat(persistedCase.getInterpreterUsed()).isTrue();

        dartsGateway.verifyReceivedNotificationType(3);
    }

    @Test
    void handlesScenarioWhereCaseAndHearingExistsAndHearingLevelDataHasntChanged() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM,
            today.toLocalDate()
        );
        dartsGateway.darNotificationReturnsSuccess();

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(today));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_ROOM, today.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertThat(persistedCase.getInterpreterUsed()).isTrue();

        dartsGateway.verifyDoesntReceiveDarEvent();
    }

    private static DartsEvent someMinimalDartsEvent() {
        return new DartsEvent()
            .type(INTERPRETER_USED_EVENT_TYPE)
            .subType(INTERPRETER_USED_EVENT_SUBTYPE)
            .courtroom(SOME_ROOM)
            .courthouse(SOME_COURTHOUSE)
            .eventId("1")
            .eventText("some-text")
            .messageId("some-message-id");
    }
}

