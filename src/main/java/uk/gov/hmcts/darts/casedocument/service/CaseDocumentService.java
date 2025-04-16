package uk.gov.hmcts.darts.casedocument.service;

import uk.gov.hmcts.darts.casedocument.model.CourtCaseDocument;

@FunctionalInterface
public interface CaseDocumentService {

    CourtCaseDocument generateCaseDocument(Integer caseId);
}
