package uk.gov.hmcts.darts.casedocument.service;

@FunctionalInterface
public interface GenerateCaseDocumentSingleCaseProcessor {

    void processGenerateCaseDocument(Integer caseId);
    
}
