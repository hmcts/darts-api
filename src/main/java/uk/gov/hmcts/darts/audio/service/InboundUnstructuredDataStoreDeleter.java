package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.util.List;

public interface InboundUnstructuredDataStoreDeleter {
    List<ExternalObjectDirectoryEntity> delete();
}
