package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CaseManagementRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.event.model.DartsEvent;
import uk.gov.hmcts.darts.event.model.DartsEventRetentionPolicy;
import uk.gov.hmcts.darts.event.service.EventDispatcher;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionProcessor;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.RetentionConfidenceCategoryMapperTestData;
import uk.gov.hmcts.darts.test.common.data.builder.TestRetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.testutils.stubs.NodeRegisterStub;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus.COMPLETE;
import static uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus.IGNORED;
import static uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus.PENDING;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum.CASE_CLOSED;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true") // To override Clock bean
class StopAndCloseHandlerTest extends HandlerTestData {

    private static final String ARCHIVE_CASE_EVENT_TYPE = "3000";
    private static final String ARCHIVE_CASE_EVENT_NAME = "Archive Case";
    private static final String STOP_AND_CLOSE_HANDLER = "StopAndCloseHandler";
    private static final OffsetDateTime CURRENT_DATE_TIME = OffsetDateTime.of(2024, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime testTime = OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private EventDispatcher eventDispatcher;

    @Autowired
    NodeRegisterStub nodeRegisterStub;

    @Autowired
    ApplyRetentionProcessor applyRetentionProcessor;

    @MockitoBean
    private CurrentTimeHelper currentTimeHelper;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    @Mock
    private CaseRetentionRepository caseRetentionRepository;

    @TestConfiguration
    public static class ClockConfig {
        @Bean
        public Clock clock() {
            return Clock.fixed(CURRENT_DATE_TIME.toInstant(), ZoneOffset.UTC);
        }
    }

    @BeforeEach
    public void setupStubs() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(CURRENT_DATE_TIME);

        CourtroomEntity courtroom = dartsDatabase.createCourtroomUnlessExists(SOME_COURTHOUSE, SOME_ROOM);
        nodeRegisterStub.setupNodeRegistry(courtroom);
        dartsGateway.darNotificationReturnsSuccess();

        createAndSaveRetentionConfidenceCategoryMappings();
    }

    private void createAndSaveRetentionConfidenceCategoryMappings() {
        RetentionConfidenceCategoryMapperTestData testData = PersistableFactory.getRetentionConfidenceCategoryMapperTestData();

        TestRetentionConfidenceCategoryMapperEntity closedMappingEntity = testData.someMinimalBuilder()
            .confidenceCategory(CASE_CLOSED)
            .confidenceReason(RetentionConfidenceReasonEnum.CASE_CLOSED)
            .confidenceScore(CASE_PERFECTLY_CLOSED)
            .build();
        dartsPersistence.save(closedMappingEntity.getEntity());
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
    void givenDarStopAndCloseEventReceivedAndCourtCaseAndHearingDoesNotExist_thenNotifyDarUpdateAndNotifyDarStopRecording() {
        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(SOME_CASE_NUMBER, SOME_COURTHOUSE).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertThat(persistedCase.getClosed()).isTrue();
        assertEquals(HEARING_DATE_ODT, persistedCase.getCaseClosedTimestamp());

        dartsGateway.verifyReceivedNotificationType(2);
        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 2);
    }

    @Test
    void givenDarStopAndCloseEventReceivedAndHearingDoesNotExist_thenNotifyDarUpdateAndNotifyDarStopRecording() {
        var caseEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(SOME_CASE_NUMBER, SOME_COURTHOUSE, SOME_ROOM);

        assertFalse(caseEntity.getClosed());
        assertNull(caseEntity.getCaseClosedTimestamp());

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .caseNumbers(List.of(SOME_CASE_NUMBER))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(SOME_CASE_NUMBER, SOME_COURTHOUSE).get();

        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate());

        var persistedEvent = dartsDatabase.getAllEvents().get(0);

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertThat(persistedCase.getClosed()).isTrue();
        assertEquals(HEARING_DATE_ODT, persistedCase.getCaseClosedTimestamp());

