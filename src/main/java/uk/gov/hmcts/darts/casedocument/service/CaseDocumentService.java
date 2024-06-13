package uk.gov.hmcts.darts.casedocument.service;

import uk.gov.hmcts.darts.casedocument.service.model.CaseDocument;

public interface CaseDocumentService {

    CaseDocument generateCaseDocument(Integer caseId);
}
