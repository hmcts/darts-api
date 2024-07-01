package uk.gov.hmcts.darts.casedocument.service;

import uk.gov.hmcts.darts.casedocument.model.CourtCaseDocument;

public interface CaseDocumentService {

    CourtCaseDocument generateCaseDocument(Integer caseId);
}
