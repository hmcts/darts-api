package uk.gov.hmcts.darts.event.service.impl;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.testutils.stubs.NodeRegisterStub;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Slf4j
@Disabled
class SetReportingRestrictionEventHandlerTest extends HandlerTestData {
    public static final String TEST_REPORTING_RESTRICTION = "Reporting Restriction Test";

    @Autowired
    EventDispatcher eventDispatcher;

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
        dartsGateway.darNotificationReturnsSuccess();
    }

    @Test
    void givenSetReportingRestrictionEventReceivedAndCourtCaseAndHearingDoesNotExist_thenNotifyDarUpdate() {

        var result = WireMock.listAllStubMappings();
        log.info("mappings: {}", result.getMappings());

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
            SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertEquals(
            "Judge directed on reporting restrictions",
            persistedCase.getReportingRestrictions().getEventName()
        );

        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
    }

    @Test
    void givenSetReportingRestrictionEventReceivedAndHearingDoesNotExist_thenNotifyDarUpdate() {
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

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertEquals(
            "Judge directed on reporting restrictions",
            persistedCase.getReportingRestrictions().getEventName()
        );

        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
    }

    @Test
    void givenSetReportingRestrictionEventReceivedAndCaseAndHearingExistButRoomHasChanged_thenNotifyDarUpdate() {
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

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_OTHER_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(caseHearing.size()).isEqualTo(1);
        assertThat(caseHearing.get(0).getHearingIsActual()).isEqualTo(true);

        assertTrue(dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate()).isEmpty());

        assertEquals(
            "Judge directed on reporting restrictions",
            persistedCase.getReportingRestrictions().getEventName()
        );

        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
    }

    @Test
    void givenSetReportingRestrictionEventReceivedAndCaseAndHearingExistAndRoomHasNotChanged_thenDoNotNotifyDar() {
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

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertEquals(
            "Judge directed on reporting restrictions",
            persistedCase.getReportingRestrictions().getEventName()
        );

        dartsGateway.verifyDoesntReceiveDarEvent();
    }

    @Test
    void givenClearRestrictionsEventReceivedAndCaseAndHearingExistButRoomHasChanged_thenNotifyDarUpdateAndClearRestrictions() {

        dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM
        );

        eventDispatcher.receive(clearReportingRestrictionsDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

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

        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
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
