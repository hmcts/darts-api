package uk.gov.hmcts.darts.datamanagement.service;

@FunctionalInterface
public interface InboundToUnstructuredProcessorSingleElement {

    void processSingleElement(Long inboundEodEntityId);
}
