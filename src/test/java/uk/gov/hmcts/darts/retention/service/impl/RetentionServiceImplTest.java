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
import uk.gov.hmcts.darts.common.entity.MediaEntity;
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
import java.time.Instant;
import java.time.Period;
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
        Period daysBetweenEvents = Period.ofDays(10);
        retentionService = new RetentionServiceImpl(caseRetentionRepository,
                                                    retentionConfidenceCategoryMapperRepository,
                                                    caseRepository,
                                                    retentionMapper,
                                                    clock,
                                                    findCurrentEntitiesHelper,
                                                    closeEvents,
                                                    daysBetweenEvents);
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
            when(retentionConfidenceCategoryMapperRepository.findByConfidenceCategory(eq(RetentionConfidenceCategoryEnum.CASE_CLOSED)))
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
                .confidenceCategory(RetentionConfidenceCategoryEnum.CASE_CLOSED)
                .confidenceScore(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED)
                .confidenceReason(RetentionConfidenceReasonEnum.CASE_CLOSED)
                .build();
        }

    }

    @Nested
    class GetRetentionConfidenceCategoryForMediaTest {
        @Test
        void shouldReturnMaxMediaClosed_whenMediaExists() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            MediaEntity media1 = new MediaEntity();
            media1.setCreatedDateTime(java.time.OffsetDateTime.parse("2024-01-01T10:00:00Z"));
            MediaEntity media2 = new MediaEntity();
            media2.setCreatedDateTime(java.time.OffsetDateTime.parse("2024-01-02T10:00:00Z"));
            List<MediaEntity> mediaList = new ArrayList<>(List.of(media1, media2));
            when(findCurrentEntitiesHelper.getCurrentMedia(courtCase)).thenReturn(mediaList);

            var result = retentionService.getRetentionConfidenceCategoryForMedia(courtCase);

            assertEquals(RetentionConfidenceCategoryEnum.AGED_CASE_MAX_MEDIA_CLOSED, result);
        }

        @Test
        void shouldReturnMaxHearingClosed_whenNoMediaExists() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            when(findCurrentEntitiesHelper.getCurrentMedia(courtCase)).thenReturn(new ArrayList<>());

            var result = retentionService.getRetentionConfidenceCategoryForMedia(courtCase);

            assertEquals(RetentionConfidenceCategoryEnum.AGED_CASE_MAX_HEARING_CLOSED, result);
        }
    }

    @Nested
    class GetConfidenceCategoryTest {
        @Test
        void getConfidenceCategory_shouldReturnCaseClosed_whenLatestEventIsClosed() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            EventEntity closedEvent = getEvent(1L, "2024-01-01T10:00:00Z", "Case closed");
            List<EventEntity> events = new ArrayList<>(List.of(closedEvent));
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(events);

            var result = retentionService.getConfidenceCategory(courtCase);
            assertEquals(RetentionConfidenceCategoryEnum.CASE_CLOSED, result);
        }

        @Test
        void getConfidenceCategory_shouldReturnCaseClosedWithin_whenLatestClosedEventIsNotLatestButWithinDays() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            EventEntity closedEvent = getEvent(1L, "2024-01-01T10:00:00Z", "Case closed");

            EventEntity otherEvent = getEvent(2L, "2024-01-05T10:00:00Z", "Other event");

            // The list order does not matter, but the timestamps do
            List<EventEntity> events = new ArrayList<>(List.of(closedEvent, otherEvent));
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(events);

            var result = retentionService.getConfidenceCategory(courtCase);
            assertEquals(RetentionConfidenceCategoryEnum.CASE_CLOSED_WITHIN, result);
        }

        private static @NotNull EventEntity getEvent(long id, String text, String eventName) {
            EventEntity eventEntity = new EventEntity();
            eventEntity.setId(id);
            eventEntity.setCreatedDateTime(java.time.OffsetDateTime.parse(text));
            EventHandlerEntity eventHandler = new EventHandlerEntity();
            eventHandler.setEventName(eventName);
            eventEntity.setEventType(eventHandler);
            return eventEntity;
        }

        @Test
        void getConfidenceCategory_shouldReturnMaxEventOutwith_whenLatestClosedEventIsNotLatestAndOutwithDays() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            EventEntity closedEvent = getEvent(1L, "2024-01-01T10:00:00Z", "Case closed");

            EventEntity otherEvent = getEvent(2L, "2024-01-20T10:00:00Z", "Other event");

            // The list order does not matter, but the timestamps do
            List<EventEntity> events = new ArrayList<>(List.of(closedEvent, otherEvent));
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(events);

            var result = retentionService.getConfidenceCategory(courtCase);
            assertEquals(RetentionConfidenceCategoryEnum.MAX_EVENT_OUTWITH, result);
        }

        @Test
        void shouldReturnNull_whenNoEvents() {
            CourtCaseEntity courtCase = new CourtCaseEntity();
            when(findCurrentEntitiesHelper.getCurrentEvents(courtCase)).thenReturn(new ArrayList<>());
            var result = retentionService.getConfidenceCategory(courtCase);
            assertNull(result);
        }
    }
    
}
