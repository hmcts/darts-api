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

    public static final int BATCH_SIZE = 2;
    public static final int CASE_1_ID = 22;
    public static final int CASE_2_ID = 23;
    public static final int CASE_DOCUMENT_GENERATION_DAYS = 5;

    @Mock
    CaseRepository caseRepository;
    @Mock
    GenerateCaseDocumentSingleCaseProcessor singleCaseProcessor;
    @Mock
    CurrentTimeHelper currentTimeHelper;

    GenerateCaseDocumentBatchProcessorImpl batchProcessor;

    @Mock
    CourtCaseEntity case1;
    @Mock
    CourtCaseEntity case2;

    @BeforeEach
    void setup() {
        batchProcessor = new GenerateCaseDocumentBatchProcessorImpl(
            BATCH_SIZE,
            caseRepository,
            singleCaseProcessor,
            currentTimeHelper
        );

        when(case1.getId()).thenReturn(CASE_1_ID);
        when(case2.getId()).thenReturn(CASE_2_ID);
    }

    @Test
    void testBatchGenerationOfCaseDocument() {
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());
        when(caseRepository.findCasesNeedingCaseDocumentGenerated(any(), eq(Limit.of(BATCH_SIZE))))
            .thenReturn(List.of(case1, case2));
        
        batchProcessor.processGenerateCaseDocument(BATCH_SIZE);

        verify(singleCaseProcessor, times(2)).processGenerateCaseDocument(any());
        verify(singleCaseProcessor).processGenerateCaseDocument(CASE_1_ID);
        verify(singleCaseProcessor).processGenerateCaseDocument(CASE_2_ID);
    }

    @Test
    void testExceptionIsHandledAndProcessingContinues() {
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());
        when(caseRepository.findCasesNeedingCaseDocumentGenerated(any(), eq(Limit.of(BATCH_SIZE))))
            .thenReturn(List.of(case1, case2));
        doThrow(RuntimeException.class).when(singleCaseProcessor).processGenerateCaseDocument(CASE_1_ID);

        batchProcessor.processGenerateCaseDocument(BATCH_SIZE);

        verify(singleCaseProcessor).processGenerateCaseDocument(CASE_2_ID);
    }

}