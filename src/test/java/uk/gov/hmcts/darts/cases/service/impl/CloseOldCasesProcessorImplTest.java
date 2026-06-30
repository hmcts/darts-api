package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.cases.helper.FindCurrentEntitiesHelper;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.cases.service.CloseOldCasesProcessor;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.retention.api.RetentionApi;
import uk.gov.hmcts.darts.retention.enums.CaseRetentionStatus;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionPolicyEnum;
import uk.gov.hmcts.darts.retention.helper.RetentionDateHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseOldCasesProcessorImplTest {

    private static final OffsetDateTime CURRENT_DATE_TIME = OffsetDateTime.of(2024, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseRetentionRepository caseRetentionRepository;
    @Mock
    private FindCurrentEntitiesHelper findCurrentEntitiesHelper;
    @Mock
    private RetentionApi retentionApi;
    @Mock
    private RetentionDateHelper retentionDateHelper;
    @Mock
    private AuthorisationApi authorisationApi;

    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private CaseService caseService;

    private UserAccountEntity userAccountEntity;

    private CloseOldCasesProcessor closeOldCasesProcessor;

    @BeforeEach
    void setUp() {
        userAccountEntity = CommonTestDataUtil.createUserAccountWithId();
        when(authorisationApi.getCurrentUser()).thenReturn(userAccountEntity);

        CloseOldCasesProcessorImpl.CloseCaseProcessor closeCaseProcessor = new CloseOldCasesProcessorImpl.CloseCaseProcessor(
            caseService,
            caseRetentionRepository,
            retentionApi,
            retentionDateHelper,
            findCurrentEntitiesHelper
        );
        ReflectionTestUtils.setField(closeCaseProcessor, "closeEvents", List.of(79, 218));

        Period closeOpenCasesPeriod = Period.ofYears(6);
        closeOldCasesProcessor = new CloseOldCasesProcessorImpl(closeCaseProcessor, caseRepository, authorisationApi, closeOpenCasesPeriod);

        lenient().when(currentTimeHelper.currentOffsetDateTime()).thenReturn(CURRENT_DATE_TIME);
    }

    @Test
    void closeCases_shouldCloseTheExpectedCases_andSetExpectedRetentionConfidence() {
        // given
        LocalDateTime hearingDate = DateConverterUtil.toLocalDateTime(OffsetDateTime.now().minusYears(7));
        OffsetDateTime createdDate = OffsetDateTime.now().minusYears(7);

        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);
        HearingEntity hearingEntity = hearings.getFirst();
        hearingEntity.setHearingDate(hearingDate.minusDays(10).toLocalDate());
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.setCreatedDateTime(createdDate);
        courtCase.setClosed(false);
        courtCase.setHearings(hearings);
        courtCase.setId(1);
        stubCaseToClose(courtCase);

        // Create test events excluding event handler types 79 and 218
        List<EventEntity> testEvents = createTestEventsExcludingCloseEventTypes(hearingEntity);
        when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(testEvents);

        // Create test media
        List<MediaEntity> testMedia = createTestMedia(hearingEntity);
        lenient().when(findCurrentEntitiesHelper.getCurrentMedia(courtCase)).thenReturn(testMedia);

        LocalDate retentionDate = stubRetentionCreation(courtCase);
        OffsetDateTime expectedCaseClosedTimestamp = testEvents.get(2).getCreatedDateTime();
        assertFalse(courtCase.getClosed());

        // when
        closeOldCasesProcessor.closeCases(2);

        // then
        assertTrue(courtCase.getClosed());
        assertNotNull(courtCase.getCaseClosedTimestamp());
        assertEquals(expectedCaseClosedTimestamp, courtCase.getCaseClosedTimestamp());
        assertNull(courtCase.getRetConfReason());
        assertNull(courtCase.getRetConfScore());
        assertNull(courtCase.getRetConfUpdatedTs());
        verifyRetentionCreated(courtCase, retentionDate, RetentionConfidenceCategoryEnum.AGED_CASE_MAX_EVENT_CLOSED);
    }

    @Test
    void closeCases_shouldUseLatestCloseEvent_whenCurrentEventsContainCloseEventTypes() {
        // given
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);
        HearingEntity hearingEntity = hearings.getFirst();
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.setClosed(false);
        courtCase.setHearings(hearings);
        courtCase.setId(1);
        stubCaseToClose(courtCase);

        EventEntity olderCloseEvent = createEventWithHandlerType(1L, 2, "Case closed", hearingEntity, 218);
        EventEntity latestCloseEvent = createEventWithHandlerType(2L, 3, "Archive Case", hearingEntity, 79);
        EventEntity latestNonCloseEvent = createEventWithHandlerType(3L, 4, "Other event", hearingEntity, 50);
        when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(new ArrayList<>(List.of(olderCloseEvent, latestNonCloseEvent, latestCloseEvent)));

        LocalDate retentionDate = stubRetentionCreation(courtCase);

        // when
        closeOldCasesProcessor.closeCases(2);

        // then
        assertTrue(courtCase.getClosed());
        assertEquals(latestCloseEvent.getCreatedDateTime(), courtCase.getCaseClosedTimestamp());
        verifyRetentionCreated(courtCase, retentionDate, RetentionConfidenceCategoryEnum.AGED_CASE_CASE_CLOSED);
    }

    @Test
    void closeCases_shouldUseCaseCreatedDate_whenThereAreNoCurrentEventsAndNoHearings() {
        // given
        CourtCaseEntity courtCase = new CourtCaseEntity();
        OffsetDateTime createdDate = OffsetDateTime.of(2018, 6, 30, 10, 0, 0, 0, ZoneOffset.UTC);
        courtCase.setId(1);
        courtCase.setClosed(false);
        courtCase.setCreatedDateTime(createdDate);
        stubCaseToClose(courtCase);

        when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(List.of());
        LocalDate retentionDate = stubRetentionCreation(courtCase);

        // when
        closeOldCasesProcessor.closeCases(2);

        // then
        assertTrue(courtCase.getClosed());
        assertEquals(createdDate, courtCase.getCaseClosedTimestamp());
        verifyRetentionCreated(courtCase, retentionDate, RetentionConfidenceCategoryEnum.AGED_CASE_CASE_CREATION_CLOSED);
    }

    @Test
    void closeCases_shouldUseLatestHearingDate_whenThereAreNoCurrentEventsOrMedia() {
        // given
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(2);
        CourtCaseEntity courtCase = hearings.getFirst().getCourtCase();
        courtCase.setId(1);
        courtCase.setClosed(false);
        hearings.forEach(hearing -> hearing.setCourtCase(courtCase));
        hearings.get(0).setHearingDate(LocalDate.of(2018, 6, 28));
        hearings.get(1).setHearingDate(LocalDate.of(2018, 6, 30));
        courtCase.setHearings(hearings);
        stubCaseToClose(courtCase);

        when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(List.of());
        when(findCurrentEntitiesHelper.getCurrentMedia(courtCase)).thenReturn(List.of());
        LocalDate retentionDate = stubRetentionCreation(courtCase);
        OffsetDateTime expectedCaseClosedTimestamp = OffsetDateTime.of(LocalDate.of(2018, 6, 30).atStartOfDay(), ZoneOffset.UTC);

        // when
        closeOldCasesProcessor.closeCases(2);

        // then
        assertTrue(courtCase.getClosed());
        assertEquals(expectedCaseClosedTimestamp, courtCase.getCaseClosedTimestamp());
        verifyRetentionCreated(courtCase, retentionDate, RetentionConfidenceCategoryEnum.AGED_CASE_MAX_HEARING_CLOSED);
    }

    @Test
    void closeCases_shouldUseLatestMediaCreatedDate_whenThereAreNoCurrentEventsAndMediaExists() {
        // given
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);
        HearingEntity hearingEntity = hearings.getFirst();
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.setId(1);
        courtCase.setClosed(false);
        courtCase.setHearings(hearings);
        stubCaseToClose(courtCase);

        List<MediaEntity> testMedia = createTestMedia(hearingEntity);
        when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(List.of());
        when(findCurrentEntitiesHelper.getCurrentMedia(courtCase)).thenReturn(testMedia);
        LocalDate retentionDate = stubRetentionCreation(courtCase);
        OffsetDateTime expectedCaseClosedTimestamp = testMedia.get(1).getCreatedDateTime();

        // when
        closeOldCasesProcessor.closeCases(2);

        // then
        assertTrue(courtCase.getClosed());
        assertEquals(expectedCaseClosedTimestamp, courtCase.getCaseClosedTimestamp());
        verifyRetentionCreated(courtCase, retentionDate, RetentionConfidenceCategoryEnum.AGED_CASE_MAX_MEDIA_CLOSED);
    }

    private void stubCaseToClose(CourtCaseEntity courtCase) {
        when(caseRepository.findOpenCasesToClose(any(), any())).thenReturn(List.of(courtCase.getId()));
        when(caseService.getCourtCaseById(courtCase.getId())).thenReturn(courtCase);
    }

    private LocalDate stubRetentionCreation(CourtCaseEntity courtCase) {
        CaseRetentionEntity caseRetention = createRetentionEntity(courtCase, userAccountEntity);
        LocalDate retentionDate = LocalDate.of(2024, 10, 1);
        when(retentionApi.createRetention(any(), any(), any(), any(), any(), any(), any())).thenReturn(caseRetention);
        when(retentionDateHelper.getRetentionDateForPolicy(courtCase, RetentionPolicyEnum.DEFAULT)).thenReturn(retentionDate);
        return retentionDate;
    }

    private void verifyRetentionCreated(CourtCaseEntity courtCase, LocalDate retentionDate,
                                        RetentionConfidenceCategoryEnum expectedRetentionConfidenceCategory) {
        verify(retentionApi).createRetention(
            eq(RetentionPolicyEnum.DEFAULT),
            any(),
            eq(courtCase),
            eq(retentionDate),
            eq(userAccountEntity),
            eq(CaseRetentionStatus.PENDING),
            eq(expectedRetentionConfidenceCategory)
        );
    }

    public static CaseRetentionEntity createRetentionEntity(CourtCaseEntity courtCase, UserAccountEntity userAccount) {
        CaseRetentionEntity caseRetention = new CaseRetentionEntity();
        caseRetention.setCourtCase(courtCase);
        caseRetention.setLastModifiedBy(userAccount);
        caseRetention.setCreatedBy(userAccount);
        caseRetention.setSubmittedBy(userAccount);
        return caseRetention;
    }

    /**
     * Creates test events for the given hearing, excluding event handler types 79 and 218
     * (which are close events as defined in application.yaml: close-events: ${RETENTION_CLOSE_EVENTS:79,218}).
     */
    private List<EventEntity> createTestEventsExcludingCloseEventTypes(HearingEntity hearingEntity) {
        List<EventEntity> events = new ArrayList<>();

        // Create event with type 1 (not a close event)
        EventEntity event1 = createEventWithHandlerType(1L, 1, "Event 1 text", hearingEntity, 1);
        events.add(event1);

        // Create event with type 50 (not a close event)
        EventEntity event2 = createEventWithHandlerType(2L, 2, "Event 2 text", hearingEntity, 50);
        events.add(event2);

        // Create event with type 100 (not a close event)
        EventEntity event3 = createEventWithHandlerType(3L, 3, "Event 3 text", hearingEntity, 100);
        events.add(event3);

        return events;
    }

    /**
     * Creates test media for the given hearing.
     */
    private List<MediaEntity> createTestMedia(HearingEntity hearingEntity) {
        List<MediaEntity> mediaList = new ArrayList<>();

        // Create media with channel 1
        MediaEntity media1 = new MediaEntity();
        OffsetDateTime startTime = OffsetDateTime.of(hearingEntity.getHearingDate(), LocalTime.of(9, 0), ZoneOffset.UTC);
        media1.setStart(startTime);
        media1.setEnd(startTime.plusHours(2));
        media1.setChannel(1);
        media1.setHearings(new HashSet<>(List.of(hearingEntity)));
        media1.setCourtroom(hearingEntity.getCourtroom());
        media1.setId(1L);
        media1.setIsCurrent(true);
        media1.setCreatedDateTime(startTime.minusMinutes(30));
        mediaList.add(media1);

        // Create media with channel 2
        MediaEntity media2 = new MediaEntity();
        media2.setStart(startTime.plusHours(3));
        media2.setEnd(startTime.plusHours(5));
        media2.setChannel(2);
        media2.setHearings(new HashSet<>(List.of(hearingEntity)));
        media2.setCourtroom(hearingEntity.getCourtroom());
        media2.setId(2L);
        media2.setIsCurrent(true);
        media2.setCreatedDateTime(startTime.plusHours(2).plusMinutes(30));
        mediaList.add(media2);

        return mediaList;
    }

    /**
     * Helper method to create an EventEntity with a specific event handler type.
     */
    private EventEntity createEventWithHandlerType(Long eventId, Integer internalEventId, String eventText,
                                                   HearingEntity hearingEntity, Integer eventHandlerTypeId) {
        EventEntity event = new EventEntity();
        event.setId(eventId);
        event.setEventId(internalEventId);
        event.setEventText(eventText);
        event.setTimestamp(OffsetDateTime.now().minusDays(1));
        event.setCreatedDateTime(OffsetDateTime.of(hearingEntity.getHearingDate(), LocalTime.of(10, 0), ZoneOffset.UTC).plusDays(internalEventId));
        event.setHearingEntities(new HashSet<>(List.of(hearingEntity)));
        event.setCourtroom(hearingEntity.getCourtroom());
        event.setIsCurrent(true);

        // Create EventHandlerEntity with the specified type
        EventHandlerEntity eventHandler = new EventHandlerEntity();
        eventHandler.setId(eventHandlerTypeId);
        eventHandler.setEventName("Event Type " + eventHandlerTypeId);
        eventHandler.setType(String.valueOf(eventHandlerTypeId));
        eventHandler.setActive(true);

        event.setEventType(eventHandler);
        return event;
    }

}
