package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.repository.EventTypeRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ClearReportingRestrictionEventHandlerTest extends IntegrationBase {

    public static final String CLEAR_REPORTING_RESTRICTION_EVENT_HANDLER = "ClearReportingRestrictionEventHandler";
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_ROOM = "some-room";
    private static final String SOME_CASE_NUMBER = "1";
    private static final String CLEAR_REPORTING_RESTRICTION = "Restrictions lifted";
    private final OffsetDateTime today = now();

    @Autowired
    EventTypeRepository eventTypeRepository;

    @Autowired
    private ClearReportingRestrictionEventHandler clearReportingRestrictionEventHandler;

    @Test
    void handleClearReportingRestrictionEventHandler() {

        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM,
            today.toLocalDate());

        var persistedCaseAtStart = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE).get();

        var reportingRestrictionsEntity = getEventHandlerEntity();
        persistedCaseAtStart.setReportingRestrictions(reportingRestrictionsEntity);
        dartsDatabase.save(persistedCaseAtStart);

        var persistedCaseAfterSettingReportingRestrictions = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE).get();
        assertNotNull(persistedCaseAfterSettingReportingRestrictions.getReportingRestrictions());

        clearReportingRestrictionEventHandler.handle(someMinimalDartsEvent()
                                                         .caseNumbers(List.of(SOME_CASE_NUMBER))
                                                         .courthouse(SOME_COURTHOUSE)
                                                         .courtroom(SOME_ROOM)
                                                         .dateTime(today));

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_ROOM, today.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);
        var persistedCaseAfterHandlingEvent = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE).get();

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCaseAfterHandlingEvent.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);
        assertNull(persistedCaseAfterHandlingEvent.getReportingRestrictions());

    }

    private EventHandlerEntity getEventHandlerEntity() {
        return eventTypeRepository.findByHandler(CLEAR_REPORTING_RESTRICTION_EVENT_HANDLER).stream()
            .findFirst().get();
    }

    private DartsEvent someMinimalDartsEvent() {
        return new DartsEvent()
            .type("21201")
            .subType(null)
            .courtroom("known-room")
            .courthouse("known-courthouse")
            .eventId("192")
            .eventText(CLEAR_REPORTING_RESTRICTION)
            .messageId("some-message-id");
    }

}
