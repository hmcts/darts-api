package uk.gov.hmcts.darts.retention.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.retention.service.ResetRetentionAfterDocumentChangeService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetRetentionAfterDocumentChangeServiceImplTest {

    @Mock
    private CaseRepository caseRepository;

    private ResetRetentionAfterDocumentChangeService resetRetentionAfterDocumentChangeService;

    @BeforeEach
    void setUp() {
        resetRetentionAfterDocumentChangeService = new ResetRetentionAfterDocumentChangeServiceImpl(caseRepository);
    }

    @Test
    void updateRetentionAfterDocumentChange_resetCaseRetentionProcessing_casesWithMediaUploadedAfterRetentionApplied() {
        // given
        int batchSize = 100;
        List<Integer> caseIds = List.of(1, 2);
        stubCaseIdsForAllDocumentTypes(batchSize, caseIds, List.of(), List.of(), List.of());
        when(caseRepository.resetRetentionProcessingForCases(caseIds)).thenReturn(caseIds.size());

        // when
        resetRetentionAfterDocumentChangeService.updateRetentionAfterDocumentChange(batchSize);

        // then
        verifyDocumentTypeQueries(batchSize);
        verify(caseRepository).resetRetentionProcessingForCases(caseIds);
    }

    @Test
    void updateRetentionAfterDocumentChange_resetCaseRetentionProcessing_casesWithTranscriptionsUploadedAfterRetentionApplied() {
        // given
        int batchSize = 100;
        List<Integer> caseIds = List.of(3, 4);
        stubCaseIdsForAllDocumentTypes(batchSize, List.of(), caseIds, List.of(), List.of());
        when(caseRepository.resetRetentionProcessingForCases(caseIds)).thenReturn(caseIds.size());

        // when
        resetRetentionAfterDocumentChangeService.updateRetentionAfterDocumentChange(batchSize);

        // then
        verifyDocumentTypeQueries(batchSize);
        verify(caseRepository).resetRetentionProcessingForCases(caseIds);
    }

    @Test
    void updateRetentionAfterDocumentChange_resetCaseRetentionProcessing_casesWithAnnotationsUploadedAfterRetentionApplied() {
        // given
        int batchSize = 100;
        List<Integer> caseIds = List.of(5, 6);
        stubCaseIdsForAllDocumentTypes(batchSize, List.of(), List.of(), caseIds, List.of());
        when(caseRepository.resetRetentionProcessingForCases(caseIds)).thenReturn(caseIds.size());

        // when
        resetRetentionAfterDocumentChangeService.updateRetentionAfterDocumentChange(batchSize);

        // then
        verifyDocumentTypeQueries(batchSize);
        verify(caseRepository).resetRetentionProcessingForCases(caseIds);
    }

    @Test
    void updateRetentionAfterDocumentChange_resetCaseRetentionProcessing_casesWithCaseDocumentsUploadedAfterRetentionApplied() {
        // given
        int batchSize = 100;
        List<Integer> caseIds = List.of(7, 8);
        stubCaseIdsForAllDocumentTypes(batchSize, List.of(), List.of(), List.of(), caseIds);
        when(caseRepository.resetRetentionProcessingForCases(caseIds)).thenReturn(caseIds.size());

        // when
        resetRetentionAfterDocumentChangeService.updateRetentionAfterDocumentChange(batchSize);

        // then
        verifyDocumentTypeQueries(batchSize);
        verify(caseRepository).resetRetentionProcessingForCases(caseIds);
    }

    @Test
    void updateRetentionAfterDocumentChange_doNotResetCaseRetentionProcessing_noCasesWithDocumentsUploadedAfterRetentionApplied() {
        // given
        int batchSize = 100;
        stubCaseIdsForAllDocumentTypes(batchSize, List.of(), List.of(), List.of(), List.of());

        // when
        resetRetentionAfterDocumentChangeService.updateRetentionAfterDocumentChange(batchSize);

        // then
        verifyDocumentTypeQueries(batchSize);
        verify(caseRepository, never()).resetRetentionProcessingForCases(anyList());
    }

    private void stubCaseIdsForAllDocumentTypes(int batchSize,
                                                List<Integer> mediaCaseIds,
                                                List<Integer> transcriptionCaseIds,
                                                List<Integer> annotationCaseIds,
                                                List<Integer> caseDocumentCaseIds) {
        Limit limit = Limit.of(batchSize);
        when(caseRepository.findCaseIdsWithMediaUploadedAfterRetentionAppliedAndRetentionNotPending(limit)).thenReturn(mediaCaseIds);
        when(caseRepository.findCaseIdsWithTranscriptionsUploadedAfterRetentionAppliedAndRetentionNotPending(limit)).thenReturn(transcriptionCaseIds);
        when(caseRepository.findCaseIdsWithAnnotationsUploadedAfterRetentionAppliedAndRetentionNotPending(limit)).thenReturn(annotationCaseIds);
        when(caseRepository.findCaseIdsWithCaseDocumentsUploadedAfterRetentionAppliedAndRetentionNotPending(limit)).thenReturn(caseDocumentCaseIds);
    }

    private void verifyDocumentTypeQueries(int batchSize) {
        Limit limit = Limit.of(batchSize);
        verify(caseRepository).findCaseIdsWithMediaUploadedAfterRetentionAppliedAndRetentionNotPending(limit);
        verify(caseRepository).findCaseIdsWithTranscriptionsUploadedAfterRetentionAppliedAndRetentionNotPending(limit);
        verify(caseRepository).findCaseIdsWithAnnotationsUploadedAfterRetentionAppliedAndRetentionNotPending(limit);
        verify(caseRepository).findCaseIdsWithCaseDocumentsUploadedAfterRetentionAppliedAndRetentionNotPending(limit);
    }
}


