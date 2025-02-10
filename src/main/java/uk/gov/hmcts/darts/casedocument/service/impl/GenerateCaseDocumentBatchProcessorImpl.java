package uk.gov.hmcts.darts.casedocument.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentProcessor;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
@Slf4j
@Component
public class GenerateCaseDocumentBatchProcessorImpl implements GenerateCaseDocumentProcessor {

    @Value("${darts.case-document.generation-days}")
    private final int caseDocumentGenerationDays;
    private final CaseRepository caseRepository;
    private final GenerateCaseDocumentSingleCaseProcessor singleCaseProcessor;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public void processGenerateCaseDocument(int batchSize) {

        OffsetDateTime caseClosedBeforeTimestamp = currentTimeHelper.currentOffsetDateTime().minusDays(caseDocumentGenerationDays);
        var casesIdsNeedingCaseDocumentGenerated = caseRepository.findCasesIdsNeedingCaseDocumentGenerated(caseClosedBeforeTimestamp, Limit.of(batchSize));
        log.debug("Found {} casesIdsNeedingCaseDocumentGenerated needing case document out of a batch size {}", casesIdsNeedingCaseDocumentGenerated.size(),
                  batchSize);
        for (var courtCaseId : casesIdsNeedingCaseDocumentGenerated) {
            try {
                singleCaseProcessor.processGenerateCaseDocument(courtCaseId);
            } catch (Exception exc) {
                log.error("Error generating case document for case id '{}'", courtCaseId, exc);
            }
        }
    }
}