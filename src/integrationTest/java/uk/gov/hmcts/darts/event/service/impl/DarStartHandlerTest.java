package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DarNotifyEvent;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.stubs.NodeRegisterStub;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.event.enums.DarNotifyType.CASE_UPDATE;
import static uk.gov.hmcts.darts.event.enums.DarNotifyType.START_RECORDING;
import static uk.gov.hmcts.darts.test.common.data.CaseTestData.someMinimalCase;

class DarStartHandlerTest extends HandlerTestData {

    private static final String SOME_CLOSED_CASE_NUMBER = "CASE_CLOSED_1";
    private static final String HEARING_STARTED_EVENT_TYPE = "1100";
    private static final String HEARING_STARTED_EVENT_NAME = "Hearing started";
    private static final String DAR_START_HANDLER = "DarStartHandler";

    private final OffsetDateTime today = now();

    @Autowired
    private EventDispatcher eventDispatcher;

    @Autowired
    private NodeRegisterStub nodeRegisterStub;

    @MockBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    public void setupStubs() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        CourtroomEntity courtroom = dartsDatabase.createCourtroomUnlessExists(SOME_COURTHOUSE, SOME_ROOM);
        nodeRegisterStub.setupNodeRegistry(courtroom);
        doNothing().when(dartsGatewayClient).darNotify(any(DarNotifyEvent.class));
    }

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
    void givenDarStartRecordingEventReceivedAndCourtCaseAndHearingDoesNotExist_thenNotifyDarUpdateAndNotifyDarStartRecording() {
        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(SOME_CASE_NUMBER, SOME_COURTHOUSE).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        verifyDarNotificationCount(2);
        verifyDarNotifications(darNotifyEventArgumentCaptor.getAllValues(), List.of(START_RECORDING, CASE_UPDATE), SOME_ROOM);
    }

    @Test
    void givenDarStartRecordingEventReceivedAndHearingDoesNotExist_thenNotifyDarUpdateAndNotifyDarStartRecording() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(SOME_CASE_NUMBER, SOME_COURTHOUSE, SOME_ROOM);

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(SOME_CASE_NUMBER, SOME_COURTHOUSE).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        verifyDarNotificationCount(2);
        verifyDarNotifications(darNotifyEventArgumentCaptor.getAllValues(), List.of(START_RECORDING, CASE_UPDATE), SOME_ROOM);
    }


    @Test
    void givenDarStartRecordingEventReceivedAndCaseAndHearingExistButRoomHasChanged_thenNotifyDarUpdateAndNotifyDarStartRecording() {
        var caseEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(SOME_CASE_NUMBER, SOME_COURTHOUSE, SOME_ROOM);

        CourtroomEntity otherCourtroom = dartsDatabase.givenTheCourtHouseHasRoom(caseEntity.getCourthouse(), SOME_OTHER_ROOM);
        nodeRegisterStub.setupNodeRegistry(otherCourtroom);

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_OTHER_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(SOME_CASE_NUMBER, SOME_COURTHOUSE).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_OTHER_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_OTHER_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertTrue(dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate()).isEmpty());
        verifyDarNotificationCount(2);
        verifyDarNotifications(darNotifyEventArgumentCaptor.getAllValues(), List.of(START_RECORDING, CASE_UPDATE), SOME_OTHER_ROOM);
    }

    @Test
    void givenDarStartRecordingEventReceivedAndCaseAndHearingExistAndRoomHasNotChanged_thenNotifyDarStartRecording() {
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

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(SOME_CASE_NUMBER, SOME_COURTHOUSE).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        verifyDarNotificationCount(1);
        verifyDarNotification(darNotifyEventArgumentCaptor.getValue(), START_RECORDING, SOME_COURTHOUSE, SOME_ROOM);
    }

    @Test
    /*
    Should not Notify DAR PC when case is closed.
     */
    void shouldNotNotifyDarStartRecordingForHearingStartedCaseClosed() throws InterruptedException {
        dartsDatabase.createCourtroomUnlessExists(SOME_COURTHOUSE, SOME_ROOM);

        List<EventHandlerEntity> eventHandlerEntityList = dartsDatabase.findByHandlerAndActiveTrue(
            DAR_START_HANDLER);
        assertThat(eventHandlerEntityList.size()).isEqualTo(6);

        EventHandlerEntity hearingStartedEventHandler = eventHandlerEntityList.stream()
            .filter(eventHandlerEntity -> HEARING_STARTED_EVENT_NAME.equals(eventHandlerEntity.getEventName()))
            .findFirst()
            .orElseThrow();

        CourtCaseEntity createdCase = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CLOSED_CASE_NUMBER);
        createdCase.setClosed(true);
        dartsDatabase.getCaseRepository().saveAndFlush(createdCase);

        DartsEvent dartsEvent = someMinimalDartsEvent()
            .type(hearingStartedEventHandler.getType())
            .subType(hearingStartedEventHandler.getSubType())
            .caseNumbers(List.of(SOME_CLOSED_CASE_NUMBER))
            .dateTime(today);

        eventDispatcher.receive(dartsEvent);

        verifyDarNotificationNotReceived();
    }

    @Test
    /*
    Should not Notify DAR PC when case is closed. Same case number exists at another courthouse.
     */
    void shouldNotNotifyDarStartRecordingForHearingStartedCaseClosedOtherCourthouse() throws InterruptedException {
        dartsDatabase.createCourtroomUnlessExists(SOME_COURTHOUSE, SOME_ROOM);

        List<EventHandlerEntity> eventHandlerEntityList = dartsDatabase.findByHandlerAndActiveTrue(
            DAR_START_HANDLER);
        assertThat(eventHandlerEntityList.size()).isEqualTo(6);


        CourtCaseEntity createdCase = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CLOSED_CASE_NUMBER);
        createdCase.setClosed(true);
        dartsDatabase.getCaseRepository().saveAndFlush(createdCase);

        //create another case at a different courthouse, but same case number thats still open.
        dartsDatabase.createCase("another courthouse", SOME_CLOSED_CASE_NUMBER);

        EventHandlerEntity hearingStartedEventHandler = eventHandlerEntityList.stream()
            .filter(eventHandlerEntity -> HEARING_STARTED_EVENT_NAME.equals(eventHandlerEntity.getEventName()))
            .findFirst()
            .orElseThrow();

        DartsEvent dartsEvent = someMinimalDartsEvent()
            .type(hearingStartedEventHandler.getType())
            .subType(hearingStartedEventHandler.getSubType())
            .caseNumbers(List.of(SOME_CLOSED_CASE_NUMBER))
            .dateTime(today);

        eventDispatcher.receive(dartsEvent);

        verifyDarNotificationNotReceived();
    }

    private static DartsEvent someMinimalDartsEvent() {
        return new DartsEvent()
            .messageId("some-message-id")
            .type(HEARING_STARTED_EVENT_TYPE)
            .subType(null)
            .eventId("1")
            .courthouse(SOME_COURTHOUSE)
            .courtroom(SOME_ROOM)
            .eventText("some-text");
    }

}
