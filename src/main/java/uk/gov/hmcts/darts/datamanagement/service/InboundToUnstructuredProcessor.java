package uk.gov.hmcts.darts.datamanagement.service;

@FunctionalInterface
public interface InboundToUnstructuredProcessor {

    void processInboundToUnstructured(int batchSize);

}
