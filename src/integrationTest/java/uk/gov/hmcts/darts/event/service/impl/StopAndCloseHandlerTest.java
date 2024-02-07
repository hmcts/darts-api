package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithGatewayStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.someMinimalCase;

class StopAndCloseHandlerTest extends IntegrationBaseWithGatewayStub {

    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_ROOM = "some-room";
    private static final String SOME_CASE_NUMBER = "CASE1";
    private static final String ARCHIVE_CASE_EVENT_TYPE = "3000";
    private static final String ARCHIVE_CASE_EVENT_NAME = "Archive Case";
    private static final String STOP_AND_CLOSE_HANDLER = "StopAndCloseHandler";
    private final OffsetDateTime testTime = OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private EventDispatcher eventDispatcher;

    @MockBean
    private CurrentTimeHelper currentTimeHelper;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Mock
    private CaseRetentionRepository caseRetentionRepository;

    @Test
    void throwsOnUnknownCourthouse() {
        dartsDatabase.save(someMinimalCase());
        DartsEvent event = someMinimalDartsEvent().courthouse(SOME_ROOM);
        event.setCaseNumbers(List.of("123"));
        event.setDateTime(testTime);
        assertThatThrownBy(() -> eventDispatcher.receive(event))
                .isInstanceOf(DartsApiException.class);
    }

