package uk.gov.hmcts.darts.casedocument.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateCaseDocumentBatchProcessorImplTest {

    private static final int BATCH_SIZE = 2;
    private static final int CASE_1_ID = 22;
    private static final int CASE_2_ID = 23;

    @Mock
    private CaseRepository caseRepository;
    @Mock
    private GenerateCaseDocumentSingleCaseProcessor singleCaseProcessor;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private GenerateCaseDocumentBatchProcessorImpl batchProcessor;

    @Mock
    private CourtCaseEntity case1;
    @Mock
    private CourtCaseEntity case2;

    @BeforeEach
    void setup() {
        batchProcessor = new GenerateCaseDocumentBatchProcessorImpl(
            BATCH_SIZE,
            caseRepository,
            singleCaseProcessor,
            currentTimeHelper
        );
    }

    @Test
    void testBatchGenerationOfCaseDocument() {
        // given
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());
        when(caseRepository.findCasesIdsNeedingCaseDocumentGenerated(any(), eq(Limit.of(BATCH_SIZE))))
            .thenReturn(List.of(CASE_1_ID, CASE_2_ID));

        // when
        batchProcessor.processGenerateCaseDocument(BATCH_SIZE);

        // then
        verify(singleCaseProcessor, times(2)).processGenerateCaseDocument(any());
        verify(singleCaseProcessor).processGenerateCaseDocument(CASE_1_ID);
        verify(singleCaseProcessor).processGenerateCaseDocument(CASE_2_ID);
    }

    @Test
    void testExceptionIsHandledAndProcessingContinues() {
        // given
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());
        when(caseRepository.findCasesIdsNeedingCaseDocumentGenerated(any(), eq(Limit.of(BATCH_SIZE))))
            .thenReturn(List.of(CASE_1_ID, CASE_2_ID));
        doThrow(RuntimeException.class).when(singleCaseProcessor).processGenerateCaseDocument(CASE_1_ID);

        // when
        batchProcessor.processGenerateCaseDocument(BATCH_SIZE);

        // then
        verify(singleCaseProcessor).processGenerateCaseDocument(CASE_2_ID);
    }

}