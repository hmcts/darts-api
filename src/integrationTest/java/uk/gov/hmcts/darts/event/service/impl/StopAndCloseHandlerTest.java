package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.someMinimalCase;

class StopAndCloseHandlerTest extends IntegrationBase {

    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_ROOM = "some-room";
    private static final String SOME_CASE_NUMBER = "CASE1";
    private static final String HEARING_ENDED_EVENT_TYPE = "30500";
    private static final String HEARING_ENDED_EVENT_NAME = "Hearing ended";
    private static final String STOP_AND_CLOSE_HANDLER = "StopAndCloseHandler";
    private final OffsetDateTime today = now();

    @Autowired
    private EventDispatcher eventDispatcher;

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
    void shouldNotifyDarStopRecordingForHearingEndedAndCaseClosedFlagAndDate() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        assertNull(courtCaseEntity.getClosed());
        assertNull(courtCaseEntity.getCaseClosedTimestamp());

        dartsGateway.darNotificationReturnsSuccess();

        List<EventHandlerEntity> eventHandlerEntityList = dartsDatabase.findByHandlerAndActiveTrue(
            STOP_AND_CLOSE_HANDLER);
        assertEquals(3, eventHandlerEntityList.size());

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

        assertThat(persistedCase.getClosed()).isTrue();
        assertThat(persistedCase.getCaseClosedTimestamp()
                       .isAfter(OffsetDateTime.parse("2023-08-08T00:00:00.000Z"))).isTrue();

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
