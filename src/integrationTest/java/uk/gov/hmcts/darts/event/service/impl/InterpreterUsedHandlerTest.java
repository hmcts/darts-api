package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithGatewayStub;
import uk.gov.hmcts.darts.testutils.stubs.NodeRegisterStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class InterpreterUsedHandlerTest extends IntegrationBaseWithGatewayStub {

    public static final String SOME_COURTHOUSE = "some-courthouse";
    public static final String SOME_ROOM = "some-room";
    public static final String SOME_OTHER_ROOM = "some-other-room";
    public static final String SOME_CASE_NUMBER = "some-case-number";
    public static final String INTERPRETER_USED_EVENT_TYPE = "2917";
    public static final String INTERPRETER_USED_EVENT_SUBTYPE = "3979";
    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 23, 10, 0, 0);
    private static final OffsetDateTime HEARING_DATE_ODT = HEARING_DATE.atOffset(ZoneOffset.UTC);

    @Autowired
    private EventDispatcher eventDispatcher;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    NodeRegisterStub nodeRegisterStub;

    @MockBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    public void setupStubs() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        CourtroomEntity courtroom = dartsDatabase.createCourtroomUnlessExists(SOME_COURTHOUSE, SOME_ROOM);
        nodeRegisterStub.setupNodeRegistry(courtroom);
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
    void handlesScenarioWhereCourtCaseAndHearingDontExist() {
        dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        dartsGateway.darNotificationReturnsSuccess();

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

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
                                    .dateTime(HEARING_DATE_ODT));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

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

        CourtroomEntity otherCourtroom = dartsDatabase.givenTheCourtHouseHasRoom(caseEntity.getCourthouse(), SOME_OTHER_ROOM);
        nodeRegisterStub.setupNodeRegistry(otherCourtroom);
        dartsGateway.darNotificationReturnsSuccess();

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

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_OTHER_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(caseHearing.size()).isEqualTo(1);
        assertThat(caseHearing.get(0).getHearingIsActual()).isEqualTo(true);

        assertTrue(
            dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate()).isEmpty());

        assertThat(persistedCase.getInterpreterUsed()).isTrue();

        dartsGateway.verifyReceivedNotificationType(3);
    }

    @Test
    void handlesScenarioWhereCaseAndHearingExistsAndHearingLevelDataHasntChanged() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM,
            HEARING_DATE
        );
        dartsGateway.darNotificationReturnsSuccess();

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

