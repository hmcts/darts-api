package uk.gov.hmcts.darts.retention.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
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
import java.time.ZoneId;
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

    private RetentionService retentionService;

    public static final String FIXED_DATE_TIME = "2024-01-01T00:00:00Z";

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse(FIXED_DATE_TIME),
                                  ZoneId.of("UTC"));

        retentionService = new RetentionServiceImpl(caseRetentionRepository,
                                                    retentionConfidenceCategoryMapperRepository,
                                                    caseRepository,
                                                    retentionMapper,
                                                    clock);
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

}
