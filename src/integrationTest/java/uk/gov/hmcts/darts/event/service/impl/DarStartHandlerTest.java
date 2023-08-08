package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.someMinimalCase;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.createCourthouseWithRoom;

class DarStartHandlerTest extends IntegrationBase {

    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_ROOM = "some-room";
    private static final String SOME_CASE_NUMBER = "CASE1";
    private static final String HEARING_STARTED_EVENT_TYPE = "1100";
    private static final String HEARING_STARTED_EVENT_NAME = "Hearing started";
    private static final String DAR_START_HANDLER = "DarStartHandler";

    private final OffsetDateTime today = now();

    @Autowired
    private DarStartHandler darStartHandler;

    @Test
    void throwsOnUnknownCourthouse() {
        dartsDatabase.save(someMinimalCase());
        DartsEvent event = someMinimalDartsEvent().courthouse(SOME_ROOM);
        event.setCaseNumbers(List.of("123"));
        event.setDateTime(today);
        assertThatThrownBy(() -> darStartHandler.handle(event))
            .isInstanceOf(DartsApiException.class);
    }

    @Test
    void shouldNotifyDarStartRecordingForHearingStarted() {
        var courthouseEntity = createCourthouseWithRoom(SOME_COURTHOUSE, SOME_ROOM);
        dartsDatabase.save(courthouseEntity);

        dartsGateway.darNotificationReturnsSuccess();

        List<EventHandlerEntity> eventHandlerEntityList = dartsDatabase.findByHandlerAndActiveTrue(
            DAR_START_HANDLER);
        assertThat(eventHandlerEntityList.size()).isEqualTo(6);

        EventHandlerEntity hearingStartedEventHandler = eventHandlerEntityList.stream()
            .filter(eventHandlerEntity -> HEARING_STARTED_EVENT_NAME.equals(eventHandlerEntity.getEventName()))
            .findFirst()
            .orElseThrow();

        DartsEvent dartsEvent = someMinimalDartsEvent()
            .type(hearingStartedEventHandler.getType())
            .subType(hearingStartedEventHandler.getSubType())
            .caseNumbers(List.of(SOME_CASE_NUMBER))
            .dateTime(today);

        darStartHandler.handle(dartsEvent);

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

        assertThat(persistedCase.getClosed()).isNull();
        assertThat(persistedCase.getCaseClosedTimestamp()).isNull();

        dartsGateway.verifyReceivedNotificationType(1);
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
