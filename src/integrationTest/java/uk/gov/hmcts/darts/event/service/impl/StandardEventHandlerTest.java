package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.stubs.NodeRegisterStub;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.event.enums.EventStatus.AUDIO_LINK_NOT_DONE_MODERNISED;

@SuppressWarnings({"PMD.DoNotUseThreads"})
class StandardEventHandlerTest extends HandlerTestData {

    @Autowired
    EventDispatcher eventDispatcher;

    @Autowired
    NodeRegisterStub nodeRegisterStub;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    public void setupStubs() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        CourtroomEntity courtroom = dartsDatabase.createCourtroomUnlessExists(SOME_COURTHOUSE, SOME_ROOM);
        nodeRegisterStub.setupNodeRegistry(courtroom);
        dartsGateway.darNotificationReturnsSuccess();
    }

    @Test
    void throwsOnUnknownCourthouse() {
        dartsDatabase.save(PersistableFactory.getCourtCaseTestData().someMinimalCase());
        DartsEvent dartsEvent = someMinimalDartsEvent().courthouse(UNKNOWN_COURTHOUSE);
        dartsEvent.setCaseNumbers(List.of("123"));
        dartsEvent.setDateTime(HEARING_DATE_ODT);
        assertThatThrownBy(() -> eventDispatcher.receive(dartsEvent))
            .isInstanceOf(DartsApiException.class);
    }

    @Test
    void givenStandardEventReceivedAndCourtCaseAndHearingDoesNotExist_thenNotifyDarUpdate() {
        dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE,
            SOME_ROOM,
            HEARING_DATE_ODT.toLocalDate()
        );

        var persistedEvent = dartsDatabase.getAllEvents().getFirst();

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedEvent.isLogEntry()).isEqualTo(false);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.getFirst().getHearingIsActual()).isEqualTo(true);
        assertThat(persistedEvent.getEventStatus()).isEqualTo(AUDIO_LINK_NOT_DONE_MODERNISED.getStatusNumber());

        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
    }

    @Test
    void givenStandardEventReceivedAndHearingDoesNotExist_thenNotifyDarUpdate() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM
        );

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().getFirst();

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedEvent.isLogEntry()).isEqualTo(false);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.getFirst().getHearingIsActual()).isEqualTo(true);

        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
    }

    @Test
    void givenStandardEventReceivedAndCaseAndHearingExistButRoomHasChanged_thenNotifyDarUpdate() {
        var caseEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM
        );

        CourtroomEntity otherCourtroom = dartsDatabase.givenTheCourtHouseHasRoom(caseEntity.getCourthouse(), SOME_OTHER_ROOM);
        nodeRegisterStub.setupNodeRegistry(otherCourtroom);

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_OTHER_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        var caseHearing = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_OTHER_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().getFirst();

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_OTHER_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedEvent.isLogEntry()).isEqualTo(false);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
        assertThat(caseHearing.size()).isEqualTo(1);
        assertThat(caseHearing.getFirst().getHearingIsActual()).isEqualTo(true);

        assertTrue(dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate()).isEmpty());

        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
    }

    @Test
    void givenStandardEventReceivedAndCaseAndHearingExistAndRoomHasNotChanged_thenDoNotNotifyDar() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM,
            HEARING_DATE
        );

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().getFirst();

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedEvent.isLogEntry()).isEqualTo(false);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.getFirst().getHearingIsActual()).isEqualTo(true);

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
    void testSummationWithConcurrency() throws InterruptedException, ExecutionException {

        dartsDatabase.createCourthouseUnlessExists(SOME_COURTHOUSE);
        dartsGateway.darNotificationReturnsSuccess();

        int numberOfThreads = 100;
        try (ExecutorService service = Executors.newFixedThreadPool(5)) {
            CountDownLatch latch = new CountDownLatch(numberOfThreads);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < numberOfThreads; i++) {
                int nanoSec = i * 1000;
                final int threadNum = i;
                Future<?> future = service.submit(() -> {
                    try {
                        DartsEvent dartsEvent = someMinimalDartsEvent()
                            .caseNumbers(List.of("asyncTestCaseNumber" + threadNum))
                            .courthouse(SOME_COURTHOUSE)
                            .courtroom("asyncTestCourtroom" + threadNum)
                            .dateTime(HEARING_DATE_ODT.withNano(nanoSec))
                            .eventId(null);
                        eventDispatcher.receive(dartsEvent);
                    } finally {
                        latch.countDown();
                    }
                });
                futures.add(future);
            }

            // Wait for all tasks to complete
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertTrue(completed, "Not all threads completed in time");

            // Wait for all futures to complete to ensure DB operations are done
            for (Future<?> future : futures) {
                future.get();
            }

            // Add a small delay to allow for any potential lag in DB updates
            Thread.sleep(100);

            assertEquals(numberOfThreads, dartsDatabase.getHearingRepository().findAll().size());
            assertEquals(numberOfThreads, dartsDatabase.getAllEvents().size(), "Expected all events to be processed");
        }
    }

    @Test
    void createsAnEventLinkedCaseForEachCaseNumberFromTheDartsEvent() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM
        );

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER, SOME_CASE_NUMBER_2))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedEvents = dartsDatabase.getEventRepository().findAll();
        var eventLinkedCases = dartsDatabase.getEventLinkedCaseRepository().findAll();

        assertThat(eventLinkedCases)
            .extracting("courtCase.caseNumber")
            .containsExactly(SOME_CASE_NUMBER, SOME_CASE_NUMBER_2);

        assertThat(eventLinkedCases)
            .extracting("event.id")
            .containsOnly(idFrom(persistedEvents));
    }

    @Test
    void createsAnEventLinkedCaseWhenCourtroomDoesntExist() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM
        );

        dartsDatabase.getNodeRegisterRepository().deleteAll();
        dartsDatabase.getCourtroomRepository()
            .findByCourthouseNameAndCourtroomName(SOME_COURTHOUSE, SOME_ROOM)
            .ifPresent(c -> dartsDatabase.getCourtroomRepository().delete(c));

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER, SOME_CASE_NUMBER_2))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedEvents = dartsDatabase.getEventRepository().findAll();
        var eventLinkedCases = dartsDatabase.getEventLinkedCaseRepository().findAll();

        assertThat(eventLinkedCases)
            .extracting("courtCase.caseNumber")
            .containsExactly(SOME_CASE_NUMBER, SOME_CASE_NUMBER_2);

        assertThat(eventLinkedCases)
            .extracting("event.id")
            .containsOnly(idFrom(persistedEvents));
    }

    private Integer idFrom(List<EventEntity> eventEntities) {
        return eventEntities.getFirst().getId();
    }
}