package uk.gov.hmcts.darts.retention.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.retention.service.ResetRetentionAfterDocumentChangeService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResetRetentionAfterDocumentChangeServiceImpl implements ResetRetentionAfterDocumentChangeService {

    private final CaseRepository caseRepository;

    @Override
    public void updateRetentionAfterDocumentChange(int batchSize) {
        resetRetentionForMedia(batchSize);
        resetRetentionForTranscription(batchSize);
        resetRetentionForAnnotation(batchSize);
        resetRetentionForCaseDocument(batchSize);
    }

    private void resetRetentionForMedia(int batchSize) {
        // find all cases that have had media uploaded after the case retention retainUntilAppliedOn date and case retention is not pending
        List<Integer> cases = caseRepository.findCaseIdsWithMediaUploadedAfterRetentionAppliedAndRetentionNotPending(Limit.of(batchSize));
        log.info("Found {} cases with media uploaded after retention applied and retention not pending", cases.size());
        resetCaseRetention(cases);
    }

    private void resetRetentionForTranscription(int batchSize) {
        List<Integer> cases = caseRepository.findCaseIdsWithTranscriptionsUploadedAfterRetentionAppliedAndRetentionNotPending(Limit.of(batchSize));
        log.info("Found {} cases with transcriptions uploaded after retention applied and retention not pending", cases.size());
        resetCaseRetention(cases);
    }

    private void resetRetentionForAnnotation(int batchSize) {
        List<Integer> cases = caseRepository.findCaseIdsWithAnnotationsUploadedAfterRetentionAppliedAndRetentionNotPending(Limit.of(batchSize));
        log.info("Found {} cases with annotations uploaded after retention applied and retention not pending", cases.size());
        resetCaseRetention(cases);
    }

    private void resetRetentionForCaseDocument(int batchSize) {
        List<Integer> cases = caseRepository.findCaseIdsWithCaseDocumentsUploadedAfterRetentionAppliedAndRetentionNotPending(Limit.of(batchSize));
        log.info("Found {} cases with case documents uploaded after retention applied and retention not pending", cases.size());
        resetCaseRetention(cases);
    }


    private void resetCaseRetention(List<Integer> caseIds) {
        if (caseIds.isEmpty()) {
            return;
        }

        int updatedCases = caseRepository.resetRetentionProcessingForCases(caseIds);
        log.info("Reset retention processing for {} cases", updatedCases);
    }
}
