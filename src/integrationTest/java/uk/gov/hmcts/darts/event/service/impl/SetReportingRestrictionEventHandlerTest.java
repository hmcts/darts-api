package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class SetReportingRestrictionEventHandlerTest extends IntegrationBase {
    public static final String SOME_COURTHOUSE = "some-courthouse";
    public static final String SOME_ROOM = "some-room";
    public static final String SOME_CASE_NUMBER = "1";
    public static final String TEST_REPORTING_RESTRICTION = "Reporting Restriction Test";
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
    void handleSetReportingRestrictionEventHandler() {

        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
                SOME_CASE_NUMBER,
                SOME_COURTHOUSE,
                SOME_ROOM,
                today.toLocalDate()
        );

        eventDispatcher.receive(someMinimalDartsEvent()
                                        .caseNumbers(List.of(SOME_CASE_NUMBER))
                                        .courthouse(SOME_COURTHOUSE)
                                        .courtroom(SOME_ROOM)
                                        .dateTime(today));

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
                SOME_COURTHOUSE, SOME_ROOM, today.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);
        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
                SOME_CASE_NUMBER,
                SOME_COURTHOUSE
        ).get();

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);
        assertEquals(
                "Judge directed on reporting restrictions",
                persistedCase.getReportingRestrictions().getEventName()
        );
    }

    @Test
    void handleSetReportingRestrictionEventHandlerForClearRestrictionsEvent() {

        dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
                SOME_CASE_NUMBER,
                SOME_COURTHOUSE,
                SOME_ROOM,
                today.toLocalDate()
        );

        eventDispatcher.receive(clearReportingRestrictionsDartsEvent()
                                        .caseNumbers(List.of(SOME_CASE_NUMBER))
                                        .courthouse(SOME_COURTHOUSE)
                                        .courtroom(SOME_ROOM)
                                        .dateTime(today));

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
                SOME_COURTHOUSE, SOME_ROOM, today.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);
        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
                SOME_CASE_NUMBER,
                SOME_COURTHOUSE
        ).get();

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);
        assertEquals("Restrictions lifted", persistedCase.getReportingRestrictions().getEventName());
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

    private DartsEvent clearReportingRestrictionsDartsEvent() {
        return new DartsEvent()
                .type("21201")
                .subType(null)
                .courtroom("known-room")
                .courthouse("known-courthouse")
                .eventId("1")
                .eventText(TEST_REPORTING_RESTRICTION)
                .messageId("some-message-id");
    }
}
