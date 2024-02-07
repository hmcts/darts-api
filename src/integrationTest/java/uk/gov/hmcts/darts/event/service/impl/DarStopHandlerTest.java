package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithGatewayStub;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.someMinimalCase;

class DarStopHandlerTest extends IntegrationBaseWithGatewayStub {

    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_ROOM = "some-room";
    private static final String SOME_CASE_NUMBER = "CASE1";
    private static final String HEARING_ENDED_EVENT_TYPE = "1200";
    private static final String HEARING_ENDED_EVENT_NAME = "Hearing ended";
    private static final String DAR_STOP_HANDLER = "DarStopHandler";
    private final OffsetDateTime today = now();

    @Autowired
    private EventDispatcher eventDispatcher;


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
        DartsEvent event = someMinimalDartsEvent().courthouse(SOME_ROOM);
        event.setCaseNumbers(List.of("123"));
        event.setDateTime(today);
        assertThatThrownBy(() -> eventDispatcher.receive(event))
                .isInstanceOf(DartsApiException.class);
    }

    @Test
    void shouldNotifyDarStopRecordingForHearingEnded() {
        dartsDatabase.createCourtroomUnlessExists(SOME_COURTHOUSE, SOME_ROOM);

        dartsGateway.darNotificationReturnsSuccess();

        List<EventHandlerEntity> eventHandlerEntityList = dartsDatabase.findByHandlerAndActiveTrue(
                DAR_STOP_HANDLER);
        assertThat(eventHandlerEntityList.size()).isEqualTo(5);

        EventHandlerEntity hearingEndedEventHandler = eventHandlerEntityList.stream()
                .filter(eventHandlerEntity -> HEARING_ENDED_EVENT_NAME.equals(eventHandlerEntity.getEventName()))
                .findFirst()
                .orElseThrow();

        DartsEvent dartsEvent = someMinimalDartsEvent()
                .type(hearingEndedEventHandler.getType())
                .subType(hearingEndedEventHandler.getSubType())
                .caseNumbers(List.of(SOME_CASE_NUMBER))
                .dateTime(today);

        eventDispatcher.receive(dartsEvent);

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

        assertThat(persistedCase.getClosed()).isFalse();
        assertThat(persistedCase.getCaseClosedTimestamp()).isNull();

        dartsGateway.verifyReceivedNotificationType(2);
    }

    private static DartsEvent someMinimalDartsEvent() {
        return new DartsEvent()
                .messageId("some-message-id")
                .type(HEARING_ENDED_EVENT_TYPE)
                .subType(null)
                .eventId("1")
                .courthouse(SOME_COURTHOUSE)
                .courtroom(SOME_ROOM)
                .eventText("some-text");
    }

}
