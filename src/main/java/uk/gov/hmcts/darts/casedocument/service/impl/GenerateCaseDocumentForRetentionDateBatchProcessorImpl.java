package uk.gov.hmcts.darts.casedocument.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentForRetentionDateProcessor;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
@Slf4j
@Service
public class GenerateCaseDocumentForRetentionDateBatchProcessorImpl implements GenerateCaseDocumentForRetentionDateProcessor {

    @Value("${darts.case-document.expiry-days}")
    private int caseDocumentExpiryDays;

    private final CaseRepository caseRepository;
    private final GenerateCaseDocumentSingleCaseProcessor singleCaseProcessor;
    private final CurrentTimeHelper currentTimeHelper;

    @Override
    public void processGenerateCaseDocumentForRetentionDate(int batchSize) {
        OffsetDateTime currentTimestamp = currentTimeHelper.currentOffsetDateTime();
        OffsetDateTime caseRetailUntilTimestamp = currentTimestamp.plusDays(caseDocumentExpiryDays);
        OffsetDateTime caseDocumentCreatedAfterTimestamp = currentTimestamp.minusDays(caseDocumentExpiryDays);
        var cases = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(caseRetailUntilTimestamp,
                                                                                          caseDocumentCreatedAfterTimestamp,
                                                                                          Pageable.ofSize(batchSize));
        for (var courtCase : cases) {
            try {
                if (!courtCase.isRetentionUpdated()) {
                    singleCaseProcessor.processGenerateCaseDocument(courtCase.getId());
                } else {
                    log.info("Retention calculation is In Progress for case id {}", courtCase.getId());
                }
            } catch (Exception exc) {
                log.error("Error generating retention date case document for case id '{}'", courtCase.getId(), exc);
            }
        }
    }


}
