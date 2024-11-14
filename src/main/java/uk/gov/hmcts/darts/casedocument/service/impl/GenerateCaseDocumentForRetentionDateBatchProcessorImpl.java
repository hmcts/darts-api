package uk.gov.hmcts.darts.casedocument.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentForRetentionDateProcessor;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class GenerateCaseDocumentForRetentionDateBatchProcessorImpl implements GenerateCaseDocumentForRetentionDateProcessor {

    @Value("${darts.case-document.expiry-days}")
    private int caseDocumentExpiryDays;

    private final CaseRepository caseRepository;
    private final GenerateCaseDocumentSingleCaseProcessor singleCaseProcessor;
    private final CurrentTimeHelper currentTimeHelper;
    private final CaseService caseService;

    @Override
    public void processGenerateCaseDocumentForRetentionDate(int batchSize) {
        OffsetDateTime currentTimestamp = currentTimeHelper.currentOffsetDateTime();
        OffsetDateTime caseRetailUntilTimestamp = currentTimestamp.plusDays(caseDocumentExpiryDays);
        OffsetDateTime caseDocumentCreatedAfterTimestamp = currentTimestamp.minusDays(caseDocumentExpiryDays);
        List<Integer> casesIds = caseRepository.findCasesNeedingCaseDocumentForRetentionDateGeneration(caseRetailUntilTimestamp,
                                                                                                       caseDocumentCreatedAfterTimestamp,
                                                                                                       Limit.of(batchSize));
        log.debug("Found {} cases needing case document based on retention out of a batch size {}", casesIds.size(), batchSize);
        for (Integer courtCaseId : casesIds) {
            try {
                CourtCaseEntity courtCase = caseService.getCourtCaseById(courtCaseId);
                if (!courtCase.isRetentionUpdated()) {
                    singleCaseProcessor.processGenerateCaseDocument(courtCaseId);
                } else {
                    log.info("Retention calculation is In Progress for case id {}", courtCaseId);
                }
            } catch (Exception exc) {
                log.error("Error generating retention date case document for case id '{}'", courtCaseId, exc);
            }
        }
    }
}
