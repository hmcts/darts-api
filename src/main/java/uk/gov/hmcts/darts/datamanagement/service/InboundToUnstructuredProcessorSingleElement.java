package uk.gov.hmcts.darts.datamanagement.service;

import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.util.List;

public interface InboundToUnstructuredProcessorSingleElement {

    void processSingleElement(ExternalObjectDirectoryEntity inboundExternalObjectDirectory,
                              List<ExternalObjectDirectoryEntity> unstructuredStoredList,
                              List<ExternalObjectDirectoryEntity> unstructuredFailedList);
}
