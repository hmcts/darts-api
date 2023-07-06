package uk.gov.hmcts.darts.event.handlers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.impl.DefaultEventHandler;
import uk.gov.hmcts.darts.event.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.event.testutils.DartsDatabaseStub.minimalCaseEntity;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureWireMock(port = 8070)
@SuppressWarnings("PMD.TooManyMethods")
class DefaultEventHandlerTest extends IntegrationBase {

    public static final String UNKNOWN_COURTROOM = "unknown-courtroom";
    public static final String UNKNOWN_COURTHOUSE = "unknown-courthouse";
    public static final String SOME_COURTHOUSE = "some-courthouse";
    public static final String SOME_ROOM = "some-room";
    public static final String SOME_OTHER_ROOM = "some-other-room";
    public static final String SOME_CASE_NUMBER = "some-case-number";
    private final OffsetDateTime today = now();

    @Autowired
    DefaultEventHandler eventHandler;

    @Test
    void throwsOnUnknownCourtroom() {
        dartsDatabase.save(minimalCaseEntity());
        assertThatThrownBy(() -> eventHandler.handle(someMinimalDartsEvent().courtroom(UNKNOWN_COURTROOM)))
              .isInstanceOf(DartsApiException.class);
    }

    @Test
    void throwsOnUnknownCourthouse() {
        dartsDatabase.save(minimalCaseEntity());
        assertThatThrownBy(() -> eventHandler.handle(someMinimalDartsEvent().courthouse(UNKNOWN_COURTHOUSE)))
              .isInstanceOf(DartsApiException.class);
    }

    @Test
    void handlesScenarioWhereCourtCaseAndHearingDontExist() {
        dartsDatabase.givenTheDatabaseContainsCourthouseWithRoom(
              SOME_COURTHOUSE,
              SOME_ROOM);
        dartsGateway.darNotifyIsUp();

        eventHandler.handle(someMinimalDartsEvent()
              .caseNumbers(List.of(SOME_CASE_NUMBER))
              .courthouse(SOME_COURTHOUSE)
              .courtroom(SOME_ROOM)
              .dateTime(today));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
              SOME_CASE_NUMBER,
              SOME_COURTHOUSE).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
              SOME_COURTHOUSE,
              SOME_ROOM,
              today.toLocalDate());

        var persistedEvent = dartsDatabase.findAll().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        dartsGateway.verifyReceivedNotificationType(3);
    }

    @Test
    void handlesScenarioWhereHearingDoesntExist() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
              SOME_CASE_NUMBER,
              SOME_COURTHOUSE,
              SOME_ROOM);
        dartsGateway.darNotifyIsUp();

        eventHandler.handle(someMinimalDartsEvent()
              .caseNumbers(List.of(SOME_CASE_NUMBER))
              .courthouse(SOME_COURTHOUSE)
              .courtroom(SOME_ROOM)
              .dateTime(today));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
              SOME_CASE_NUMBER,
              SOME_COURTHOUSE).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
              SOME_COURTHOUSE, SOME_ROOM, today.toLocalDate());

        var persistedEvent = dartsDatabase.findAll().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        dartsGateway.verifyReceivedNotificationType(3);
    }

    @Test
    void handlesScenarioWhereCaseAndHearingExistsButRoomNumberHasChanged() {
        var caseEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
              SOME_CASE_NUMBER,
              SOME_COURTHOUSE,
              SOME_ROOM);

        dartsDatabase.givenTheCourtHouseHasRoom(caseEntity.getCourthouse(), SOME_OTHER_ROOM);
        dartsGateway.darNotifyIsUp();

        eventHandler.handle(someMinimalDartsEvent()
              .caseNumbers(List.of(SOME_CASE_NUMBER))
              .courthouse(SOME_COURTHOUSE)
              .courtroom(SOME_OTHER_ROOM)
              .dateTime(today));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
              SOME_CASE_NUMBER,
              SOME_COURTHOUSE).get();

        var caseHearing = dartsDatabase.findByCourthouseCourtroomAndDate(
              SOME_COURTHOUSE, SOME_OTHER_ROOM, today.toLocalDate());

        var persistedEvent = dartsDatabase.findAll().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_OTHER_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(caseHearing.size()).isEqualTo(1);
        assertThat(caseHearing.get(0).getHearingIsActual()).isEqualTo(true);

        assertTrue(
              dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, today.toLocalDate()).isEmpty());

        dartsGateway.verifyReceivedNotificationType(3);
    }

    @Test
    void handlesScenarioWhereCaseAndHearingExistsAndHearingLevelDataHasntChanged() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
              SOME_CASE_NUMBER,
              SOME_COURTHOUSE,
              SOME_ROOM,
              today.toLocalDate());
        dartsGateway.darNotifyIsUp();

        eventHandler.handle(someMinimalDartsEvent()
              .caseNumbers(List.of(SOME_CASE_NUMBER))
              .courthouse(SOME_COURTHOUSE)
              .courtroom(SOME_ROOM)
              .dateTime(today));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
              SOME_CASE_NUMBER,
              SOME_COURTHOUSE).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
              SOME_COURTHOUSE, SOME_ROOM, today.toLocalDate());

        var persistedEvent = dartsDatabase.findAll().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        dartsGateway.verifyDoesntReceiveDarEvent();
    }

    private static DartsEvent someMinimalDartsEvent() {
        return new DartsEvent()
              .type("1000")
              .subType("1002")
              .courtroom("unknown-room")
              .courthouse("known-courthouse")
              .eventId("1")
              .eventText("some-text")
              .messageId("some-message-id");
    }
}

