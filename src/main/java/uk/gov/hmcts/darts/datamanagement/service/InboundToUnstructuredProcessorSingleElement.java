package uk.gov.hmcts.darts.datamanagement.service;

import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

public interface InboundToUnstructuredProcessorSingleElement {

    void processSingleElement(ExternalObjectDirectoryEntity inboundObjectId);
}
