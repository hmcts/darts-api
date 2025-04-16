package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

@FunctionalInterface
public interface TransientObjectDirectoryService {

    TransientObjectDirectoryEntity saveTransientObjectDirectoryEntity(TransformedMediaEntity transformedMediaEntity,
                                                                      String blobName);
}
