package uk.gov.hmcts.darts.datamanagement.service;

public interface InboundToUnstructuredProcessorSingleElement {

    void processSingleElement(Integer inboundEodEntityId);
}
