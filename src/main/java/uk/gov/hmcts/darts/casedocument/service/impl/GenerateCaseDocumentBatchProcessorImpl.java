package uk.gov.hmcts.darts.casedocument.service.impl;

import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentProcessor;

public class GenerateCaseDocumentBatchProcessorImpl implements GenerateCaseDocumentProcessor {

    private final int batchSize;

    public GenerateCaseDocumentBatchProcessorImpl(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public void processGenerateCaseDocument() {

    }
}
