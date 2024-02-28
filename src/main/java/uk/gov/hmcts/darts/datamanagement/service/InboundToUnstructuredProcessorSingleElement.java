package uk.gov.hmcts.darts.datamanagement.service;

import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.util.List;

public interface InboundToUnstructuredProcessorSingleElement {

    void processSingleElement(Integer inboundExternalObjectDirectoryId,
                              List<ExternalObjectDirectoryEntity> unstructuredStoredList,
                              List<ExternalObjectDirectoryEntity> unstructuredFailedList);

    void processSingleElement(Integer inboundObjectId);
}
