package uk.gov.hmcts.darts.casedocument.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.casedocument.service.CaseDocumentService;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseDocumentProcessor;
import uk.gov.hmcts.darts.casedocument.template.CourtCaseDocument;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateCaseDocumentSingleCaseDocumentProcessorImpl implements GenerateCaseDocumentSingleCaseDocumentProcessor {

    private final CaseDocumentRepository caseDocumentRepository;
    private final CaseDocumentService caseDocumentService;

    @Override
    public void processGenerateCaseDocument(Integer caseId) {

        CourtCaseDocument courtCaseDocument = caseDocumentService.generateCaseDocument(caseId);

    }
}