    @BeforeEach
    public void setupStubs() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.of(2024, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void shouldNotifyDarStopRecordingForHearingEndedAndCaseClosedFlagAndDate() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        assertFalse(courtCaseEntity.getClosed());
        assertNull(courtCaseEntity.getCaseClosedTimestamp());

        dartsGateway.darNotificationReturnsSuccess();

        List<EventHandlerEntity> eventHandlerEntityList = dartsDatabase.findByHandlerAndActiveTrue(
                STOP_AND_CLOSE_HANDLER);
        assertEquals(2, eventHandlerEntityList.size());

        EventHandlerEntity hearingEndedEventHandler = eventHandlerEntityList.stream()
                .filter(eventHandlerEntity -> ARCHIVE_CASE_EVENT_NAME.equals(eventHandlerEntity.getEventName()))
                .findFirst()
                .orElseThrow();

        DartsEvent dartsEvent = someMinimalDartsEvent()
                .type(hearingEndedEventHandler.getType())
                .subType(hearingEndedEventHandler.getSubType())
                .caseNumbers(List.of(SOME_CASE_NUMBER))
                .dateTime(testTime);

        eventDispatcher.receive(dartsEvent);

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
                SOME_CASE_NUMBER,
                SOME_COURTHOUSE
        ).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
                SOME_COURTHOUSE, SOME_ROOM, testTime.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM);
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE);
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertThat(persistedCase.getClosed()).isTrue();
        assertEquals(testTime, persistedCase.getCaseClosedTimestamp());

        dartsGateway.verifyReceivedNotificationType(2);
    }

    @Test
    void shouldCreateNewCaseRetentionWhenNoneExist() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        assertFalse(courtCaseEntity.getClosed());
        assertNull(courtCaseEntity.getCaseClosedTimestamp());

        dartsGateway.darNotificationReturnsSuccess();

        List<EventHandlerEntity> eventHandlerEntityList = dartsDatabase.findByHandlerAndActiveTrue(
                STOP_AND_CLOSE_HANDLER);

        EventHandlerEntity hearingEndedEventHandler = eventHandlerEntityList.stream()
                .filter(eventHandlerEntity -> ARCHIVE_CASE_EVENT_NAME.equals(eventHandlerEntity.getEventName()))
                .findFirst()
                .orElseThrow();

        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.caseRetentionFixedPolicy("3");
        retentionPolicy.setCaseTotalSentence("20Y3M4D");

        DartsEvent dartsEvent = someMinimalDartsEvent()
                .type(hearingEndedEventHandler.getType())
                .subType(hearingEndedEventHandler.getSubType())
                .caseNumbers(List.of(SOME_CASE_NUMBER))
                .dateTime(testTime)
                .retentionPolicy(retentionPolicy);

        eventDispatcher.receive(dartsEvent);

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
                SOME_CASE_NUMBER,
                SOME_COURTHOUSE
        ).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
                SOME_COURTHOUSE, SOME_ROOM, testTime.toLocalDate());

        var hearing = hearingsForCase.get(0);

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        List<EventEntity> eventsForHearing = dartsDatabase.getEventRepository().findAllByHearingId(hearing.getId());
        assertEquals(1, eventsForHearing.size());

        List<CaseRetentionEntity> caseRetentionEntities = dartsDatabase.getCaseRetentionRepository().findByCaseId(persistedCase.getId());
        assertEquals(1, caseRetentionEntities.size());
        CaseRetentionEntity caseRetentionEntity = caseRetentionEntities.get(0);
        assertEquals("20Y3M4D", caseRetentionEntity.getTotalSentence());
        assertEquals(OffsetDateTime.of(2045, 1, 5, 0, 0, 0, 0, ZoneOffset.UTC), caseRetentionEntity.getRetainUntil());
        assertEquals("PENDING", caseRetentionEntity.getCurrentState());
        assertEquals(5, caseRetentionEntity.getRetentionPolicyType().getId());
        assertNotNull(caseRetentionEntity.getCaseManagementRetention().getId());

        /*
        now add a 2nd event, which will override the first one
         */
        DartsEventRetentionPolicy retentionPolicy2 = new DartsEventRetentionPolicy();
        retentionPolicy2.caseRetentionFixedPolicy("2");

        DartsEvent dartsEvent2 = someMinimalDartsEvent()
                .type(hearingEndedEventHandler.getType())
                .subType(hearingEndedEventHandler.getSubType())
                .caseNumbers(List.of(SOME_CASE_NUMBER))
                .dateTime(testTime.plusSeconds(10))
                .retentionPolicy(retentionPolicy2);

        eventDispatcher.receive(dartsEvent2);

        List<CaseRetentionEntity> caseRetentionEntities2 = dartsDatabase.getCaseRetentionRepository().findByCaseId(persistedCase.getId());
        assertEquals(1, caseRetentionEntities2.size());
        caseRetentionEntity = caseRetentionEntities2.get(0);
        assertNull(caseRetentionEntity.getTotalSentence());
        assertEquals(OffsetDateTime.of(2031, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC), caseRetentionEntity.getRetainUntil());
        assertEquals("PENDING", caseRetentionEntity.getCurrentState());
        assertEquals(4, caseRetentionEntity.getRetentionPolicyType().getId());
        assertNotNull(caseRetentionEntity.getCaseManagementRetention().getId());

        eventsForHearing = dartsDatabase.getEventRepository().findAllByHearingId(hearing.getId());
        assertEquals(2, eventsForHearing.size());
        eventsForHearing = eventsForHearing.stream().sorted(Comparator.comparing(EventEntity::getCreatedDateTime)).toList();
        EventEntity latestEvent = eventsForHearing.get(eventsForHearing.size() - 1);

        List<CaseManagementRetentionEntity> caseManagementRetentionEntities = dartsDatabase.getCaseManagementRetentionRepository().findAll();
        assertEquals(2, caseManagementRetentionEntities.size());

        CaseManagementRetentionEntity caseManagementRetentionEntity = dartsDatabase.getCaseManagementRetentionRepository().findById(
                caseRetentionEntity.getCaseManagementRetention().getId()).get();
        assertEquals(latestEvent.getId(), caseManagementRetentionEntity.getEventEntity().getId());

        /*
        now add a 3rd event, which will NOT override any, as its before the 2nd.
         */
        DartsEventRetentionPolicy retentionPolicy3 = new DartsEventRetentionPolicy();
        retentionPolicy3.caseRetentionFixedPolicy("4");

        DartsEvent dartsEvent3 = someMinimalDartsEvent()
                .type(hearingEndedEventHandler.getType())
                .subType(hearingEndedEventHandler.getSubType())
                .caseNumbers(List.of(SOME_CASE_NUMBER))
                .dateTime(testTime.plusSeconds(5))
                .retentionPolicy(retentionPolicy3);

        eventDispatcher.receive(dartsEvent3);

        List<CaseRetentionEntity> caseRetentionEntities3 = dartsDatabase.getCaseRetentionRepository().findByCaseId(persistedCase.getId());
        assertEquals(1, caseRetentionEntities3.size());
        caseRetentionEntity = caseRetentionEntities3.get(0);
        assertNull(caseRetentionEntity.getTotalSentence());
        assertEquals(OffsetDateTime.of(2031, 10, 1, 0, 0, 0, 0, ZoneOffset.UTC), caseRetentionEntity.getRetainUntil());
        assertEquals("PENDING", caseRetentionEntity.getCurrentState());
        assertEquals(4, caseRetentionEntity.getRetentionPolicyType().getId());
        assertNotNull(caseRetentionEntity.getCaseManagementRetention().getId());

        eventsForHearing = dartsDatabase.getEventRepository().findAllByHearingId(hearing.getId());
        assertEquals(3, eventsForHearing.size());
        eventsForHearing = eventsForHearing.stream().sorted(Comparator.comparing(EventEntity::getCreatedDateTime)).toList();
        latestEvent = eventsForHearing.get(eventsForHearing.size() - 1);

        caseManagementRetentionEntities = dartsDatabase.getCaseManagementRetentionRepository().findAll();
        assertEquals(3, caseManagementRetentionEntities.size());

        assertNotEquals(latestEvent.getId(), caseManagementRetentionEntity.getEventEntity().getId());

    }

    @Test
    void shouldIgnoreWhenManualRetentionExists() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        assertFalse(courtCaseEntity.getClosed());
        assertNull(courtCaseEntity.getCaseClosedTimestamp());

        dartsGateway.darNotificationReturnsSuccess();

        List<EventHandlerEntity> eventHandlerEntityList = dartsDatabase.findByHandlerAndActiveTrue(
                STOP_AND_CLOSE_HANDLER);

        dartsDatabase.getCaseRetentionStub().createCaseRetentionObject(courtCaseEntity, CaseRetentionStatus.COMPLETE,
                                                                       OffsetDateTime.of(2021, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC), true);

        EventHandlerEntity hearingEndedEventHandler = eventHandlerEntityList.stream()
                .filter(eventHandlerEntity -> ARCHIVE_CASE_EVENT_NAME.equals(eventHandlerEntity.getEventName()))
                .findFirst()
                .orElseThrow();

        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.caseRetentionFixedPolicy("3");
        retentionPolicy.setCaseTotalSentence("20Y3M4D");

        DartsEvent dartsEvent = someMinimalDartsEvent()
                .type(hearingEndedEventHandler.getType())
                .subType(hearingEndedEventHandler.getSubType())
                .caseNumbers(List.of(SOME_CASE_NUMBER))
                .dateTime(testTime)
                .retentionPolicy(retentionPolicy);

        eventDispatcher.receive(dartsEvent);

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
                SOME_CASE_NUMBER,
                SOME_COURTHOUSE
        ).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
                SOME_COURTHOUSE, SOME_ROOM, testTime.toLocalDate());

        var hearing = hearingsForCase.get(0);

        List<EventEntity> eventsForHearing = dartsDatabase.getEventRepository().findAllByHearingId(hearing.getId());
        assertEquals(1, eventsForHearing.size());

        List<CaseRetentionEntity> caseRetentionEntities = dartsDatabase.getCaseRetentionRepository().findByCaseId(persistedCase.getId());
        assertEquals(1, caseRetentionEntities.size());
        CaseRetentionEntity caseRetentionEntity = caseRetentionEntities.get(0);
        assertEquals(OffsetDateTime.of(2021, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC), caseRetentionEntity.getRetainUntil());
        assertEquals("COMPLETE", caseRetentionEntity.getCurrentState());
        assertEquals(9, caseRetentionEntity.getRetentionPolicyType().getId());

        List<CaseManagementRetentionEntity> caseManagementRetentionEntities = dartsDatabase.getCaseManagementRetentionRepository().findAll();
        assertEquals(1, caseManagementRetentionEntities.size());

        verify(caseRetentionRepository, never()).saveAndFlush(any());

    }


    private static DartsEvent someMinimalDartsEvent() {
        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.caseRetentionFixedPolicy("3");
        retentionPolicy.setCaseTotalSentence("20Y3M4D");
        return new DartsEvent()
                .messageId("some-message-id")
                .type(ARCHIVE_CASE_EVENT_TYPE)
                .subType(null)
                .eventId("1")
                .courthouse(SOME_COURTHOUSE)
                .courtroom(SOME_ROOM)
                .eventText("some-text")
                .retentionPolicy(retentionPolicy);
    }

}
