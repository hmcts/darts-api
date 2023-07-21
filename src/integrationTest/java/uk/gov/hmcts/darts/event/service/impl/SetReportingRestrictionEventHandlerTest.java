package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventTypeRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SetReportingRestrictionEventHandlerTest extends IntegrationBase {
    public static final String SOME_COURTHOUSE = "some-courthouse";
    public static final String SOME_ROOM = "some-room";
    public static final String SOME_CASE_NUMBER = "1";
    public static final String TEST_REPORTING_RESTRICTION = "Reporting Restriction Test";
    public static final String SET_REPORTING_RESTRICTION_EVENT_HANDLER = "SetReportingRestrictionEventHandler";
    private final OffsetDateTime today = now();

    @Autowired
    EventTypeRepository eventTypeRepository;

    @Autowired
    SetReportingRestrictionEventHandler setReportingRestrictionEventHandler;

    @Test
    void handleSetReportingRestrictionEventHandler() {

        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM,
            today.toLocalDate());

        setReportingRestrictionEventHandler.handle(someMinimalDartsEvent()
                                                       .caseNumbers(List.of(SOME_CASE_NUMBER))
                                                       .courthouse(SOME_COURTHOUSE)
                                                       .courtroom(SOME_ROOM)
                                                       .dateTime(today));

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_ROOM, today.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);
        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE).get();

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);
        assertNotNull(persistedCase.getReportingRestrictions());
    }

    @Test
    void throwsDartsApiExceptionOnUnknownReportingRestriction() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM,
            today.toLocalDate());

        assertThatThrownBy(() -> setReportingRestrictionEventHandler.handle(
            unknownReportingRestrictionDartsEvent()
                .caseNumbers(List.of(SOME_CASE_NUMBER))
                .courthouse(SOME_COURTHOUSE)
                .courtroom(SOME_ROOM)
                .dateTime(today)
        ))
            .isInstanceOf(DartsApiException.class);
    }

    private DartsEvent someMinimalDartsEvent() {
        return new DartsEvent()
            .type("2198")
            .subType("3933")
            .courtroom("known-room")
            .courthouse("known-courthouse")
            .eventId("1")
            .eventText(TEST_REPORTING_RESTRICTION)
            .messageId("some-message-id");
    }

    private DartsEvent unknownReportingRestrictionDartsEvent() {
        return new DartsEvent()
            .type("99999")
            .subType("3933")
            .courtroom("known-room")
            .courthouse("known-courthouse")
            .eventId("54")
            .eventText("1234")
            .messageId("some-message-id");
    }
}
