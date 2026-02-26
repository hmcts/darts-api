package uk.gov.hmcts.darts.retention.service.impl;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.cases.helper.FindCurrentEntitiesHelper;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.RetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.RetentionConfidenceCategoryMapperRepository;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.retention.mapper.RetentionMapper;
import uk.gov.hmcts.darts.retention.service.RetentionService;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.RetentionConfidenceCategoryMapperTestData;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetentionServiceImplTest {

    @Mock
    private CaseRetentionRepository caseRetentionRepository;
    @Mock
    private RetentionConfidenceCategoryMapperRepository retentionConfidenceCategoryMapperRepository;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private RetentionMapper retentionMapper;
    @Mock
    private FindCurrentEntitiesHelper findCurrentEntitiesHelper;

    private RetentionService retentionService;

    private static final String FIXED_DATE_TIME = "2024-01-01T00:00:00Z";

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse(FIXED_DATE_TIME),
                                  ZoneId.of("UTC"));
        List<String> closeEvents = List.of("Case closed", "Archive case");
        retentionService = new RetentionServiceImpl(caseRetentionRepository,
                                                    retentionConfidenceCategoryMapperRepository,
                                                    caseRepository,
                                                    retentionMapper,
                                                    clock,
                                                    findCurrentEntitiesHelper,
                                                    closeEvents);
    }

    @Nested
    class GetCaseRetentionsTest {

        private static final int CASE_ID = 1;

        @Test
        void shouldReturnCaseRetentions_whenCaseRetentionsExist() {
            // Given
            var caseRetentionEntity = new CaseRetentionEntity();
            when(caseRetentionRepository.findByCaseId(eq(CASE_ID)))
                .thenReturn(List.of(caseRetentionEntity));

            // When
            var caseRetentions = retentionService.getCaseRetentions(CASE_ID);

            // Then
            verify(retentionMapper).mapToCaseRetention(caseRetentionEntity);
            assertEquals(1, caseRetentions.size());
        }

        @Test
        void shouldReturnEmptyList_whenCaseRetentionsDoNotExist() {
            // Given
            when(caseRetentionRepository.findByCaseId(any()))
                .thenReturn(List.of());

            // When
            var caseRetentions = retentionService.getCaseRetentions(CASE_ID);

            // Then
            verify(retentionMapper, never()).mapToCaseRetention(any());
            assertEquals(0, caseRetentions.size());
        }

    }

    @Nested
    class UpdateCourtCaseConfidenceAttributesForRetentionTest {

        @Captor
        private ArgumentCaptor<CourtCaseEntity> caseEntityCaptor;

        @Test
        void shouldUpdateConfidenceAttributes_whenConfidenceMappingExistsInDB() {
            // Given
            RetentionConfidenceCategoryMapperEntity confidenceMapping = createConfidenceMapping();
            when(retentionConfidenceCategoryMapperRepository.findByConfidenceCategory(eq(RetentionConfidenceCategoryEnum.CASE_CLOSED.getId())))
                .thenReturn(Optional.of(confidenceMapping));

            var courtCaseEntity = new CourtCaseEntity();

            // When
            retentionService.updateCourtCaseConfidenceAttributesForRetention(courtCaseEntity,
                                                                             RetentionConfidenceCategoryEnum.CASE_CLOSED);

            // Then
            verify(caseRepository).save(caseEntityCaptor.capture());
            CourtCaseEntity mutatedCourtCaseToBePersisted = caseEntityCaptor.getValue();

            assertEquals(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED, mutatedCourtCaseToBePersisted.getRetConfScore());
            assertEquals(RetentionConfidenceReasonEnum.CASE_CLOSED, mutatedCourtCaseToBePersisted.getRetConfReason());
            assertEquals(Instant.parse(FIXED_DATE_TIME), mutatedCourtCaseToBePersisted.getRetConfUpdatedTs().toInstant());
        }

        @Test
        void shouldSetNullScoreAndReason_whenConfidenceMappingDoesNotExistInDB() {
            // Given
            when(retentionConfidenceCategoryMapperRepository.findByConfidenceCategory(any()))
                .thenReturn(Optional.empty());

            var courtCaseEntity = new CourtCaseEntity();

            // When
            retentionService.updateCourtCaseConfidenceAttributesForRetention(courtCaseEntity,
                                                                             RetentionConfidenceCategoryEnum.CASE_CLOSED);

            // Then
            verify(caseRepository).save(caseEntityCaptor.capture());
            CourtCaseEntity mutatedCourtCaseToBePersisted = caseEntityCaptor.getValue();

            assertNull(mutatedCourtCaseToBePersisted.getRetConfScore());
            assertNull(mutatedCourtCaseToBePersisted.getRetConfReason());
            assertEquals(Instant.parse(FIXED_DATE_TIME), mutatedCourtCaseToBePersisted.getRetConfUpdatedTs().toInstant());
        }

        private RetentionConfidenceCategoryMapperEntity createConfidenceMapping() {
            RetentionConfidenceCategoryMapperTestData testData = PersistableFactory.getRetentionConfidenceCategoryMapperTestData();

            return testData.someMinimalBuilder()
                .confidenceCategory(RetentionConfidenceCategoryEnum.CASE_CLOSED.getId())
                .confidenceScore(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED)
                .confidenceReason(RetentionConfidenceReasonEnum.CASE_CLOSED)
                .build();
        }

    }

    @Nested
    class GetConfidenceCategoryTest {

        private static final OffsetDateTime DATETIME_2025 = OffsetDateTime.of(2025, 1, 1, 10, 10, 0, 0, UTC);
        private static final String POLICY_A_NAME = "Policy A";
        private static final String SOME_PAST_DATE_TIME = "2000-01-01T00:00:00Z";
        private static final String SOME_FUTURE_DATE_TIME = "2100-01-01T00:00:00Z";

        @Test
        void getConfidenceCategory_shouldReturnCaseClosed_whenLatestEventIsClosed() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            var retentionPolicyTypeEntity1 = createRetentionPolicyType(POLICY_A_NAME, SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME, DATETIME_2025);
            UserAccountEntity testUser = CommonTestDataUtil.createUserAccount();
            CaseRetentionEntity caseRetention = createCaseRetention(courtCase, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
            caseRetention.setRetainUntilAppliedOn(DATETIME_2025);
            caseRetention.setConfidenceCategory(CASE_CLOSED);

            EventEntity closedEvent = getEvent(1L, "2024-01-01T10:00:00Z", "Case closed");
            List<EventEntity> events = new ArrayList<>(List.of(closedEvent));
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(events);

            var result = retentionService.getConfidenceCategory(courtCase, Duration.ofDays(10), caseRetention);
            assertEquals(CASE_CLOSED, result);
        }

        @Test
        void getConfidenceCategory_shouldReturnCaseClosedWithin_whenLatestClosedEventIsNotLatestButWithinDays() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            var retentionPolicyTypeEntity1 = createRetentionPolicyType(POLICY_A_NAME, SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME, DATETIME_2025);
            UserAccountEntity testUser = CommonTestDataUtil.createUserAccount();
            CaseRetentionEntity caseRetention = createCaseRetention(courtCase, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
            caseRetention.setRetainUntilAppliedOn(DATETIME_2025);
            caseRetention.setConfidenceCategory(CASE_CLOSED_WITHIN);
            EventEntity closedEvent = getEvent(1L, "2024-01-01T10:00:00Z", "Case closed");

            EventEntity otherEvent = getEvent(2L, "2024-01-05T10:00:00Z", "Other event");

            List<EventEntity> events = new ArrayList<>(List.of(closedEvent, otherEvent));
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(events);

            var result = retentionService.getConfidenceCategory(courtCase, Duration.ofDays(10), caseRetention);
            assertEquals(CASE_CLOSED_WITHIN, result);
        }

        @Test
        void getConfidenceCategory_shouldReturnMaxEventOutwith_whenLatestClosedEventIsNotLatestAndOutwithDays() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            var retentionPolicyTypeEntity1 = createRetentionPolicyType(POLICY_A_NAME, SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME, DATETIME_2025);
            UserAccountEntity testUser = CommonTestDataUtil.createUserAccount();
            CaseRetentionEntity caseRetention = createCaseRetention(courtCase, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
            caseRetention.setRetainUntilAppliedOn(DATETIME_2025);
            caseRetention.setConfidenceCategory(CASE_CLOSED);
            EventEntity closedEvent = getEvent(1L, "2024-01-01T10:00:00Z", "Case closed");

            EventEntity otherEvent = getEvent(2L, "2024-01-20T10:00:00Z", "Other event");

            List<EventEntity> events = new ArrayList<>(List.of(closedEvent, otherEvent));
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(events);

            var result = retentionService.getConfidenceCategory(courtCase, Duration.ofDays(10), caseRetention);
            assertEquals(MAX_EVENT_OUTWITH, result);
        }

        @Test
        void getConfidenceCategory_shouldReturnCaseClosedWithin_whenLatestEventIsLogEntryAndNonLogEventWithinDays() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            EventEntity closedEvent = getEvent(1L, "2024-01-01T10:00:00Z", "Case closed");
            EventEntity logEvent = getEvent(2L, "2024-01-05T10:00:00Z", "Log event");
            logEvent.setLogEntry(true);
            EventEntity nonLogEvent = getEvent(3L, "2024-01-04T10:00:00Z", "Other event");
            nonLogEvent.setLogEntry(false);
            List<EventEntity> events = new ArrayList<>(List.of(closedEvent, logEvent, nonLogEvent));
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(events);

            var result = retentionService.getConfidenceCategory(courtCase, Duration.ofDays(10));
            assertEquals(RetentionConfidenceCategoryEnum.CASE_CLOSED_WITHIN, result);
        }

        @Test
        void getConfidenceCategory_shouldReturnMaxEventOutwith_whenLatestEventIsLogEntryAndNonLogEventOutwithDays() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            var retentionPolicyTypeEntity1 = createRetentionPolicyType(POLICY_A_NAME, SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME, DATETIME_2025);
            UserAccountEntity testUser = CommonTestDataUtil.createUserAccount();
            CaseRetentionEntity caseRetention = createCaseRetention(courtCase, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
            caseRetention.setRetainUntilAppliedOn(DATETIME_2025);
            caseRetention.setConfidenceCategory(CASE_CLOSED);
            EventEntity closedEvent = getEvent(1L, "2024-01-01T10:00:00Z", "Case closed");
            EventEntity logEvent = getEvent(2L, "2024-01-20T10:00:00Z", "Log event");
            logEvent.setLogEntry(true);
            EventEntity nonLogEvent = getEvent(3L, "2024-01-15T10:00:00Z", "Other event");
            nonLogEvent.setLogEntry(false);
            List<EventEntity> events = new ArrayList<>(List.of(closedEvent, logEvent, nonLogEvent));
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(events);

            var result = retentionService.getConfidenceCategory(courtCase, Duration.ofDays(10), caseRetention);
            assertEquals(MAX_EVENT_OUTWITH, result);
        }

        @Test
        void shouldReturnNull_whenNoEvents() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            var retentionPolicyTypeEntity1 = createRetentionPolicyType(POLICY_A_NAME, SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME, DATETIME_2025);
            UserAccountEntity testUser = CommonTestDataUtil.createUserAccount();
            CaseRetentionEntity caseRetention = createCaseRetention(courtCase, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
            caseRetention.setRetainUntilAppliedOn(DATETIME_2025);
            caseRetention.setConfidenceCategory(CASE_CLOSED);
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(new ArrayList<>());
            var result = retentionService.getConfidenceCategory(courtCase, Duration.ofDays(10), caseRetention);
            assertNull(result);
        }

        @Test
        void getConfidenceCategory_shouldReturnManualOverride_whenRetConfReasonIsManualOverride() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            var retentionPolicyTypeEntity1 = createRetentionPolicyType(POLICY_A_NAME, SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME, DATETIME_2025);
            UserAccountEntity testUser = CommonTestDataUtil.createUserAccount();
            CaseRetentionEntity caseRetention = createCaseRetention(courtCase, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
            caseRetention.setRetainUntilAppliedOn(DATETIME_2025);
            caseRetention.setConfidenceCategory(RetentionConfidenceCategoryEnum.MANUAL_OVERRIDE);
            courtCase.setRetConfReason(RetentionConfidenceReasonEnum.MANUAL_OVERRIDE);
            EventEntity event = getEvent(1L, "2024-01-01T10:00:00Z", "Other event");
            List<EventEntity> events = new ArrayList<>(List.of(event));
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(events);

            var result = retentionService.getConfidenceCategory(courtCase, Duration.ofDays(10), caseRetention);
            assertEquals(RetentionConfidenceCategoryEnum.MANUAL_OVERRIDE, result);
        }

        @Test
        void getConfidenceCategory_shouldReturnEnum_whenNoClosedEventAndRetConfReasonIsValidEnum() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            var retentionPolicyTypeEntity1 = createRetentionPolicyType(POLICY_A_NAME, SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME, DATETIME_2025);
            UserAccountEntity testUser = CommonTestDataUtil.createUserAccount();
            CaseRetentionEntity caseRetention = createCaseRetention(courtCase, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
            caseRetention.setRetainUntilAppliedOn(DATETIME_2025);
            caseRetention.setConfidenceCategory(CASE_CLOSED);
            courtCase.setRetConfReason(RetentionConfidenceReasonEnum.CASE_CLOSED);
            EventEntity event = getEvent(1L, "2024-01-01T10:00:00Z", "Other event");
            List<EventEntity> events = new ArrayList<>(List.of(event));
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(events);

            var result = retentionService.getConfidenceCategory(courtCase, Duration.ofDays(10), caseRetention);
            assertEquals(CASE_CLOSED, result);
        }

        @Test
        void getConfidenceCategory_shouldReturnAgedCase_whenNoClosedEventAndRetConfReasonIsNull() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            var retentionPolicyTypeEntity1 = createRetentionPolicyType(POLICY_A_NAME, SOME_PAST_DATE_TIME, SOME_FUTURE_DATE_TIME, DATETIME_2025);
            UserAccountEntity testUser = CommonTestDataUtil.createUserAccount();
            CaseRetentionEntity caseRetention = createCaseRetention(courtCase, retentionPolicyTypeEntity1, DATETIME_2025, COMPLETE, testUser);
            caseRetention.setRetainUntilAppliedOn(DATETIME_2025);
            caseRetention.setConfidenceCategory(CASE_CLOSED);
            courtCase.setRetConfReason(null);
            EventEntity event = getEvent(1L, "2024-01-01T10:00:00Z", "Other event");
            List<EventEntity> events = new ArrayList<>(List.of(event));
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(events);

            var result = retentionService.getConfidenceCategory(courtCase, Duration.ofDays(10), caseRetention);
            assertEquals(CASE_CLOSED, result);
        }

        private static @NotNull EventEntity getEvent(long id, String text, String eventName) {
            EventEntity eventEntity = new EventEntity();
            eventEntity.setId(id);
            eventEntity.setCreatedDateTime(java.time.OffsetDateTime.parse(text));
            eventEntity.setTimestamp(java.time.OffsetDateTime.parse(text)); // Ensure timestamp is set
            EventHandlerEntity eventHandler = new EventHandlerEntity();
            eventHandler.setEventName(eventName);
            eventEntity.setEventType(eventHandler);
            return eventEntity;
        }

    }

}
