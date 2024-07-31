package uk.gov.hmcts.darts.casedocument.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentForRetentionDateProcessor;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
@Slf4j
public class GenerateCaseDocumentForRetentionDateBatchProcessorImpl implements GenerateCaseDocumentForRetentionDateProcessor {

    @Value("${darts.case-document.expiry-days}")
    private int caseDocumentExpiryDays;

    private final int batchSize;
    private final CaseRepository caseRepository;
    private final GenerateCaseDocumentSingleCaseProcessor singleCaseProcessor;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public void processGenerateCaseDocumentForRetentionDate() {
        OffsetDateTime currentTimestamp = currentTimeHelper.currentOffsetDateTime();
        OffsetDateTime caseRetailUntilTimestamp = currentTimestamp.plusDays(caseDocumentExpiryDays);
        OffsetDateTime caseDocumentCreatedAfterTimestamp = currentTimestamp.minusDays(caseDocumentExpiryDays);
        var cases = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(caseRetailUntilTimestamp,
                                                                                          caseDocumentCreatedAfterTimestamp,
                                                                                          Pageable.ofSize(batchSize));
        for (var courtCase : cases) {
            try {
                if (!courtCase.isRetentionUpdated()) {
                    log.info("Retention calculation is In Progress for case id {}", courtCase.getId());
                    singleCaseProcessor.processGenerateCaseDocument(courtCase.getId());
                    courtCase.setRetentionUpdated(true);
                }
            } catch (Exception exc) {
                log.error("Error generating retention date case document for case id '{}'", courtCase.getId(), exc);
            }
        }
    }


}
