package uk.gov.hmcts.darts.casedocument.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentProcessor;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
@Slf4j
public class GenerateCaseDocumentBatchProcessorImpl implements GenerateCaseDocumentProcessor {

    @Value("${darts.case-document.generation-days}")
    private int caseDocumentGenerationDays;

    private final int batchSize;
    private final CaseRepository caseRepository;
    private final GenerateCaseDocumentSingleCaseProcessor singleCaseProcessor;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public void processGenerateCaseDocument() {

        OffsetDateTime caseClosedBeforeTimestamp = currentTimeHelper.currentOffsetDateTime().minusDays(caseDocumentGenerationDays);
        var cases = caseRepository.findCasesNeedingCaseDocumentGenerated(caseClosedBeforeTimestamp, Pageable.ofSize(batchSize));

        for (var courtCase : cases) {
            try {
                singleCaseProcessor.processGenerateCaseDocument(courtCase.getId());
            } catch (Exception exc) {
                log.error("Error generating case document for case id '{}'", courtCase.getId(), exc);
            }
        }
    }

}