        dartsGateway.verifyReceivedNotificationType(2);
        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 2);
    }


    @Test
    void givenDarStopAndCloseEventEventReceivedAndCaseAndHearingExistButRoomHasChanged_thenNotifyDarUpdateAndNotifyDarStopRecording() {
        var caseEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(SOME_CASE_NUMBER, SOME_COURTHOUSE, SOME_ROOM);

        assertFalse(caseEntity.getClosed());
        assertNull(caseEntity.getCaseClosedTimestamp());

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

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_OTHER_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertTrue(dartsDatabase.findByCourthouseCourtroomAndDate(SOME_COURTHOUSE, SOME_ROOM, HEARING_DATE_ODT.toLocalDate()).isEmpty());

        assertThat(persistedCase.getClosed()).isTrue();
        assertEquals(HEARING_DATE_ODT, persistedCase.getCaseClosedTimestamp());

        dartsGateway.verifyReceivedNotificationType(2);
        dartsGateway.verifyReceivedNotificationType(3);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 2);
    }

    @Test
    void givenDarStopAndCloseEventReceivedAndCaseAndHearingExistAndRoomHasNotChanged_thenNotifyDarStopRecording() {
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

        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));
        assertThat(persistedCase.getCourthouse().getCourthouseName()).isEqualTo(SOME_COURTHOUSE.toUpperCase(Locale.ROOT));
        assertThat(hearingsForCase.size()).isEqualTo(1);
        assertThat(hearingsForCase.get(0).getHearingIsActual()).isEqualTo(true);

        assertThat(persistedCase.getClosed()).isTrue();
        assertEquals(HEARING_DATE_ODT, persistedCase.getCaseClosedTimestamp());

        dartsGateway.verifyReceivedNotificationType(2);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
    }

    @Test
    void givenDarStopAndCloseEventReceivedAndDartsGatewayIsDown_whenDarNotifyOccurs_thenEventIsStillPersisted() {
        dartsGateway.darNotificationReturnsGatewayTimeoutError();

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

        var allEvents = dartsDatabase.getAllEvents();
        assertEquals(1, allEvents.size());

        var persistedEvent = allEvents.get(0);
        assertThat(persistedEvent.getCourtroom().getName()).isEqualTo(SOME_ROOM.toUpperCase(Locale.ROOT));

        dartsGateway.verifyReceivedNotificationType(2);
        dartsGateway.verifyNotificationUrl("http://1.2.3.4/VIQDARNotifyEvent/DARNotifyEvent.asmx", 1);
    }

    @Test
    void shouldCreateNewCaseRetentionWhenNoneExist() {
        // given
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        assertFalse(courtCaseEntity.getClosed());
        assertNull(courtCaseEntity.getCaseClosedTimestamp());

        EventHandlerEntity hearingEndedEventHandler = getHearingEndedEventHandler();

        DartsEventRetentionPolicy retentionPolicy = new DartsEventRetentionPolicy();
        retentionPolicy.caseRetentionFixedPolicy("3");
        retentionPolicy.setCaseTotalSentence("20Y3M4D");

        DartsEvent dartsEvent = someMinimalDartsEvent()
            .type(hearingEndedEventHandler.getType())
            .subType(hearingEndedEventHandler.getSubType())
            .caseNumbers(List.of(SOME_CASE_NUMBER))
            .dateTime(testTime)
            .retentionPolicy(retentionPolicy);

        // when
        eventDispatcher.receive(dartsEvent);

        // then
        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        assertEquals(testTime, persistedCase.getCaseClosedTimestamp());
        assertTrue(persistedCase.getClosed());
        assertEquals(RetentionConfidenceReasonEnum.CASE_CLOSED, persistedCase.getRetConfReason());
        assertEquals(CASE_PERFECTLY_CLOSED, persistedCase.getRetConfScore());
        assertEquals(CURRENT_DATE_TIME, persistedCase.getRetConfUpdatedTs());
        var hearingsForCase = dartsDatabase.findByCourthouseCourtroomAndDate(
            SOME_COURTHOUSE, SOME_ROOM, testTime.toLocalDate());

        var hearing = hearingsForCase.get(0);

        List<EventEntity> eventsForHearing = dartsDatabase.getEventRepository().findAllByHearingId(hearing.getId());
        assertEquals(1, eventsForHearing.size());

        List<CaseRetentionEntity> caseRetentionEntities = dartsDatabase.getCaseRetentionRepository().findByCaseId(persistedCase.getId());
        assertEquals(1, caseRetentionEntities.size());
        CaseRetentionEntity caseRetentionEntity = caseRetentionEntities.get(0);
        assertEquals("20Y3M4D", caseRetentionEntity.getTotalSentence());
        assertEquals(OffsetDateTime.of(2041, 1, 14, 0, 0, 0, 0, ZoneOffset.UTC), caseRetentionEntity.getRetainUntil());
        assertEquals("PENDING", caseRetentionEntity.getCurrentState());
        assertEquals(5, caseRetentionEntity.getRetentionPolicyType().getId());
        assertEquals(CASE_CLOSED, caseRetentionEntity.getConfidenceCategory());
        assertNotNull(caseRetentionEntity.getCaseManagementRetention().getId());
    }

    @Test
    void shouldUpdateExistingCaseRetentionWhenPendingExist() {
        // given
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        assertFalse(courtCaseEntity.getClosed());
        assertNull(courtCaseEntity.getCaseClosedTimestamp());

        HearingEntity hearing = dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER,
                                                                             DateConverterUtil.toLocalDateTime(testTime));

        //setup existing retention
        CaseRetentionEntity caseRetentionObject = dartsDatabase.getCaseRetentionStub().createCaseRetentionObject(
            courtCaseEntity, PENDING,
            OffsetDateTime.of(2021, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC),
            false
        );
        EventEntity existingPendingEvent = dartsDatabase.getEventStub().createEvent(hearing, 77, testTime,
                                                                                    "EVENTNAME");
        CaseManagementRetentionEntity existingCaseManagement = new CaseManagementRetentionEntity();
        existingCaseManagement.setCourtCase(courtCaseEntity);
        existingCaseManagement.setEventEntity(existingPendingEvent);
        existingCaseManagement.setRetentionPolicyTypeEntity(caseRetentionObject.getRetentionPolicyType());
        dartsDatabase.save(existingCaseManagement);

        caseRetentionObject.setCaseManagementRetention(existingCaseManagement);
        dartsDatabase.save(caseRetentionObject);

        EventHandlerEntity hearingEndedEventHandler = getHearingEndedEventHandler();

        DartsEventRetentionPolicy retentionPolicy2 = new DartsEventRetentionPolicy();
        retentionPolicy2.caseRetentionFixedPolicy("2");
        retentionPolicy2.setCaseTotalSentence("20Y3M4D");//this should get ignored.

        DartsEvent dartsEvent = someMinimalDartsEvent()
            .type(hearingEndedEventHandler.getType())
            .subType(hearingEndedEventHandler.getSubType())
            .caseNumbers(List.of(SOME_CASE_NUMBER))
            .dateTime(testTime.plusSeconds(10))
            .retentionPolicy(retentionPolicy2);

        // when
        eventDispatcher.receive(dartsEvent);

        // then
        List<CaseRetentionEntity> caseRetentionEntities2 = dartsDatabase.getCaseRetentionRepository().findByCaseId(courtCaseEntity.getId());
        assertEquals(1, caseRetentionEntities2.size());
        CaseRetentionEntity caseRetentionEntity = caseRetentionEntities2.get(0);
        assertNull(caseRetentionEntity.getTotalSentence());
        var date7YearsLater = testTime.plusYears(7).truncatedTo(ChronoUnit.DAYS);
        assertEquals(date7YearsLater, caseRetentionEntity.getRetainUntil());
        assertEquals(String.valueOf(PENDING), caseRetentionEntity.getCurrentState());
        assertEquals(4, caseRetentionEntity.getRetentionPolicyType().getId());
        assertEquals(CASE_CLOSED, caseRetentionEntity.getConfidenceCategory());
        assertNotNull(caseRetentionEntity.getCaseManagementRetention().getId());

        List<EventEntity> eventsForHearing = dartsDatabase.getEventRepository().findAllByHearingId(hearing.getId());
        assertEquals(2, eventsForHearing.size());
        eventsForHearing = eventsForHearing.stream().sorted(Comparator.comparing(EventEntity::getCreatedDateTime)).toList();
        EventEntity latestEvent = eventsForHearing.get(eventsForHearing.size() - 1);

        List<CaseManagementRetentionEntity> caseManagementRetentionEntities = dartsDatabase.getCaseManagementRetentionRepository().findAll();
        assertEquals(2, caseManagementRetentionEntities.size());

        CaseManagementRetentionEntity caseManagementRetentionEntity = dartsDatabase.getCaseManagementRetentionRepository().findById(
            caseRetentionEntity.getCaseManagementRetention().getId()).get();
        assertEquals(latestEvent.getId(), caseManagementRetentionEntity.getEventEntity().getId());

        var persistedCase = dartsDatabase.findByCaseByCaseNumberAndCourtHouseName(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE
        ).get();

        assertTrue(persistedCase.getClosed());
        assertEquals(testTime.plusSeconds(10), persistedCase.getCaseClosedTimestamp());
        assertEquals(RetentionConfidenceReasonEnum.CASE_CLOSED, persistedCase.getRetConfReason());
        assertEquals(CASE_PERFECTLY_CLOSED, persistedCase.getRetConfScore());
        assertEquals(CURRENT_DATE_TIME, persistedCase.getRetConfUpdatedTs());

        // apply retention and check it was applied correctly
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now().plusMonths(1));
        applyRetentionProcessor.processApplyRetention(1000);
        CaseRetentionEntity processedCaseRetentionEntity = dartsDatabase.getCaseRetentionRepository().findById(caseRetentionEntity.getId()).get();
        assertEquals(String.valueOf(COMPLETE), processedCaseRetentionEntity.getCurrentState());
    }

    /**
     * This test was created as part of DMP-3867, to validate the outcome of a missing case_management_retention record
     * for a case which had already received a case closed event, with retention information.
     *
     * <p>The outcome, that can be seen below, is multiple pending case_retention records associated with the case.
     * The created datetime fields are set correctly as per the order received. When retention is applied the latest
     * record becomes COMPLETE and the initial one IGNORED, as expected.
     *
     * <p>This differs from the "shouldUpdateExistingCaseRetentionWhenPendingExist" test above, where the existing case
     * retention record is updated with the information received in the latest event. When retention is applied the
     * record becomes COMPLETE, as expected and also giving the same result as this test.
     */
    @Test
    void createsAdditionalPendingCaseRetentionWhenCaseManagementRetentionDoesNotExist() {
        // create an open case
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        assertFalse(courtCaseEntity.getClosed());
        assertNull(courtCaseEntity.getCaseClosedTimestamp());

        // setup existing retention, without link to case management retention
        var initialRetainUntilDate = OffsetDateTime.of(2021, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
        CaseRetentionEntity caseRetentionObject = dartsDatabase.getCaseRetentionStub().createCaseRetentionObject(
            courtCaseEntity,
            PENDING,
            initialRetainUntilDate,
            false
        );

        dartsDatabase.save(caseRetentionObject);

        EventHandlerEntity hearingEndedEventHandler = getHearingEndedEventHandler();

        DartsEventRetentionPolicy retentionPolicy2 = new DartsEventRetentionPolicy();
        retentionPolicy2.caseRetentionFixedPolicy("2");
        retentionPolicy2.setCaseTotalSentence("20Y3M4D");//this should get ignored.

        DartsEvent dartsEvent = someMinimalDartsEvent()
            .type(hearingEndedEventHandler.getType())
            .subType(hearingEndedEventHandler.getSubType())
            .caseNumbers(List.of(SOME_CASE_NUMBER))
            .dateTime(testTime.plusSeconds(10))
            .retentionPolicy(retentionPolicy2);

        // when
        eventDispatcher.receive(dartsEvent);

        // then
        List<CaseRetentionEntity> caseRetentionEntities2 = dartsDatabase.getCaseRetentionRepository().findByCaseId(courtCaseEntity.getId());
        // there are 2 case retention entries
        assertEquals(2, caseRetentionEntities2.size());

        assertThat(caseRetentionEntities2.get(0).getCreatedDateTime()).isAfter(caseRetentionEntities2.get(1).getCreatedDateTime());

        // the latest entry should match the data received from the event
        CaseRetentionEntity latestCaseRetentionEntity = caseRetentionEntities2.get(0);
        assertNull(latestCaseRetentionEntity.getTotalSentence());
        var date7YearsLater = testTime.plusYears(7).truncatedTo(ChronoUnit.DAYS);
        assertEquals(date7YearsLater, latestCaseRetentionEntity.getRetainUntil());
        assertEquals(String.valueOf(PENDING), latestCaseRetentionEntity.getCurrentState());
        assertEquals(4, latestCaseRetentionEntity.getRetentionPolicyType().getId());
        assertEquals(CASE_CLOSED, latestCaseRetentionEntity.getConfidenceCategory());
        assertNotNull(latestCaseRetentionEntity.getCaseManagementRetention().getId());

        // the initial one is untouched by the new event, with no CMR link
        CaseRetentionEntity initialCaseRetentionEntity = caseRetentionEntities2.get(1);
        assertEquals(initialRetainUntilDate, initialCaseRetentionEntity.getRetainUntil());
        assertEquals(String.valueOf(PENDING), initialCaseRetentionEntity.getCurrentState());
        assertNull(initialCaseRetentionEntity.getCaseManagementRetention());

        // the initial entry should be created first
        assertTrue(initialCaseRetentionEntity.getCreatedDateTime().isBefore(latestCaseRetentionEntity.getCreatedDateTime()));

        // only one event linked to the case
        List<EventEntity> eventsForHearing = dartsDatabase.getEventRepository().findAllByCaseId(courtCaseEntity.getId());
        assertEquals(1, eventsForHearing.size());
        EventEntity latestEvent = eventsForHearing.get(0);

        // only one case management retention entity, created with the latest event received
        List<CaseManagementRetentionEntity> caseManagementRetentionEntities = dartsDatabase.getCaseManagementRetentionRepository().findAll();
        assertEquals(1, caseManagementRetentionEntities.size());

        // checking it links to the event which created it
        CaseManagementRetentionEntity caseManagementRetentionEntity = dartsDatabase.getCaseManagementRetentionRepository().findById(
            latestCaseRetentionEntity.getCaseManagementRetention().getId()).get();
        assertEquals(latestEvent.getId(), caseManagementRetentionEntity.getEventEntity().getId());

        // checking the case is closed
        var persistedCase = dartsDatabase.getCaseRepository().findById(courtCaseEntity.getId()).get();
        assertTrue(persistedCase.getClosed());
        assertEquals(testTime.plusSeconds(10), persistedCase.getCaseClosedTimestamp());
        assertEquals(RetentionConfidenceReasonEnum.CASE_CLOSED, persistedCase.getRetConfReason());
        assertEquals(CASE_PERFECTLY_CLOSED, persistedCase.getRetConfScore());
        assertEquals(CURRENT_DATE_TIME, persistedCase.getRetConfUpdatedTs());

        // apply retention and check it was applied correctly
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now().plusMonths(1));
        applyRetentionProcessor.processApplyRetention(1000);
        CaseRetentionEntity processedInitialCaseRetentionEntity = dartsDatabase.getCaseRetentionRepository().findById(initialCaseRetentionEntity.getId()).get();
        CaseRetentionEntity processedLatestCaseRetentionEntity = dartsDatabase.getCaseRetentionRepository().findById(latestCaseRetentionEntity.getId()).get();
        assertEquals(String.valueOf(IGNORED), processedInitialCaseRetentionEntity.getCurrentState());
        assertEquals(String.valueOf(COMPLETE), processedLatestCaseRetentionEntity.getCurrentState());
    }

    @Test
    void shouldDoNothingWhenPendingExistBeforeThisOne() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        assertFalse(courtCaseEntity.getClosed());
        assertNull(courtCaseEntity.getCaseClosedTimestamp());

        HearingEntity hearing = dartsDatabase.getHearingStub().createHearing(SOME_COURTHOUSE, SOME_ROOM, SOME_CASE_NUMBER,
                                                                             DateConverterUtil.toLocalDateTime(testTime));

        //setup existing retention
        CaseRetentionEntity caseRetentionObject = dartsDatabase.getCaseRetentionStub().createCaseRetentionObject(courtCaseEntity, PENDING,
                                                                                                                 OffsetDateTime.of(2021, 10, 10, 10, 0, 0, 0,
                                                                                                                                   ZoneOffset.UTC), false);
        EventEntity existingPendingEvent = dartsDatabase.getEventStub().createEvent(hearing, 77, testTime.plusSeconds(20),
                                                                                    "EVENTNAME");
        CaseManagementRetentionEntity existingCaseManagement = new CaseManagementRetentionEntity();
        existingCaseManagement.setCourtCase(courtCaseEntity);
        existingCaseManagement.setEventEntity(existingPendingEvent);
        existingCaseManagement.setRetentionPolicyTypeEntity(caseRetentionObject.getRetentionPolicyType());
        dartsDatabase.save(existingCaseManagement);

        caseRetentionObject.setCaseManagementRetention(existingCaseManagement);
        dartsDatabase.save(caseRetentionObject);

        EventHandlerEntity hearingEndedEventHandler = getHearingEndedEventHandler();

        DartsEventRetentionPolicy retentionPolicy2 = new DartsEventRetentionPolicy();
        retentionPolicy2.caseRetentionFixedPolicy("2");

        DartsEvent dartsEvent = someMinimalDartsEvent()
            .type(hearingEndedEventHandler.getType())
            .subType(hearingEndedEventHandler.getSubType())
            .caseNumbers(List.of(SOME_CASE_NUMBER))
            .dateTime(testTime.plusSeconds(10))
            .retentionPolicy(retentionPolicy2);

        eventDispatcher.receive(dartsEvent);

        List<CaseRetentionEntity> caseRetentionEntities = dartsDatabase.getCaseRetentionRepository().findByCaseId(courtCaseEntity.getId());
        assertEquals(1, caseRetentionEntities.size());
        CaseRetentionEntity caseRetentionEntity = caseRetentionEntities.get(0);
        assertEquals("10y0m0d", caseRetentionEntity.getTotalSentence());
        assertEquals(OffsetDateTime.of(2021, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC), caseRetentionEntity.getRetainUntil());
        assertEquals(String.valueOf(PENDING), caseRetentionEntity.getCurrentState());
        assertEquals(7, caseRetentionEntity.getRetentionPolicyType().getId());
        assertNotNull(caseRetentionEntity.getCaseManagementRetention().getId());

        List<EventEntity> eventsForHearing = dartsDatabase.getEventRepository().findAllByHearingId(hearing.getId());
        assertEquals(2, eventsForHearing.size());
        eventsForHearing = eventsForHearing.stream().sorted(Comparator.comparing(EventEntity::getCreatedDateTime)).toList();
        EventEntity latestEvent = eventsForHearing.get(eventsForHearing.size() - 1);

        List<CaseManagementRetentionEntity> caseManagementRetentionEntities = dartsDatabase.getCaseManagementRetentionRepository().findAll();
        assertEquals(2, caseManagementRetentionEntities.size());

        CaseManagementRetentionEntity caseManagementRetentionEntity = dartsDatabase.getCaseManagementRetentionRepository().findById(
            caseRetentionEntity.getCaseManagementRetention().getId()).get();
        assertNotEquals(latestEvent.getId(), caseManagementRetentionEntity.getEventEntity().getId());
    }

    @Test
    void shouldIgnoreWhenManualRetentionExists() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        assertFalse(courtCaseEntity.getClosed());
        assertNull(courtCaseEntity.getCaseClosedTimestamp());

        dartsDatabase.getCaseRetentionStub().createCaseRetentionObject(courtCaseEntity, COMPLETE,
                                                                       OffsetDateTime.of(2021, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC), true);

        EventHandlerEntity hearingEndedEventHandler = getHearingEndedEventHandler();

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
        assertNull(caseRetentionEntity.getConfidenceCategory());

        List<CaseManagementRetentionEntity> caseManagementRetentionEntities = dartsDatabase.getCaseManagementRetentionRepository().findAll();
        assertEquals(1, caseManagementRetentionEntities.size());

        verify(caseRetentionRepository, never()).saveAndFlush(any());
    }


    @Test
    void shouldCreateNewCaseRetentionWithDefaultPolicyWhenNotDefined() {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase(SOME_COURTHOUSE, SOME_CASE_NUMBER);
        assertFalse(courtCaseEntity.getClosed());
        assertNull(courtCaseEntity.getCaseClosedTimestamp());

        EventHandlerEntity hearingEndedEventHandler = getHearingEndedEventHandler();

        DartsEvent dartsEvent = someMinimalDartsEvent()
            .type(hearingEndedEventHandler.getType())
            .subType(hearingEndedEventHandler.getSubType())
            .caseNumbers(List.of(SOME_CASE_NUMBER))
            .dateTime(testTime)
            .retentionPolicy(null);

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
        assertNull(caseRetentionEntity.getTotalSentence());
        assertEquals(OffsetDateTime.of(2027, 10, 10, 0, 0, 0, 0, ZoneOffset.UTC), caseRetentionEntity.getRetainUntil());
        assertEquals("PENDING", caseRetentionEntity.getCurrentState());
        assertEquals(7, caseRetentionEntity.getRetentionPolicyType().getId());
        assertNotNull(caseRetentionEntity.getCaseManagementRetention().getId());
    }

    @Test
    void createsAnEventLinkedCaseForStopAndCloseHandlerWhenCourtroomDoesntExist() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM
        );

        dartsDatabase.getNodeRegisterRepository().deleteAll();
        dartsDatabase.getCourtroomRepository()
            .findByCourthouseNameAndCourtroomName(SOME_COURTHOUSE, SOME_ROOM)
            .ifPresent(c -> dartsDatabase.getCourtroomRepository().delete(c));

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .type("30300")
                                    .subType(null)
                                    .caseNumbers(List.of(SOME_CASE_NUMBER, SOME_CASE_NUMBER_2))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedEvents = dartsDatabase.getEventRepository().findAll();
        var eventLinkedCases = dartsDatabase.getEventLinkedCaseRepository().findAll();

        assertThat(eventLinkedCases)
            .extracting("courtCase.caseNumber")
            .containsExactly(SOME_CASE_NUMBER, SOME_CASE_NUMBER_2);

        assertThat(eventLinkedCases)
            .extracting("event.id")
            .containsOnly(idFrom(persistedEvents));
    }

    @Test
    void createsAnEventLinkedCaseForStopAndCloseHandlerWhenTheCaseDoesntExist() {
        dartsDatabase.givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_ROOM
        );

        dartsDatabase.getNodeRegisterRepository().deleteAll();
        dartsDatabase.getCourtroomRepository()
            .findByCourthouseNameAndCourtroomName(SOME_COURTHOUSE, SOME_ROOM)
            .ifPresent(c -> dartsDatabase.getCourtroomRepository().delete(c));
        dartsDatabase.getCaseRepository().deleteAll();

        eventDispatcher.receive(someMinimalDartsEvent()
                                    .type("30300")
                                    .subType(null)
                                    .caseNumbers(List.of(SOME_CASE_NUMBER, SOME_CASE_NUMBER_2))
                                    .courthouse(SOME_COURTHOUSE)
                                    .courtroom(SOME_ROOM)
                                    .dateTime(HEARING_DATE_ODT));

        var persistedEvents = dartsDatabase.getEventRepository().findAll();
        var eventLinkedCases = dartsDatabase.getEventLinkedCaseRepository().findAll();

        assertThat(eventLinkedCases)
            .extracting("courtCase.caseNumber")
            .containsExactly(SOME_CASE_NUMBER, SOME_CASE_NUMBER_2);

        assertThat(eventLinkedCases)
            .extracting("event.id")
            .containsOnly(idFrom(persistedEvents));
    }

    private Integer idFrom(List<EventEntity> eventEntities) {
        return eventEntities.get(0).getId();
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

    private EventHandlerEntity getHearingEndedEventHandler() {
        List<EventHandlerEntity> eventHandlerEntityList = dartsDatabase.findByHandlerAndActiveTrue(STOP_AND_CLOSE_HANDLER);
        return eventHandlerEntityList.stream()
            .filter(eventHandlerEntity -> ARCHIVE_CASE_EVENT_NAME.equals(eventHandlerEntity.getEventName()))
            .findFirst()
            .orElseThrow();
    }

}