package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithGatewayStub;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.someMinimalCase;

@SuppressWarnings({"PMD.DoNotUseThreads"})
class StandardEventHandlerTest extends IntegrationBaseWithGatewayStub {

    public static final String UNKNOWN_COURTHOUSE = "unknown-courthouse";
    public static final String SOME_COURTHOUSE = "some-courthouse";
    public static final String SOME_ROOM = "some-room";
    public static final String SOME_OTHER_ROOM = "some-other-room";
    public static final String SOME_CASE_NUMBER = "some-case-number";
    private final OffsetDateTime today = now();

    @Autowired
    EventDispatcher eventDispatcher;


    @MockBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    public void setupStubs() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void throwsOnUnknownCourthouse() {
        dartsDatabase.save(someMinimalCase());
        DartsEvent dartsEvent = someMinimalDartsEvent().courthouse(UNKNOWN_COURTHOUSE);
        dartsEvent.setCaseNumbers(List.of("123"));
        dartsEvent.setDateTime(today);
        assertThatThrownBy(() -> eventDispatcher.receive(dartsEvent))
                .isInstanceOf(DartsApiException.class);
    }

    @Test
    void handlesScenarioWhereCourtCaseAndHearingDontExist() {
        dartsDatabase.givenTheDatabaseContainsCourthouseWithRoom(
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
                SOME_COURTHOUSE,
                SOME_ROOM,
                today.toLocalDate()
        );

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedEvent.getIsLogEntry()).isEqualTo(false);
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
        assertThat(persistedEvent.getIsLogEntry()).isEqualTo(false);
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
        assertThat(persistedEvent.getIsLogEntry()).isEqualTo(false);
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
        assertThat(persistedEvent.getIsLogEntry()).isEqualTo(false);
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


    @Test
    void testSummationWithConcurrency() throws InterruptedException {

        dartsDatabase.createCourthouseUnlessExists(SOME_COURTHOUSE);
        dartsGateway.darNotificationReturnsSuccess();

        int numberOfThreads = 100;
        ExecutorService service = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);


        for (int i = 0; i < numberOfThreads; i++) {
            int nanoSec = i * 1000;
            service.submit(() -> {
                DartsEvent dartsEvent = someMinimalDartsEvent()
                        .caseNumbers(List.of("asyncTestCaseNumber"))
                        .courthouse(SOME_COURTHOUSE)
                        .courtroom("asyncTestCourtroom")
                        .dateTime(today.withNano(nanoSec))
                        .eventId(null);
                eventDispatcher.receive(dartsEvent);
                latch.countDown();
            });
        }
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(1, dartsDatabase.getHearingRepository().findAll().size());
        assertEquals(numberOfThreads, dartsDatabase.getAllEvents().size());
    }
}

