package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.stubs.NodeRegisterStub;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class InterpreterUsedHandlerTest extends HandlerTestData {

    private static final String INTERPRETER_USED_EVENT_TYPE = "2917";
    private static final String INTERPRETER_USED_EVENT_SUBTYPE = "3979";

    @Autowired
    private EventDispatcher eventDispatcher;

    @Autowired
    private CaseRepository caseRepository;

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
        DartsEvent event = someMinimalDartsEvent().courthouse(SOME_ROOM);
        event.setCaseNumbers(List.of("123"));
        event.setDateTime(HEARING_DATE_ODT);
        assertThatThrownBy(() -> eventDispatcher.receive(event))
            .isInstanceOf(DartsApiException.class);
    }

    @Test
    void givenInterpreterUsedEventReceivedAndCourtCaseAndHearingDoesNotExist_thenNotifyDarUpdate() {
        dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertThat(persistedCase.getInterpreterUsed()).isTrue();
    }

    @Test
    void givenInterpreterUsedEventReceivedAndHearingDoesNotExist_thenNotifyDarUpdate() {
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

        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertThat(persistedCase.getInterpreterUsed()).isTrue();
    }

    @Test
    void givenInterpreterUsedEventReceivedAndCaseAndHearingExistButRoomHasChanged_thenNotifyDarUpdate() {
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

        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        var caseHearing = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_OTHER_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_OTHER_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
        assertThat(caseHearing.size()).isEqualTo(1);
        assertThat(caseHearing.get(0).getHearingIsActual()).isEqualTo(true);

        assertTrue(dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate()).isEmpty());

        assertThat(persistedCase.getInterpreterUsed()).isTrue();
    }

    @Test
    void givenInterpreterUsedEventReceivedAndCaseAndHearingExistAndRoomHasNotChanged_thenDoNotNotifyDar() {
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

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
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

